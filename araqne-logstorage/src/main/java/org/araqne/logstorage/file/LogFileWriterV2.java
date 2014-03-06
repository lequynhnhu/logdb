/*
 * Copyright 2010 NCHOVY
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.araqne.logstorage.file;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.zip.Deflater;

import org.araqne.codec.FastEncodingRule;
import org.araqne.logstorage.Log;
import org.araqne.logstorage.LogFlushCallback;
import org.araqne.logstorage.LogFlushCallbackArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOT thread-safe
 * 
 * @author xeraph
 * 
 */
public class LogFileWriterV2 extends LogFileWriter {
	private final Logger logger = LoggerFactory.getLogger(LogFileWriterV2.class.getName());

	private static final int INDEX_ITEM_SIZE = 4;
	public static final int DEFAULT_BLOCK_SIZE = 640 * 1024; // 640KB
	public static final int DEFAULT_LEVEL = 3;

	private RandomAccessFile indexFile;
	private RandomAccessFile dataFile;
	private long count;
	private byte[] intbuf = new byte[4];
	private byte[] longbuf = new byte[8];
	private long lastKey;
	private long lastTime;

	private Long blockStartLogTime;
	private Long blockEndLogTime;
	private int blockLogCount;

	private ByteBuffer indexBuffer;
	private ByteBuffer dataBuffer;
	private byte[] compressed;
	private Deflater compresser;
	private int compressLevel;

	private File indexPath;
	private File dataPath;
	private volatile Date lastFlush = new Date();

	private List<Log> buffer = new ArrayList<Log>();

	public LogFileWriterV2(File indexPath, File dataPath, Set<LogFlushCallback> fcb, LogFlushCallbackArgs fcbArgs) throws IOException,
			InvalidLogFileHeaderException {
		this(indexPath, dataPath, DEFAULT_BLOCK_SIZE, fcb, fcbArgs);
	}

	// TODO: block size modification does not work
	private LogFileWriterV2(File indexPath, File dataPath, int blockSize, Set<LogFlushCallback> fcb, LogFlushCallbackArgs fcbArgs)
			throws IOException, InvalidLogFileHeaderException {
		this(indexPath, dataPath, blockSize, DEFAULT_LEVEL, fcb, fcbArgs);
	}

	public LogFileWriterV2(File indexPath, File dataPath, int blockSize, int level, Set<LogFlushCallback> fcb, LogFlushCallbackArgs fcbArgs)
			throws IOException,
			InvalidLogFileHeaderException {
		// level 0 will not use compression (no zip metadata overhead)
		try {
			if (level < 0 || level > 9)
				throw new IllegalArgumentException("compression level should be between 0 and 9");

			boolean indexExists = indexPath.exists();
			boolean dataExists = dataPath.exists();

			this.flushCallbacks = fcb;
			this.flushCallbackArgs = fcbArgs;
			this.indexPath = indexPath;
			this.dataPath = dataPath;
			this.indexFile = new RandomAccessFile(indexPath, "rw");
			this.dataFile = new RandomAccessFile(dataPath, "rw");

			// 1/64 alloc, if block size = 640KB, index can contain 10240 items
			this.indexBuffer = ByteBuffer.allocate(blockSize >> 6);
			this.dataBuffer = ByteBuffer.allocate(blockSize);

			this.compressed = new byte[blockSize];
			this.compresser = new Deflater(level);
			this.compressLevel = level;

			// get index file header
			LogFileHeader indexFileHeader = null;
			if (indexExists && indexFile.length() > 0) {
				indexFileHeader = LogFileHeader.extractHeader(indexFile, indexPath);
			} else {
				indexFileHeader = new LogFileHeader((short) 2, LogFileHeader.MAGIC_STRING_INDEX);
				indexFile.write(indexFileHeader.serialize());
			}

			// get data file header
			LogFileHeader dataFileHeader = null;
			if (dataExists && dataFile.length() > 0) {
				dataFileHeader = LogFileHeader.extractHeader(dataFile, dataPath);
			} else {
				dataFileHeader = new LogFileHeader((short) 2, LogFileHeader.MAGIC_STRING_DATA);
				byte[] ext = new byte[4];
				prepareInt(blockSize, ext);
				if (level > 0) {
					ext = new byte[12];
					prepareInt(blockSize, ext);
					ByteBuffer bb = ByteBuffer.wrap(ext, 4, 8);
					bb.put("deflater".getBytes());
				}

				dataFileHeader.setExtraData(ext);
				dataFile.write(dataFileHeader.serialize());
			}

			// read last key
			long length = indexFile.length();
			long pos = indexFileHeader.size();
			while (pos < length) {
				indexFile.seek(pos);
				int logCount = indexFile.readInt();
				count += logCount;
				pos += 4 + INDEX_ITEM_SIZE * logCount;
			}
			lastKey = count;

			// read last time
			length = dataFile.length();
			pos = dataFileHeader.size();
			while (pos < length) {
				// jump to end date position
				dataFile.seek(pos + 8);
				long endTime = dataFile.readLong();
				dataFile.readInt();
				int compressedLength = dataFile.readInt();
				lastTime = (lastTime < endTime) ? endTime : lastTime;
				pos += 24 + compressedLength;
			}

			// move to end
			indexFile.seek(indexFile.length());
			dataFile.seek(dataFile.length());
		} catch (Throwable t) {
			ensureClose(indexFile);
			ensureClose(dataFile);
			throw new IllegalStateException(t);
		}
	}

	@Override
	public boolean isLowDisk() {
		File dir = indexPath.getParentFile();
		return dir != null && dir.getFreeSpace() == 0;
	}

	@Override
	public long getLastKey() {
		return lastKey;
	}

	@Override
	public Date getLastDate() {
		return new Date(lastTime);
	}

	@Override
	public long getCount() {
		return count;
	}

	private LogRecord convert(Log log) {
		ByteBuffer bb = new FastEncodingRule().encode(log.getData());
		LogRecord logdata = new LogRecord(log.getDate(), log.getId(), bb);
		log.setBinaryLength(bb.remaining());
		return logdata;
	}

	public void rawWrite(LogRecord data) throws IOException {
		// do not remove this condition (date.toString() takes many CPU time)
		if (logger.isDebugEnabled())
			logger.debug("araqne logstorage: write new log, idx [{}], dat [{}], id {}, time {}",
					new Object[] { indexPath.getAbsolutePath(), dataPath.getAbsolutePath(), data.getId(),
							data.getDate().toString() });

		// check validity
		long newKey = data.getId();
		if (newKey <= lastKey)
			throw new IllegalArgumentException("invalid key: " + newKey + ", last key was " + lastKey);

		if ((blockEndLogTime != null && blockEndLogTime > data.getDate().getTime()) || indexBuffer.remaining() < INDEX_ITEM_SIZE
				|| dataBuffer.remaining() < 20 + data.getData().remaining())
			flush(false);

		// add to index buffer
		prepareInt(dataBuffer.position(), intbuf);
		indexBuffer.put(intbuf);
		if (blockStartLogTime == null)
			blockStartLogTime = data.getDate().getTime();
		blockEndLogTime = data.getDate().getTime();
		blockLogCount++;

		// add to data buffer
		prepareLong(data.getId(), longbuf);
		dataBuffer.put(longbuf);
		prepareLong(data.getDate().getTime(), longbuf);
		dataBuffer.put(longbuf);
		prepareInt(data.getData().remaining(), intbuf);
		dataBuffer.put(intbuf);
		ByteBuffer b = data.getData();
		if (b.remaining() == b.array().length) {
			dataBuffer.put(b.array());
		} else {
			byte[] array = new byte[b.remaining()];
			int pos = b.position();
			b.get(array);
			b.position(pos);
			dataBuffer.put(array);
		}

		// update last key
		lastKey = newKey;
		long time = data.getDate().getTime();
		lastTime = (lastTime < time) ? time : lastTime;

		count++;
	}

	@Override
	public void write(Log log) throws IOException {
		rawWrite(convert(log));
		buffer.add(log);
	}

	@Override
	public void write(List<Log> data) throws IOException {
		for (Log log : data) {
			try {
				rawWrite(convert(log));
				buffer.add(log);
			} catch (IllegalArgumentException e) {
				logger.error("log storage: write failed " + dataPath.getAbsolutePath(), e.getMessage());
			}
		}
	}

	@Override
	public List<Log> getBuffer() {
		List<Log> captured = buffer;
		int capturedLimit = captured.size();
		List<Log> result = new ArrayList<Log>(capturedLimit);
		for (int i = 0; i < capturedLimit; ++i) {
			result.add(captured.get(i));
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<List<Log>> getBuffers() {
		return Arrays.asList(buffer);
	}

	@Override
	public boolean flush(boolean sweep) throws IOException {
		// check if writer is closed
		if (indexFile == null || dataFile == null)
			return false;

		if (indexBuffer.position() == 0)
			return false;

		if (logger.isTraceEnabled())
			logger.trace("araqne logstorage: flush idx [{}], dat [{}] files", indexPath, dataPath);

		if (flushCallbacks != null && flushCallbackArgs != null)
			for (LogFlushCallback c : flushCallbacks) {
				try {
					LogFlushCallbackArgs arg = flushCallbackArgs.shallowCopy();
					arg.setLogs(buffer);
					c.onFlush(arg);
				} catch (Throwable t) {
					logger.warn("flush callback should not throw any exception", t);
				}
			}

		try {
			// mark last flush
			lastFlush = new Date();

			// write start date
			prepareLong(blockStartLogTime, longbuf);
			dataFile.write(longbuf);

			// write end date
			prepareLong(blockEndLogTime, longbuf);
			dataFile.write(longbuf);

			// write original size
			dataBuffer.flip();
			prepareInt(dataBuffer.limit(), intbuf);
			dataFile.write(intbuf);

			// compress data
			byte[] output = null;
			int outputSize = 0;

			if (compressLevel > 0) {
				compresser.setInput(dataBuffer.array(), 0, dataBuffer.limit());
				compresser.finish();
				int compressedSize = compresser.deflate(compressed);

				output = compressed;
				outputSize = compressedSize;
			} else {
				output = dataBuffer.array();
				outputSize = dataBuffer.limit();
			}

			// write compressed size
			prepareInt(outputSize, intbuf);
			dataFile.write(intbuf);

			// write compressed logs
			dataFile.write(output, 0, outputSize);

			dataBuffer.clear();
			compresser.reset();
			// dataFile.getFD().sync();

			// write log count
			prepareInt(blockLogCount, intbuf);
			indexFile.write(intbuf);

			// write log indexes
			indexBuffer.flip();
			indexFile.write(indexBuffer.array(), 0, indexBuffer.limit());
			indexBuffer.clear();
			// indexFile.getFD().sync();

			blockStartLogTime = null;
			blockEndLogTime = null;
			blockLogCount = 0;

			if (flushCallbacks != null && flushCallbackArgs != null)
				for (LogFlushCallback c : flushCallbacks) {
					try {
						LogFlushCallbackArgs arg = flushCallbackArgs.shallowCopy();
						arg.setLogs(buffer);
						c.onFlushCompleted(arg);
					} catch (Throwable t) {
						logger.warn("flush callback should not throw any exception", t);
					}
				}

			buffer.clear();

			return true;
		} catch (Throwable t) {
			if (flushCallbacks != null && flushCallbackArgs != null)
				for (LogFlushCallback c : flushCallbacks) {
					try {
						LogFlushCallbackArgs arg = flushCallbackArgs.shallowCopy();
						arg.setLogs(buffer);
						c.onFlushException(arg, t);
					} catch (Throwable t2) {
						logger.warn("flush callback should not throw any exception", t2);
					}
				}
			
			return false;
		}
	}

	@Override
	public void sync() throws IOException {
		if (indexFile == null || dataFile == null)
			return;

		dataFile.getFD().sync();
		indexFile.getFD().sync();
	}

	private void prepareInt(int l, byte[] b) {
		for (int i = 0; i < 4; i++)
			b[i] = (byte) ((l >> ((3 - i) * 8)) & 0xff);
	}

	private void prepareLong(long l, byte[] b) {
		for (int i = 0; i < 8; i++)
			b[i] = (byte) ((l >> ((7 - i) * 8)) & 0xff);
	}

	public Date getLastFlush() {
		return lastFlush;
	}

	private void ensureClose(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Throwable e) {
			}
		}
	}

	@Override
	public void close() throws IOException {
		flush(true);

		if (indexFile != null) {
			indexFile.close();
			indexFile = null;
		}
		if (dataFile != null) {
			dataFile.close();
			dataFile = null;
		}
	}

	@Override
	public boolean isClosed() {
		return dataFile == null;
	}

	/**
	 * @since 2.5.0
	 */
	@Override
	public void purge() {
		boolean result = indexPath.delete();
		logger.debug("araqne logstorage: delete [{}] file => {}", indexPath, result);

		result = dataPath.delete();
		logger.debug("araqne logstorage: delete [{}] file => {}", dataPath, result);
	}

	private Set<LogFlushCallback> flushCallbacks = new CopyOnWriteArraySet<LogFlushCallback>();
	private LogFlushCallbackArgs flushCallbackArgs;
}

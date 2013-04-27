/*
 * Copyright 2013 Future Systems
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
package org.araqne.logstorage.engine;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.araqne.logstorage.CachedRandomSeeker;
import org.araqne.logstorage.Log;
import org.araqne.logstorage.LogTableRegistry;
import org.araqne.logstorage.file.LogFileReader;
import org.araqne.logstorage.file.LogRecord;

/**
 * not thread-safe
 * 
 * @author xeraph
 * @since 0.9
 */
public class CachedRandomSeekerImpl implements CachedRandomSeeker {
	private boolean closed;
	private LogTableRegistry tableRegistry;
	private LogFileFetcher fetcher;

	private ConcurrentMap<OnlineWriterKey, OnlineWriter> onlineWriters;
	private Map<OnlineWriterKey, List<Log>> onlineBuffers;
	private Map<TabletKey, LogFileReader> cachedReaders;

	public CachedRandomSeekerImpl(LogTableRegistry tableRegistry, LogFileFetcher fetcher,
			ConcurrentMap<OnlineWriterKey, OnlineWriter> onlineWriters) {
		this.tableRegistry = tableRegistry;
		this.fetcher = fetcher;
		this.onlineWriters = onlineWriters;
		this.onlineBuffers = new HashMap<OnlineWriterKey, List<Log>>();
		this.cachedReaders = new HashMap<TabletKey, LogFileReader>();
	}

	@Override
	public Log getLog(String tableName, Date day, int id) throws IOException {
		if (closed)
			throw new IllegalStateException("already closed");

		int tableId = tableRegistry.getTableId(tableName);

		// check memory buffer (flush waiting)
		OnlineWriterKey onlineKey = new OnlineWriterKey(tableName, day, tableId);
		List<Log> buffer = onlineBuffers.get(onlineKey);
		if (buffer == null) {
			// try load on demand
			OnlineWriter writer = onlineWriters.get(onlineKey);
			if (writer != null) {
				buffer = writer.getBuffer();
				onlineBuffers.put(onlineKey, buffer);
			}
		}

		if (buffer != null) {
			for (Log r : buffer)
				if (r.getId() == id) {
					return r;
				}
		}

		TabletKey key = new TabletKey(tableId, day);
		LogFileReader reader = cachedReaders.get(key);
		if (reader == null) {
			reader = fetcher.fetch(tableName, day);
			cachedReaders.put(key, reader);
		}

		LogRecord log = reader.find(id);
		if (log == null)
			return null;

		return LogMarshaler.convert(tableName, log);
	}

	@Override
	public void close() {
		if (closed)
			return;

		closed = true;

		for (LogFileReader reader : cachedReaders.values()) {
			reader.close();
		}

		cachedReaders.clear();
	}
}

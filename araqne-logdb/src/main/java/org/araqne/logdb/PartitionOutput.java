/**
 * Copyright 2014 Eediom Inc.
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
package org.araqne.logdb;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import org.araqne.log.api.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author darkluster
 * 
 */
public class PartitionOutput {
	private final Logger logger = LoggerFactory.getLogger(PartitionOutput.class);

	private String tmpPath;
	private String path;
	private Date day;
	private OutputStream os;
	private OutputStreamWriter osw;
	private BufferedOutputStream bos;
	private FileMover mover;
	private File f;

	public PartitionOutput(String path, String tmpPath, Date day, String encoding, boolean useCompression)
			throws IOException {
		this.tmpPath = tmpPath;
		this.day = day;
		this.path = convertPath(path, day);

		if (tmpPath != null) {
			File dir = new File(tmpPath);
			this.f = new File(dir, UUID.randomUUID().toString() + ".part");

			if (logger.isDebugEnabled())
				logger.debug("araqne logdb: created temp partition output file [{}] for path [{}], day [{}]",
						new Object[] { f.getAbsolutePath(), path, day });

			this.tmpPath = f.getAbsolutePath();
		} else {
			this.f = new File(this.path);
		}

		f.getParentFile().mkdirs();

		if (encoding == null)
			encoding = "utf-8";

		if (useCompression) {
			this.os = new GZIPOutputStream(new FileOutputStream(f), 8192);
			this.osw = new OutputStreamWriter(os, Charset.forName(encoding));
		} else {
			this.os = new FileOutputStream(f);
			this.bos = new BufferedOutputStream(os);
			this.osw = new OutputStreamWriter(bos, Charset.forName(encoding));
		}
		mover = new LocalFileMover();
	}

	public OutputStreamWriter getWriter() {
		return osw;
	}

	private static String convertPath(String path, Date day) {
		if (!path.contains("{") || !path.contains("}") || !path.contains(":"))
			return path;

		String convertedPath = path.substring(0, path.indexOf("{"));
		for (int f = 0; (f = path.indexOf("{", f)) != -1; f++) {
			int t = path.indexOf("}", f);
			String s = path.substring(f, t + 1);
			String time = s.substring(1, s.indexOf(":"));
			String format = s.substring(s.indexOf(":") + 1, s.length() - 1);

			Date target = new Date();
			if (time.equalsIgnoreCase("logtime"))
				target = day;

			if (format.equalsIgnoreCase("epoch"))
				convertedPath += target.getTime();
			else {
				SimpleDateFormat sdf = new SimpleDateFormat(format);
				convertedPath += sdf.format(day);
			}
		}

		convertedPath += path.substring(path.lastIndexOf("}") + 1);
		return convertedPath;
	}

	public String getPath() {
		return path;
	}

	public String getTmpPath() {
		return tmpPath;
	}

	public void close() {
		FileUtils.ensureClose(osw);
		FileUtils.ensureClose(bos);
		FileUtils.ensureClose(os);

		try {
			if (tmpPath != null)
				mover.move(tmpPath, path);
		} catch (IOException e) {
			logger.error("araqne logdb: cannot move file from [{}] to [{}] cause [{}]",
					new Object[] { tmpPath, path, e.getMessage() });
		}
	}
}
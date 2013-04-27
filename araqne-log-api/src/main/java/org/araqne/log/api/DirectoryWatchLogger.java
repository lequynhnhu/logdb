/*
 * Copyright 2013 Eediom Inc.
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
package org.araqne.log.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.araqne.log.api.impl.FileUtils;
import org.araqne.log.api.impl.LastPositionHelper;

public class DirectoryWatchLogger extends AbstractLogger {
	private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DirectoryWatchLogger.class.getName());
	protected File dataDir;
	protected String basePath;
	protected Pattern fileNamePattern;
	protected Pattern dateExtractPattern;
	protected SimpleDateFormat dateFormat;
	private Matcher dateExtractMatcher;

	public DirectoryWatchLogger(LoggerSpecification spec, LoggerFactory factory) {
		super(spec, factory);

		dataDir = new File(System.getProperty("araqne.data.dir"), "araqne-log-api");
		dataDir.mkdirs();
		basePath = getConfig().get("base_path");

		String fileNameRegex = getConfig().get("filename_pattern");
		fileNamePattern = Pattern.compile(fileNameRegex);

		// optional
		String dateExtractRegex = getConfig().get("date_pattern");
		if (dateExtractRegex != null)
			dateExtractPattern = Pattern.compile(dateExtractRegex);

		// optional
		String dateFormatString = getConfig().get("date_format");
		if (dateFormatString != null)
			dateFormat = new SimpleDateFormat(dateFormatString);
	}

	@Override
	protected void runOnce() {
		List<String> logFiles = FileUtils.matchFiles(basePath, fileNamePattern);

		Map<String, String> lastPositions = LastPositionHelper.readLastPositions(getLastLogFile());

		for (String path : logFiles) {
			processFile(lastPositions, path);
		}

		LastPositionHelper.updateLastPositionFile(getLastLogFile(), lastPositions);
	}

	protected void processFile(Map<String, String> lastPositions, String path) {
		FileInputStream is = null;
		BufferedReader br = null;

		try {
			is = new FileInputStream(path);

			// skip previous read part
			long offset = 0;
			if (lastPositions.containsKey(path)) {
				offset = Long.valueOf(lastPositions.get(path));
				is.skip(offset);
				logger.trace("logpresso igloo: target file [{}] skip offset [{}]", path, offset);
			}

			br = new BufferedReader(new InputStreamReader(is, "utf-8"));

			// read and normalize log
			while (true) {
				if (getStatus() == LoggerStatus.Stopping || getStatus() == LoggerStatus.Stopped)
					break;

				String line = br.readLine();
				if (line == null || line.trim().isEmpty())
					break;

				Date d = parseDate(line);
				Map<String, Object> log = new HashMap<String, Object>();
				log.put("line", line);

				write(new SimpleLog(d, getFullName(), log));
			}

			long position = is.getChannel().position();
			logger.debug("ncia bmt: updating file [{}] old position [{}] new last position [{}]", new Object[] { path, offset,
					position });
			lastPositions.put(path, Long.toString(position));

		} catch (Throwable e) {
			logger.error("ncia bmt: [" + getName() + "] logger read error", e);
		} finally {
			FileUtils.ensureClose(is);
			FileUtils.ensureClose(br);
		}
	}

	protected File getLastLogFile() {
		return new File(dataDir, "dirwatch-" + getName() + ".lastlog");
	}

	protected Date parseDate(String line) {
		if (dateExtractPattern == null || dateFormat == null)
			return new Date();

		if (dateExtractMatcher == null)
			dateExtractMatcher = dateExtractPattern.matcher(line);
		else
			dateExtractMatcher.reset(line);

		if (!dateExtractMatcher.find())
			return new Date();

		String s = null;
		int count = dateExtractMatcher.groupCount();
		for (int i = 1; i <= count; i++) {
			if (s == null)
				s = dateExtractMatcher.group(i);
			else
				s += dateExtractMatcher.group(i);
		}

		Date d = dateFormat.parse(s, new ParsePosition(0));
		return d != null ? d : new Date();
	}
}

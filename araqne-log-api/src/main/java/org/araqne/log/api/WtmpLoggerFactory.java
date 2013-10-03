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

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

@Component(name = "wtmp-logger-factory")
@Provides
public class WtmpLoggerFactory extends AbstractLoggerFactory {

	@Override
	public String getName() {
		return "wtmp";
	}

	@Override
	public String getDisplayName(Locale locale) {
		return "wtmp";
	}

	@Override
	public String getDescription(Locale locale) {
		if (locale.equals(Locale.KOREAN))
			return "터미널 로그인, 로그아웃 기록을 수집합니다.";
		return "collect wtmp log file";
	}

	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		LoggerConfigOption path = new StringConfigType("path", t("Path", "파일 경로"), t("wtmp file path", "wtmp 파일 경로"), true);
		return Arrays.asList(path);
	}

	private Map<Locale, String> t(String en, String ko) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, en);
		m.put(Locale.KOREAN, ko);
		return m;
	}

	@Override
	protected Logger createLogger(LoggerSpecification spec) {
		return new WtmpLogger(spec, this);
	}

	@Override
	public void deleteLogger(String namespace, String name) {
		super.deleteLogger(namespace, name);

		// delete lastpos file
		File dataDir = new File(System.getProperty("araqne.data.dir"), "araqne-log-api");
		File f = new File(dataDir, "wtmp-" + name + ".lastlog");
		f.delete();
	}

}
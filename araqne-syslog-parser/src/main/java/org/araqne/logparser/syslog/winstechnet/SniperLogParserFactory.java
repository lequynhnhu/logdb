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
package org.araqne.logparser.syslog.winstechnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.araqne.log.api.LogParser;
import org.araqne.log.api.LogParserFactory;
import org.araqne.log.api.LoggerConfigOption;

@Component(name = "sniper-log-parser-factory")
@Provides
public class SniperLogParserFactory implements LogParserFactory {

	@Override
	public String getName() {
		return "sniper";
	}

	@Override
	public Collection<Locale> getDisplayNameLocales() {
		return Arrays.asList(Locale.ENGLISH);
	}

	@Override
	public String getDisplayName(Locale locale) {
		return "Sniper";
	}

	@Override
	public Collection<Locale> getDescriptionLocales() {
		return Arrays.asList(Locale.ENGLISH);
	}

	@Override
	public String getDescription(Locale locale) {
		return "Winstechnet Sniper";
	}

	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		return new ArrayList<LoggerConfigOption>();
	}

	@Override
	public LogParser createParser(Map<String, String> config) {
		return new SniperLogParser();
	}

}
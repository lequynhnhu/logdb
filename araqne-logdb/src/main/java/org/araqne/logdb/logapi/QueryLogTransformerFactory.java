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
package org.araqne.logdb.logapi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.araqne.log.api.AbstractLogTransformerFactory;
import org.araqne.log.api.LogTransformer;
import org.araqne.log.api.LoggerConfigOption;
import org.araqne.log.api.StringConfigType;
import org.araqne.logdb.LogQueryCommand;
import org.araqne.logdb.LogQueryParserService;
import org.araqne.logdb.LogQueryService;

/**
 * @since 1.7.8
 * @author xeraph
 * 
 */
@Component(name = "query-log-transformer-factory")
@Provides
public class QueryLogTransformerFactory extends AbstractLogTransformerFactory {
	// force to wait dynamic query parser instance loading
	@Requires
	private LogQueryService queryService;

	@Requires
	private LogQueryParserService queryParser;

	@Override
	public String getName() {
		return "query";
	}

	@Override
	public String getDisplayName(Locale locale) {
		if (locale != null && locale.equals(Locale.KOREAN))
			return "쿼리 기반 원본 가공";
		return "Query";
	}

	@Override
	public List<Locale> getDescriptionLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN);
	}

	@Override
	public String getDescription(Locale locale) {
		if (locale != null && locale.equals(Locale.KOREAN))
			return "로그 쿼리를 이용하여 원본 데이터를 가공합니다.";
		return "Transform data using logdb query";
	}

	@Override
	public List<Locale> getDisplayNameLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN);
	}

	@Override
	public List<LoggerConfigOption> getConfigOptions() {
		LoggerConfigOption querystring = new StringConfigType("querystring", t("Query string", "쿼리 문자열"), t(
				"Configure query string to evaluating and transforming input log data",
				"입력 로그를 변환하여 출력하는데 사용할 쿼리 문자열을 설정합니다. 그룹 함수 사용은 허용되지 않습니다."), true);

		return Arrays.asList(querystring);
	}

	private Map<Locale, String> t(String en, String ko) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, en);
		m.put(Locale.KOREAN, ko);
		return m;
	}

	@Override
	public LogTransformer newTransformer(Map<String, String> config) {
		String queryString = config.get("querystring");

		List<LogQueryCommand> commands = queryParser.parseCommands(null, queryString);
		return new QueryLogTransformer(this, commands);
	}

}
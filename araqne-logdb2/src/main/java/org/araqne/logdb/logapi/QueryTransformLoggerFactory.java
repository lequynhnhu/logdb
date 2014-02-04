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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.araqne.log.api.AbstractLoggerFactory;
import org.araqne.log.api.Logger;
import org.araqne.log.api.LoggerConfigOption;
import org.araqne.log.api.LoggerRegistry;
import org.araqne.log.api.LoggerSpecification;
import org.araqne.log.api.StringConfigType;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryParserService;
import org.araqne.logdb.QueryService;

/**
 * Generate log using logdb query language
 * 
 * @since 1.7.8
 * @author xeraph
 * 
 */
@Component(name = "query-transform-logger-factory")
@Provides
public class QueryTransformLoggerFactory extends AbstractLoggerFactory {

	@Requires
	private LoggerRegistry loggerRegistry;

	// force to wait dynamic query parser instance loading
	@Requires
	private QueryService queryService;

	@Requires
	private QueryParserService queryParser;

	@Override
	public String getName() {
		return "ql-transform";
	}

	@Override
	public String getDisplayName(Locale locale) {
		if (locale != null && locale.equals(Locale.KOREAN))
			return "쿼리변환 로깅";
		return "QL Transform";
	}

	@Override
	public Collection<Locale> getDisplayNameLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN);
	}

	@Override
	public String getDescription(Locale locale) {
		if (locale.equals(Locale.KOREAN))
			return "원본 로거에서 수집되는 로그를 대상으로 쿼리를 평가한 결과를 로그로 발생시킵니다.";
		return "Generate log using logdb query evaluation per log";
	}

	@Override
	public Collection<Locale> getDescriptionLocales() {
		return Arrays.asList(Locale.ENGLISH, Locale.KOREAN);
	}

	@Override
	public Collection<LoggerConfigOption> getConfigOptions() {
		LoggerConfigOption sourceLogger = new StringConfigType("source_logger", t("Source logger name", "원본 로거 이름"), t(
				"Full name of data source logger", "네임스페이스를 포함한 원본 로거 이름"), true);

		LoggerConfigOption querystring = new StringConfigType("querystring", t("Query string", "쿼리 문자열"), t(
				"Configure query string to evaluating and transforming input log data",
				"입력 로그를 변환하여 출력하는데 사용할 쿼리 문자열을 설정합니다. 그룹 함수 사용은 허용되지 않습니다."), true);

		return Arrays.asList(sourceLogger, querystring);
	}

	private Map<Locale, String> t(String en, String ko) {
		Map<Locale, String> m = new HashMap<Locale, String>();
		m.put(Locale.ENGLISH, en);
		m.put(Locale.KOREAN, ko);
		return m;
	}

	@Override
	protected Logger createLogger(LoggerSpecification spec) {
		String queryString = spec.getConfig().get("querystring");

		List<QueryCommand> commands = queryParser.parseCommands(null, queryString);
		return new QueryTransformLogger(spec, this, loggerRegistry, commands);
	}

}
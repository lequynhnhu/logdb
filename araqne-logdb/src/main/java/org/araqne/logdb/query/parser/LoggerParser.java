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
package org.araqne.logdb.query.parser;

import org.araqne.log.api.LoggerRegistry;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryCommandParser;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.query.command.Logger;

public class LoggerParser implements QueryCommandParser {

	private LoggerRegistry loggerRegistry;

	public LoggerParser(LoggerRegistry loggerRegistry) {
		this.loggerRegistry = loggerRegistry;
	}

	@Override
	public String getCommandName() {
		return "logger";
	}

	@Override
	public QueryCommand parse(QueryContext context, String commandString) {
		return new Logger(loggerRegistry);
	}
}
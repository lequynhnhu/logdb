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

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryCommandParser;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.QueryParseException;
import org.araqne.logdb.query.command.Mv;

/**
 * @since 2.0.2-SNAPSHOT
 * @author darkluster
 * 
 */
public class MvParser implements QueryCommandParser {

	@Override
	public String getCommandName() {
		return "mv";
	}

	@SuppressWarnings("unchecked")
	@Override
	public QueryCommand parse(QueryContext context, String commandString) {
		if (commandString.trim().endsWith(","))
			throw new QueryParseException("missing-field", commandString.length());

		ParseResult r = QueryTokenizer.parseOptions(context, commandString, getCommandName().length(),
				Arrays.asList("from", "to"));
		Map<String, String> options = (Map<String, String>) r.value;
		if (!options.containsKey("from") || !options.containsKey("to"))
			throw new QueryParseException("missing-field", commandString.length());

		String from = options.get("from");
		String to = options.get("to");

		if (new File(to).exists())
			throw new QueryParseException("file-exists", -1, to);

		return new Mv(from, to);
	}
}

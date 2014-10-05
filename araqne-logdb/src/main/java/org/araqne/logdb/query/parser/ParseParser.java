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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.araqne.log.api.LogParserRegistry;
import org.araqne.logdb.AbstractQueryCommandParser;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.QueryParseException;
import org.araqne.logdb.query.command.ParseWithAnchor;
import org.araqne.logdb.query.command.Parse;

/**
 * @since 1.6.6
 * @author xeraph
 * 
 */
public class ParseParser extends AbstractQueryCommandParser {

	private LogParserRegistry registry;

	public ParseParser(LogParserRegistry registry) {
		this.registry = registry;
	}

	@Override
	public String getCommandName() {
		return "parse";
	}

	@SuppressWarnings("unchecked")
	@Override
	public QueryCommand parse(QueryContext context, String commandString) {
		ParseResult r =
				QueryTokenizer.parseOptions(
						context, commandString, getCommandName().length(), Arrays.asList("overlay"),
						getFunctionRegistry());
		Map<String, String> options = (Map<String, String>) r.value;
		boolean overlay = CommandOptions.parseBoolean(options.get("overlay"));

		String remainder = commandString.substring(r.next).trim();

		if (remainder.isEmpty())
			throw new QueryParseException("missing-parameter", r.next);

		if (registry.getProfile(remainder) != null)
			return newParserFromRegistry(overlay, remainder);

		List<String> parseByComma = QueryTokenizer.parseByComma(remainder);
		List<String> anchors = new ArrayList<String>(parseByComma.size());
		List<String> aliases = new ArrayList<String>(parseByComma.size());
		for (String e : parseByComma) {
			e = e.trim();
			try {
				ParseResult anchor = QueryTokenizer.nextString(e);
				ParseResult as = QueryTokenizer.nextString(e, anchor.next);
				ParseResult alias = QueryTokenizer.nextString(e, as.next);

				if (!"as".equals(as.value))
					throw new QueryParseException("syntax-error: \"as\" needed", remainder.indexOf(e));
				
				if (!QueryTokenizer.isQuoted((String) anchor.value))
					throw new QueryParseException("syntax-error: anchor should be quoted", remainder.indexOf(e));

				if (QueryTokenizer.isQuoted((String) alias.value))
					throw new QueryParseException("syntax-error: alias should not be quoted", remainder.indexOf(e));

				anchors.add((String) anchor.value);
				aliases.add((String) alias.value);
			} catch (QueryParseException ex) {
				throw ex;
			} catch (Throwable th) {
				throw new IllegalStateException(th);
			}

		}

		return new ParseWithAnchor(anchors, aliases);
	}

	private QueryCommand newParserFromRegistry(boolean overlay, String parserName) {

		if (registry.getProfile(parserName) == null)
			throw new QueryParseException("parser-not-found", -1);

		try {
			return new Parse(parserName, registry.newParser(parserName), overlay);
		} catch (Throwable t) {
			throw new QueryParseException("parser-init-failure", -1, t.toString());
		}
	}
}

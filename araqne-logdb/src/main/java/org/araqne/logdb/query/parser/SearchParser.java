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
package org.araqne.logdb.query.parser;

import org.araqne.logdb.AbstractQueryCommandParser;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.query.command.Search;
import org.araqne.logdb.query.expr.Expression;

public class SearchParser extends AbstractQueryCommandParser {

	@Override
	public String getCommandName() {
		return "search";
	}

	@Override
	public QueryCommand parse(QueryContext context, String commandString) {
		String args = commandString.substring("search".length()).trim();
		String exprToken = args;

		Long limit = null;
		int begin = args.indexOf("limit=");
		if (begin >= 0) {
			begin += "limit=".length();
			int end = args.indexOf(" ", begin);
			if (end > 0) {
				limit = Long.valueOf(args.substring(begin, end));
				exprToken = args.substring(end + 1);
			} else {
				limit = Long.valueOf(args.substring(begin));
				exprToken = "";
			}
		}

		Expression expr = null;
		if (!exprToken.trim().isEmpty())
			expr = ExpressionParser.parse(context, exprToken, getFunctionRegistry());
		return new Search(limit, expr);
	}
}

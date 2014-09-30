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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.araqne.logdb.AbstractQueryCommandParser;
import org.araqne.logdb.PartitionPlaceholder;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.QueryParseException;
import org.araqne.logdb.query.command.OutputCsv;

public class OutputCsvParser extends AbstractQueryCommandParser {
	private boolean defaultUseBom;

	public OutputCsvParser() {
		this(false);
	}

	public OutputCsvParser(boolean useBom) {
		this.defaultUseBom = useBom;
	}

	@Override
	public String getCommandName() {
		return "outputcsv";
	}

	@SuppressWarnings("unchecked")
	@Override
	public QueryCommand parse(QueryContext context, String commandString) {
		if (commandString.trim().endsWith(","))
		//	throw new QueryParseException("missing-field", commandString.length());
			throw new QueryParseException("30200", commandString.trim().length() -1, commandString.trim().length() -1 , null);

		ParseResult r = QueryTokenizer.parseOptions(context, commandString, getCommandName().length(),
				Arrays.asList("overwrite", "encoding", "bom", "tab", "tmp", "partition", "emptyfile"), getFunctionRegistry());

		Map<String, String> options = (Map<String, String>) r.value;
		boolean overwrite = CommandOptions.parseBoolean(options.get("overwrite"));

		boolean useBom = false;
		if (options.get("bom") == null)
			useBom = this.defaultUseBom;
		else
			useBom = CommandOptions.parseBoolean(options.get("bom"));

		boolean useTab = CommandOptions.parseBoolean(options.get("tab"));
		boolean usePartition = CommandOptions.parseBoolean(options.get("partition"));
		boolean emptyfile = CommandOptions.parseBoolean(options.get("emptyfile"));

		String encoding = options.get("encoding");
		if (encoding == null)
			encoding = "utf-8";

		String tmpPath = options.get("tmp");

		QueryTokens tokens = QueryTokenizer.tokenize(commandString.substring(r.next));
		List<String> fields = new ArrayList<String>();
		String originalCsvPath = tokens.string(0);
		String csvPath = ExpressionParser.evalContextReference(context, originalCsvPath, getFunctionRegistry());

		List<PartitionPlaceholder> holders = PartitionPlaceholder.parse(csvPath);
		if (!usePartition && holders.size() > 0)
		//	throw new QueryParseException("use-partition-option", -1, holders.size() + " partition holders");
			throw new QueryParseException("30201", getCommandName().length() + 1, commandString.length() - 1, null);
			
		List<QueryToken> fieldTokens = tokens.subtokens(1, tokens.size());
		for (QueryToken t : fieldTokens) {
			StringTokenizer tok = new StringTokenizer(t.token, ",");
			while (tok.hasMoreTokens())
				fields.add(tok.nextToken().trim());
		}

		if (fields.size() == 0)
	//		throw new QueryParseException("missing-field", commandString.length());
			throw new QueryParseException("30202", getCommandName().length() + 1 , commandString.length() - 1, null) ;

		return new OutputCsv(csvPath,  tmpPath, overwrite, fields, encoding, useBom, useTab, usePartition,emptyfile, holders);
	}
}

package org.araqne.logdb.query.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.araqne.cron.TickService;
import org.araqne.logdb.AbstractQueryCommandParser;
import org.araqne.logdb.ByteBufferResultFactory;
import org.araqne.logdb.DefaultQuery;
import org.araqne.logdb.Query;
import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.QueryParserService;
import org.araqne.logdb.QueryResultFactory;
import org.araqne.logdb.QueryService;
import org.araqne.logdb.query.command.Join.JoinType;
import org.araqne.logdb.query.command.Sort.SortField;
import org.araqne.logdb.query.command.StreamJoin;
import org.araqne.storage.api.RCDirectBufferManager;

public class StreamJoinParser extends AbstractQueryCommandParser {
	private QueryParserService parserService;
	private QueryService queryService;
	private TickService tickService;
	private QueryResultFactory resultFactory;
	private RCDirectBufferManager rcDirectBufferManager;

	public StreamJoinParser(QueryParserService parserService, TickService tickService, QueryResultFactory resultFactory, QueryService queryService, RCDirectBufferManager rcDirectBufferManager) {
		this.parserService = parserService;
		this.tickService = tickService;
		this.resultFactory = resultFactory;
		this.queryService = queryService;
		this.rcDirectBufferManager = rcDirectBufferManager;
	}

	@Override
	public String getCommandName() {
		return "streamjoin";
	}

	//TODO:
	// Make capcity as a query arqument
	// CAPACITY MUST BE PASSED WITH PARSING ARGUMENT
	private static final int CAPACITY = 1024 * 1024 * 700;
	
	@Override
	public QueryCommand parse(QueryContext context, String commandString) {
		int b = commandString.indexOf('[');
		int e = commandString.lastIndexOf(']');

		int cmdLen = getCommandName().length();
		String fieldToken = commandString.substring(cmdLen, b);
		String subQueryString = commandString.substring(b + 1, e).trim();

		ParseResult r = QueryTokenizer.parseOptions(context, fieldToken, 0, Arrays.asList("type"), getFunctionRegistry());
		@SuppressWarnings("unchecked")
		Map<String, Object> options = (Map<String, Object>) r.value;

		String type = null;
		if (options != null) {
			type = (String) options.get("type");
		}

		if (r.next < 0)
			r.next = 0;

		JoinType joinType = JoinType.Inner;
		if (type != null && type.equals("left"))
			joinType = JoinType.Left;

		List<SortField> sortFields = SortField.parseSortFields(fieldToken, r);

		SortField[] sortFieldArray = sortFields.toArray(new SortField[0]);
		List<QueryCommand> subCommands = parserService.parseCommands(context, subQueryString);
		Query subQuery = new DefaultQuery(context, subQueryString, subCommands, new ByteBufferResultFactory(rcDirectBufferManager, CAPACITY));

		return new StreamJoin(joinType, sortFieldArray, subQuery, tickService, queryService, rcDirectBufferManager);
	}
}

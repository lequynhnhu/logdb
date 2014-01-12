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
package org.araqne.logdb.client;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.araqne.api.PrimitiveConverter;
import org.araqne.codec.EncodingRule;
import org.araqne.logdb.client.http.WebSocketTransport;
import org.araqne.logdb.client.http.impl.TrapListener;
import org.araqne.websocket.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 0.5.0
 * @author xeraph
 * 
 */
public class LogDbClient implements TrapListener, Closeable {
	private Logger logger = LoggerFactory.getLogger(LogDbClient.class);
	private LogDbTransport transport;
	private LogDbSession session;
	private int fetchSize = 10000;
	private ConcurrentMap<Integer, LogQuery> queries = new ConcurrentHashMap<Integer, LogQuery>();
	private Locale locale = Locale.getDefault();

	public LogDbClient() {
		this(new WebSocketTransport());
	}

	public LogDbClient(LogDbTransport transport) {
		this.transport = transport;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		checkNotNull("locale", locale);
		this.locale = locale;
	}

	/**
	 * @since 0.6.0
	 */
	public int getFetchSize() {
		return fetchSize;
	}

	/**
	 * @since 0.6.0
	 */
	public void setFetchSize(int fetchSize) {
		this.fetchSize = fetchSize;
	}

	public boolean isClosed() {
		return session == null || session.isClosed();
	}

	public List<LogQuery> getQueries() throws IOException {
		Message resp = session.rpc("org.araqne.logdb.msgbus.LogQueryPlugin.queries");

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> l = (List<Map<String, Object>>) resp.getParameters().get("queries");

		for (Map<String, Object> q : l) {
			int queryId = (Integer) q.get("id");
			LogQuery query = queries.get(queryId);
			if (query == null) {
				query = new LogQuery(this, queryId, (String) q.get("query_string"));
				LogQuery old = queries.putIfAbsent(queryId, query);
				if (old != null)
					query = old;
			}

			parseQueryStatus(q, query);
		}

		return new ArrayList<LogQuery>(queries.values());
	}

	public LogQuery getQuery(int id) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);

		try {
			Message resp = session.rpc("org.araqne.logdb.msgbus.LogQueryPlugin.queryStatus", params);

			Map<String, Object> q = resp.getParameters();
			int queryId = (Integer) q.get("id");
			LogQuery query = queries.get(queryId);
			if (query == null) {
				query = new LogQuery(this, queryId, (String) q.get("query_string"));
				LogQuery old = queries.putIfAbsent(queryId, query);
				if (old != null)
					query = old;
			}

			parseQueryStatus(q, query);
		} catch (MessageException t) {
			if (!t.getMessage().startsWith("msgbus-handler-not-found"))
				throw t;
		}

		return queries.get(id);
	}

	private void parseQueryStatus(Map<String, Object> q, LogQuery query) {
		List<LogQueryCommand> commands = new ArrayList<LogQueryCommand>();

		@SuppressWarnings("unchecked")
		List<Map<String, Object>> cl = (List<Map<String, Object>>) q.get("commands");
		for (Map<String, Object> cm : cl) {
			LogQueryCommand c = new LogQueryCommand();
			c.setStatus((String) cm.get("status"));
			c.setPushCount(toLong(cm.get("push_count")));
			c.setCommand((String) cm.get("command"));
			commands.add(c);
		}

		query.setCommands(commands);
		boolean end = (Boolean) q.get("is_end");

		boolean eof = end;
		if (q.containsKey("is_eof"))
			eof = (Boolean) q.get("is_eof");

		boolean cancelled = false;
		if (q.containsKey("is_cancelled"))
			cancelled = (Boolean) q.get("is_cancelled");

		if (eof) {
			if (!query.getCommands().get(0).getStatus().equalsIgnoreCase("Waiting"))
				query.updateStatus("Ended");

			if (cancelled)
				query.updateStatus("Cancelled");
		} else if (end) {
			query.updateStatus("Stopped");
		} else {
			query.updateStatus("Running");
		}

		if (q.containsKey("background"))
			query.setBackground((Boolean) q.get("background"));

		query.setElapsed(toLong(q.get("elapsed")));

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
		if (q.get("last_started") != null)
			query.setLastStarted(df.parse((String) q.get("last_started"), new ParsePosition(0)));

	}

	private Long toLong(Object v) {
		if (v == null)
			return null;

		if (v instanceof Integer)
			return (long) (Integer) v;

		if (v instanceof Long)
			return (Long) v;

		return null;
	}

	public void connect(String host, String loginName, String password) throws IOException {
		connect(host, 80, loginName, password);
	}

	public void connect(String host, int port, String loginName, String password) throws IOException {
		this.session = transport.newSession(host, port);
		this.session.login(loginName, password, true);
		this.session.addListener(this);
	}

	@SuppressWarnings("unchecked")
	public List<ArchiveConfig> listArchiveConfigs() throws IOException {
		List<ArchiveConfig> configs = new ArrayList<ArchiveConfig>();
		Message resp = session.rpc("org.logpresso.core.msgbus.ArchivePlugin.getConfigs");
		List<Map<String, Object>> l = (List<Map<String, Object>>) resp.get("configs");
		for (Map<String, Object> m : l) {
			configs.add(parseArchiveConfig(m));
		}

		return configs;
	}

	@SuppressWarnings("unchecked")
	public ArchiveConfig getArchiveConfig(String loggerName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger", loggerName);

		Message resp = session.rpc("org.logpresso.core.msgbus.ArchivePlugin.getConfig", params);
		Map<String, Object> m = (Map<String, Object>) resp.getParameters().get("config");
		return parseArchiveConfig(m);
	}

	@SuppressWarnings("unchecked")
	private ArchiveConfig parseArchiveConfig(Map<String, Object> m) {
		ArchiveConfig c = new ArchiveConfig();
		c.setLoggerName((String) m.get("logger"));
		c.setTableName((String) m.get("table"));
		c.setHost((String) m.get("host"));
		c.setPrimaryLogger((String) m.get("primary_logger"));
		c.setBackupLogger((String) m.get("backup_logger"));
		c.setEnabled((Boolean) m.get("enabled"));
		c.setMetadata((Map<String, String>) m.get("metadata"));
		return c;
	}

	public void createArchiveConfig(ArchiveConfig config) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger", config.getLoggerName());
		params.put("table", config.getTableName());
		params.put("host", config.getHost());
		params.put("enabled", config.isEnabled());
		params.put("metadata", config.getMetadata());
		session.rpc("org.logpresso.core.msgbus.ArchivePlugin.createConfig", params);
	}

	public void removeArchiveConfig(String loggerName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger", loggerName);
		session.rpc("org.logpresso.core.msgbus.ArchivePlugin.removeConfig", params);
	}

	@SuppressWarnings("unchecked")
	public List<AccountInfo> listAccounts() throws IOException {
		Message resp = session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.listAccounts");
		List<AccountInfo> accounts = new ArrayList<AccountInfo>();
		List<Object> l = (List<Object>) resp.get("accounts");
		for (Object o : l) {
			Map<String, Object> m = (Map<String, Object>) o;
			List<Object> pl = (List<Object>) m.get("privileges");

			AccountInfo account = new AccountInfo();
			String loginName = (String) m.get("login_name");
			account.setLoginName(loginName);

			for (Object o2 : pl) {
				Map<String, Object> m2 = (Map<String, Object>) o2;
				String tableName = (String) m2.get("table_name");
				Privilege p = new Privilege(loginName, tableName);
				account.getPrivileges().add(p);
			}
			accounts.add(account);
		}

		return accounts;
	}

	public void createAccount(AccountInfo account) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("login_name", account.getLoginName());
		params.put("password", account.getPassword());

		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.createAccount", params);
	}

	public void removeAccount(String loginName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("login_name", loginName);

		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.removeAccount", params);
	}

	public void changePassword(String loginName, String password) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("login_name", loginName);
		params.put("password", password);

		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.changePassword", params);
	}

	public void grantPrivilege(Privilege privilege) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("login_name", privilege.getLoginName());
		params.put("table_name", privilege.getTableName());

		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.grantPrivilege", params);
	}

	public void revokePrivilege(Privilege privilege) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("login_name", privilege.getLoginName());
		params.put("table_name", privilege.getTableName());

		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.revokePrivilege", params);
	}

	@SuppressWarnings("unchecked")
	public List<IndexTokenizerFactoryInfo> listIndexTokenizerFactories() throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("locale", locale.getLanguage());

		Message resp = session.rpc("com.logpresso.index.msgbus.ManagementPlugin.listIndexTokenizerFactories", params);

		List<IndexTokenizerFactoryInfo> l = new ArrayList<IndexTokenizerFactoryInfo>();
		for (Object o : (List<Object>) resp.getParameters().get("factories")) {
			IndexTokenizerFactoryInfo f = parseIndexTokenizerFactory(o);
			l.add(f);
		}

		return l;
	}

	public IndexTokenizerFactoryInfo getIndexTokenizerFactory(String name) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);
		params.put("locale", locale.getLanguage());

		Message resp = session.rpc("com.logpresso.index.msgbus.ManagementPlugin.getIndexTokenizerFactoryInfo", params);
		return parseIndexTokenizerFactory(resp.getParameters().get("factory"));
	}

	@SuppressWarnings("unchecked")
	private IndexTokenizerFactoryInfo parseIndexTokenizerFactory(Object o) {
		Map<String, Object> m = (Map<String, Object>) o;
		IndexTokenizerFactoryInfo f = new IndexTokenizerFactoryInfo();
		f.setName((String) m.get("name"));
		f.setConfigSpecs(parseIndexConfigList((List<Object>) m.get("config_specs")));
		return f;
	}

	@SuppressWarnings("unchecked")
	private List<IndexConfigSpec> parseIndexConfigList(List<Object> l) {
		List<IndexConfigSpec> specs = new ArrayList<IndexConfigSpec>();

		for (Object o : l) {
			Map<String, Object> m = (Map<String, Object>) o;
			IndexConfigSpec spec = new IndexConfigSpec();
			spec.setKey((String) m.get("key"));
			spec.setName((String) m.get("name"));
			spec.setDescription((String) m.get("description"));
			spec.setRequired((Boolean) m.get("required"));
			specs.add(spec);
		}

		return specs;
	}

	@SuppressWarnings("unchecked")
	public List<IndexInfo> listIndexes(String tableName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);

		Message resp = session.rpc("com.logpresso.index.msgbus.ManagementPlugin.listIndexes", params);
		List<IndexInfo> indexes = new ArrayList<IndexInfo>();

		List<Object> l = (List<Object>) resp.getParameters().get("indexes");
		for (Object o : l) {
			Map<String, Object> m = (Map<String, Object>) o;
			IndexInfo indexInfo = getIndexInfo(m);
			indexes.add(indexInfo);
		}

		return indexes;
	}

	@SuppressWarnings("unchecked")
	public IndexInfo getIndexInfo(String tableName, String indexName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		params.put("index", indexName);

		Message resp = session.rpc("com.logpresso.index.msgbus.ManagementPlugin.getIndexInfo", params);
		return getIndexInfo((Map<String, Object>) resp.getParameters().get("index"));
	}

	/**
	 * @since 0.8.1
	 */
	@SuppressWarnings("unchecked")
	public Set<String> testIndexTokenizer(String tableName, String indexName, Map<String, Object> data) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		params.put("index", indexName);
		params.put("data", data);

		Message resp = session.rpc("com.logpresso.index.msgbus.ManagementPlugin.testIndexTokenizer", params);
		return new HashSet<String>((List<String>) resp.getParameters().get("tokens"));

	}

	@SuppressWarnings("unchecked")
	private IndexInfo getIndexInfo(Map<String, Object> m) {
		IndexInfo index = new IndexInfo();
		index.setTableName((String) m.get("table"));
		index.setIndexName((String) m.get("index"));
		index.setTokenizerName((String) m.get("tokenizer_name"));
		index.setTokenizerConfigs((Map<String, String>) m.get("tokenizer_configs"));

		try {
			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
			String s = (String) m.get("min_index_day");
			if (s != null)
				index.setMinIndexDay(f.parse(s));
		} catch (ParseException e) {
		}

		index.setUseBloomFilter((Boolean) m.get("use_bloom_filter"));
		index.setBloomFilterCapacity0((Integer) m.get("bf_lv0_capacity"));
		index.setBloomFilterErrorRate0((Double) m.get("bf_lv0_error_rate"));
		index.setBloomFilterCapacity1((Integer) m.get("bf_lv1_capacity"));
		index.setBloomFilterErrorRate1((Double) m.get("bf_lv1_error_rate"));
		index.setBasePath((String) m.get("base_path"));
		index.setBuildPastIndex((Boolean) m.get("build_past_index"));

		return index;
	}

	public void createIndex(IndexInfo info) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", info.getTableName());
		params.put("index", info.getIndexName());
		params.put("tokenizer_name", info.getTokenizerName());
		params.put("tokenizer_configs", info.getTokenizerConfigs());
		params.put("base_path", info.getBasePath());
		params.put("use_bloom_filter", info.isUseBloomFilter());
		params.put("bf_lv0_capacity", info.getBloomFilterCapacity0());
		params.put("bf_lv0_error_rate", info.getBloomFilterErrorRate0());
		params.put("bf_lv1_capacity", info.getBloomFilterCapacity1());
		params.put("bf_lv1_error_rate", info.getBloomFilterErrorRate1());
		params.put("min_index_day", info.getMinIndexDay());
		params.put("build_past_index", info.isBuildPastIndex());

		session.rpc("com.logpresso.index.msgbus.ManagementPlugin.createIndex", params);
	}

	public void dropIndex(String tableName, String indexName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		params.put("index", indexName);

		session.rpc("com.logpresso.index.msgbus.ManagementPlugin.dropIndex", params);
	}

	@SuppressWarnings("unchecked")
	public List<TableInfo> listTables() throws IOException {
		Message resp = session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.listTables");
		List<TableInfo> tables = new ArrayList<TableInfo>();
		Map<String, Object> metadataMap = (Map<String, Object>) resp.getParameters().get("tables");
		Map<String, Object> fieldsMap = (Map<String, Object>) resp.getParameters().get("fields");

		for (String tableName : metadataMap.keySet()) {
			Map<String, Object> params = (Map<String, Object>) metadataMap.get(tableName);
			List<Object> fields = (List<Object>) fieldsMap.get(tableName);
			TableInfo tableInfo = getTableInfo(tableName, params, fields);
			tables.add(tableInfo);
		}

		return tables;
	}

	@SuppressWarnings("unchecked")
	public TableInfo getTableInfo(String tableName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		Message resp = session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.getTableInfo", params);

		return getTableInfo(tableName, (Map<String, Object>) resp.get("table"), (List<Object>) resp.get("fields"));
	}

	private TableInfo getTableInfo(String tableName, Map<String, Object> params, List<Object> fields) {
		Map<String, String> metadata = new HashMap<String, String>();
		for (Entry<String, Object> pair : params.entrySet())
			metadata.put(pair.getKey(), pair.getValue() == null ? null : pair.getValue().toString());

		List<FieldInfo> fieldDefs = null;
		if (fields != null) {
			fieldDefs = new ArrayList<FieldInfo>();

			for (Object o : fields) {
				@SuppressWarnings("unchecked")
				Map<String, Object> m = (Map<String, Object>) o;
				FieldInfo f = new FieldInfo();
				f.setType((String) m.get("type"));
				f.setName((String) m.get("name"));
				f.setLength((Integer) m.get("length"));
				fieldDefs.add(f);
			}
		}

		TableInfo t = new TableInfo(tableName, metadata);
		t.getSchema().setFieldDefinitions(fieldDefs);
		return t;
	}

	public void setTableMetadata(String tableName, Map<String, String> config) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		params.put("metadata", config);

		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.setTableMetadata", params);
	}

	public void unsetTableMetadata(String tableName, Set<String> keySet) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		params.put("keys", keySet);

		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.unsetTableMetadata", params);
	}

	public void createTable(String tableName) throws IOException {
		createTable(tableName, null);
	}

	public void createTable(String tableName, Map<String, String> metadata) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		params.put("metadata", metadata);
		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.createTable", params);
	}

	public void dropTable(String tableName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("table", tableName);
		session.rpc("org.araqne.logdb.msgbus.ManagementPlugin.dropTable", params);
	}

	@SuppressWarnings("unchecked")
	public List<LoggerFactoryInfo> listLoggerFactories() throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("locale", locale.getLanguage());

		Message resp = session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.getLoggerFactories", params);

		List<LoggerFactoryInfo> factories = new ArrayList<LoggerFactoryInfo>();
		List<Object> l = (List<Object>) resp.get("factories");
		for (Object o : l) {
			parseLoggerFactoryInfo(factories, o);
		}

		return factories;
	}

	@SuppressWarnings("unchecked")
	public LoggerFactoryInfo getLoggerFactoryInfo(String factoryName) throws IOException {
		List<LoggerFactoryInfo> factories = listLoggerFactories();
		LoggerFactoryInfo found = null;

		for (LoggerFactoryInfo f : factories) {
			if (f.getNamespace().equals("local") && f.getName().equals(factoryName)) {
				found = f;
				break;
			}
		}

		if (found == null)
			throw new IllegalStateException("logger factory not found: " + factoryName);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("factory", factoryName);
		params.put("locale", locale.getLanguage());
		Message resp2 = session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.getFactoryOptions", params);
		List<ConfigSpec> configSpecs = parseConfigList((List<Object>) resp2.get("options"));
		found.setConfigSpecs(configSpecs);
		return found;
	}

	@SuppressWarnings("unchecked")
	private void parseLoggerFactoryInfo(List<LoggerFactoryInfo> factories, Object o) {
		Map<String, Object> m = (Map<String, Object>) o;

		LoggerFactoryInfo f = new LoggerFactoryInfo();
		f.setFullName((String) m.get("full_name"));
		f.setDisplayName((String) m.get("display_name"));
		f.setNamespace((String) m.get("namespace"));
		f.setName((String) m.get("name"));
		f.setDescription((String) m.get("description"));

		factories.add(f);
	}

	@SuppressWarnings("unchecked")
	public List<ParserFactoryInfo> listParserFactories() throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("locale", locale.getLanguage());

		Message resp = session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.getParserFactories", params);
		List<Object> l = (List<Object>) resp.get("factories");

		List<ParserFactoryInfo> parsers = new ArrayList<ParserFactoryInfo>();
		for (Object o : l) {
			Map<String, Object> m = (Map<String, Object>) o;

			ParserFactoryInfo f = new ParserFactoryInfo();
			f.setName((String) m.get("name"));
			f.setDisplayName((String) m.get("display_name"));
			f.setDescription((String) m.get("description"));
			f.setConfigSpecs(parseConfigList((List<Object>) m.get("options")));
			parsers.add(f);
		}

		return parsers;
	}

	@SuppressWarnings("unchecked")
	public ParserFactoryInfo getParserFactoryInfo(String name) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("factory_name", name);

		Message resp = session.rpc("org.logpresso.core.msgbus.ParserPlugin.getParserFactoryInfo", params);
		Map<String, Object> m = (Map<String, Object>) resp.get("factory");

		ParserFactoryInfo f = new ParserFactoryInfo();
		f.setName((String) m.get("name"));
		f.setDisplayName((String) m.get("display_name"));
		f.setDescription((String) m.get("description"));
		f.setConfigSpecs(parseConfigList((List<Object>) m.get("options")));
		return f;
	}

	@SuppressWarnings("unchecked")
	private List<ConfigSpec> parseConfigList(List<Object> l) {
		List<ConfigSpec> specs = new ArrayList<ConfigSpec>();

		for (Object o : l) {
			Map<String, Object> m = (Map<String, Object>) o;
			ConfigSpec spec = new ConfigSpec();
			spec.setName((String) m.get("name"));
			spec.setDescription((String) m.get("description"));
			spec.setDisplayName((String) m.get("display_name"));
			spec.setType((String) m.get("type"));
			spec.setRequired((Boolean) m.get("required"));
			spec.setDefaultValue((String) m.get("default_value"));
			specs.add(spec);
		}

		return specs;
	}

	/**
	 * @deprecated Use listParsers() instead
	 */
	@Deprecated
	public List<ParserInfo> getParsers() throws IOException {
		return listParsers();
	}

	@SuppressWarnings("unchecked")
	public List<ParserInfo> listParsers() throws IOException {
		Message resp = session.rpc("org.logpresso.core.msgbus.ParserPlugin.getParsers");
		List<Object> l = (List<Object>) resp.get("parsers");

		List<ParserInfo> parsers = new ArrayList<ParserInfo>();
		for (Object o : l) {
			parsers.add(parseParserInfo((Map<String, Object>) o));
		}

		return parsers;
	}

	public ParserInfo getParser(String name) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);
		Message resp = session.rpc("org.logpresso.core.msgbus.ParserPlugin.getParser", params);

		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) resp.get("parser");
		return parseParserInfo(m);
	}

	@SuppressWarnings("unchecked")
	private ParserInfo parseParserInfo(Map<String, Object> m) {
		ParserInfo p = new ParserInfo();
		p.setName((String) m.get("name"));
		p.setFactoryName((String) m.get("factory_name"));
		p.setConfigs((Map<String, String>) m.get("configs"));

		// since 0.9.0 and logpresso-core 0.8.0
		if (m.get("fields") != null) {
			List<FieldInfo> l = new ArrayList<FieldInfo>();
			for (Object o : (List<Object>) m.get("fields"))
				l.add(PrimitiveConverter.parse(FieldInfo.class, o));

			p.setFieldDefinitions(l);
		}

		return p;
	}

	public void createParser(ParserInfo parser) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", parser.getName());
		params.put("factory_name", parser.getFactoryName());
		params.put("configs", parser.getConfigs());

		session.rpc("org.logpresso.core.msgbus.ParserPlugin.createParser", params);
	}

	public void removeParser(String name) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);
		session.rpc("org.logpresso.core.msgbus.ParserPlugin.removeParser", params);
	}

	/**
	 * @since 0.8.1
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> testParser(String parserName, Map<String, Object> data) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", parserName);
		params.put("data", data);

		Message resp = session.rpc("org.logpresso.core.msgbus.ParserPlugin.testParser", params);
		return (List<Map<String, Object>>) resp.get("rows");
	}

	@SuppressWarnings("unchecked")
	public List<TransformerFactoryInfo> listTransformerFactories() throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("locale", locale.getLanguage());

		Message resp = session.rpc("org.logpresso.core.msgbus.TransformerPlugin.listTransformerFactories", params);
		List<Object> l = (List<Object>) resp.get("factories");

		List<TransformerFactoryInfo> factories = new ArrayList<TransformerFactoryInfo>();
		for (Object o : l) {
			Map<String, Object> m = (Map<String, Object>) o;

			TransformerFactoryInfo f = new TransformerFactoryInfo();
			f.setName((String) m.get("name"));
			f.setDisplayName((String) m.get("display_name"));
			f.setDescription((String) m.get("description"));
			f.setConfigSpecs(parseConfigList((List<Object>) m.get("options")));
			factories.add(f);
		}

		return factories;
	}

	@SuppressWarnings("unchecked")
	public TransformerFactoryInfo getTransformerFactoryInfo(String name) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("factory_name", name);
		params.put("locale", locale.getLanguage());

		Message resp = session.rpc("org.logpresso.core.msgbus.TransformerPlugin.getTransformerFactoryInfo", params);
		Map<String, Object> m = (Map<String, Object>) resp.get("factory");

		TransformerFactoryInfo f = new TransformerFactoryInfo();
		f.setName((String) m.get("name"));
		f.setDisplayName((String) m.get("display_name"));
		f.setDescription((String) m.get("description"));
		f.setConfigSpecs(parseConfigList((List<Object>) m.get("options")));
		return f;
	}

	/**
	 * @deprecated Use listTransformers() instead.
	 */
	@Deprecated
	public List<TransformerInfo> getTransformers() throws IOException {
		return listTransformers();
	}

	@SuppressWarnings("unchecked")
	public List<TransformerInfo> listTransformers() throws IOException {
		Message resp = session.rpc("org.logpresso.core.msgbus.TransformerPlugin.getTransformers");
		List<Object> l = (List<Object>) resp.get("transformers");

		List<TransformerInfo> transformers = new ArrayList<TransformerInfo>();
		for (Object o : l) {
			transformers.add(parseTransformerInfo((Map<String, Object>) o));
		}

		return transformers;
	}

	public TransformerInfo getTransformer(String name) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);
		Message resp = session.rpc("org.logpresso.core.msgbus.TransformerPlugin.getTransformer", params);

		@SuppressWarnings("unchecked")
		Map<String, Object> m = (Map<String, Object>) resp.get("transformer");
		return parseTransformerInfo(m);
	}

	@SuppressWarnings("unchecked")
	private TransformerInfo parseTransformerInfo(Map<String, Object> m) {
		TransformerInfo p = new TransformerInfo();
		p.setName((String) m.get("name"));
		p.setFactoryName((String) m.get("factory_name"));
		p.setConfigs((Map<String, String>) m.get("configs"));
		return p;
	}

	public void createTransformer(TransformerInfo transformer) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", transformer.getName());
		params.put("factory_name", transformer.getFactoryName());
		params.put("configs", transformer.getConfigs());

		session.rpc("org.logpresso.core.msgbus.TransformerPlugin.createTransformer", params);
	}

	public void removeTransformer(String name) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);
		session.rpc("org.logpresso.core.msgbus.TransformerPlugin.removeTransformer", params);
	}

	public List<LoggerInfo> listLoggers() throws IOException {
		return listLoggers(null);
	}

	@SuppressWarnings("unchecked")
	public List<LoggerInfo> listLoggers(List<String> loggerNames) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger_names", loggerNames);

		Message resp = session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.getLoggers", params);
		List<Object> l = (List<Object>) resp.get("loggers");

		List<LoggerInfo> loggers = new ArrayList<LoggerInfo>();
		for (Object o : l) {
			Map<String, Object> m = (Map<String, Object>) o;
			LoggerInfo lo = decodeLoggerInfo(m);
			loggers.add(lo);
		}

		return loggers;
	}

	/**
	 * Retrieve specific logger information with config using RPC call. States
	 * will not returned because logger states' size can be very large.
	 * 
	 * @since 0.8.6
	 */
	public LoggerInfo getLogger(String loggerName) throws IOException {
		return getLogger(loggerName, false);
	}

	/**
	 * @since 0.8.6
	 */
	@SuppressWarnings("unchecked")
	public LoggerInfo getLogger(String loggerName, boolean includeStates) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger_name", loggerName);
		params.put("include_configs", true);
		params.put("include_states", includeStates);

		Message resp = session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.getLogger", params);
		Map<String, Object> m = (Map<String, Object>) resp.get("logger");
		if (m == null)
			return null;

		return decodeLoggerInfo(m);
	}

	@SuppressWarnings("unchecked")
	private LoggerInfo decodeLoggerInfo(Map<String, Object> m) {
		LoggerInfo lo = new LoggerInfo();
		lo.setNamespace((String) m.get("namespace"));
		lo.setName((String) m.get("name"));
		lo.setFactoryName((String) m.get("factory_full_name"));
		lo.setDescription((String) m.get("description"));
		lo.setPassive((Boolean) m.get("is_passive"));
		lo.setInterval((Integer) m.get("interval"));
		lo.setStatus((String) m.get("status"));
		lo.setLastStartAt(parseDate((String) m.get("last_start")));
		lo.setLastRunAt(parseDate((String) m.get("last_run")));
		lo.setLastLogAt(parseDate((String) m.get("last_log")));
		lo.setLogCount(Long.valueOf(m.get("log_count").toString()));

		Object dropCount = m.get("drop_count");
		if (dropCount != null)
			lo.setDropCount(Long.valueOf(dropCount.toString()));

		Object updateCount = m.get("update_count");
		if (updateCount != null)
			lo.setUpdateCount(Long.valueOf(updateCount.toString()));

		lo.setConfigs((Map<String, String>) m.get("configs"));
		lo.setStates((Map<String, Object>) m.get("states"));
		return lo;
	}

	private Date parseDate(String s) {
		if (s == null)
			return null;

		try {
			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
			return f.parse(s);
		} catch (ParseException e) {
			return null;
		}
	}

	public void createLogger(LoggerInfo logger) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("factory", logger.getFactoryName());
		params.put("namespace", logger.getNamespace());
		params.put("name", logger.getName());
		params.put("description", logger.getDescription());
		params.put("options", logger.getConfigs());

		session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.createLogger", params);
	}

	public void removeLogger(String fullName) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger", fullName);
		session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.removeLogger", params);
	}

	public void startLogger(String fullName, int interval) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger", fullName);
		params.put("interval", interval);
		session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.startLogger", params);
	}

	public void stopLogger(String fullName, int waitTime) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("logger", fullName);
		params.put("wait_time", waitTime);
		session.rpc("org.araqne.log.api.msgbus.LoggerPlugin.stopLogger", params);
	}

	@SuppressWarnings("unchecked")
	public List<JdbcProfileInfo> listJdbcProfiles() throws IOException {
		List<JdbcProfileInfo> l = new ArrayList<JdbcProfileInfo>();

		Message resp = session.rpc("org.logpresso.jdbc.JdbcProfilePlugin.getProfiles");

		List<Object> profiles = (List<Object>) resp.get("profiles");

		for (Object o : profiles) {
			Map<String, Object> m = (Map<String, Object>) o;
			JdbcProfileInfo info = new JdbcProfileInfo();
			info.setName((String) m.get("name"));
			info.setConnectionString((String) m.get("connection_string"));
			info.setReadOnly((Boolean) m.get("readonly"));
			info.setUser((String) m.get("user"));
			l.add(info);
		}

		return l;
	}

	public void createJdbcProfile(JdbcProfileInfo profile) throws IOException {
		checkNotNull("profile", profile);
		checkNotNull("profile.name", profile.getName());
		checkNotNull("profile.connectionString", profile.getConnectionString());
		checkNotNull("profile.user", profile.getUser());

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", profile.getName());
		params.put("connection_string", profile.getConnectionString());
		params.put("readonly", profile.isReadOnly());
		params.put("user", profile.getUser());
		params.put("password", profile.getPassword());

		session.rpc("org.logpresso.jdbc.JdbcProfilePlugin.createProfile", params);
	}

	public void removeJdbcProfile(String name) throws IOException {
		checkNotNull("name", name);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("name", name);

		session.rpc("org.logpresso.jdbc.JdbcProfilePlugin.removeProfile", params);
	}

	public LogCursor query(String queryString) throws IOException {
		int id = createQuery(queryString);
		startQuery(id);
		LogQuery q = queries.get(id);
		q.waitUntil(null);
		if (q.getStatus().equals("Cancelled"))
			throw new IllegalStateException("query cancelled, id [" + q.getId() + "] query string [" + queryString + "]");

		long total = q.getLoadedCount();

		return new LogCursorImpl(id, 0L, total, true, fetchSize);
	}

	private class LogCursorImpl implements LogCursor {

		private int id;
		private long offset;
		private long limit;
		private boolean removeOnClose;

		private long p;
		private Map<String, Object> cached;
		private Long currentCacheOffset;
		private Long nextCacheOffset;
		private int fetchUnit;
		private Map<String, Object> prefetch;

		public LogCursorImpl(int id, long offset, long limit, boolean removeOnClose, int fetchUnit) {
			this.id = id;
			this.offset = offset;
			this.limit = limit;
			this.removeOnClose = removeOnClose;

			this.p = offset;
			this.nextCacheOffset = offset;
			this.fetchUnit = fetchUnit;
		}

		@SuppressWarnings("unchecked")
		@Override
		public boolean hasNext() {
			if (prefetch != null)
				return true;

			if (p < offset || p >= offset + limit)
				return false;

			try {
				if (cached == null || p >= currentCacheOffset + fetchUnit) {
					cached = getResult(id, nextCacheOffset, fetchUnit);
					currentCacheOffset = nextCacheOffset;
					nextCacheOffset += fetchUnit;
				}

				int relative = (int) (p - currentCacheOffset);
				List<Object> l = (List<Object>) cached.get("result");
				if (relative >= l.size())
					return false;

				prefetch = (Map<String, Object>) l.get(relative);
				p++;
				return true;
			} catch (IOException e) {
				logger.error("araqne logdb client: cannot fetch log query result", e);
				return false;
			}
		}

		@Override
		public Map<String, Object> next() {
			if (!hasNext())
				throw new NoSuchElementException("end of log cursor");

			Map<String, Object> m = prefetch;
			prefetch = null;
			return m;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws IOException {
			if (removeOnClose)
				removeQuery(id);
		}
	}

	public int createQuery(String queryString) throws IOException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("query", queryString);

		Message resp = session.rpc("org.araqne.logdb.msgbus.LogQueryPlugin.createQuery", params);
		int id = resp.getInt("id");
		session.registerTrap("logstorage-query-" + id);
		session.registerTrap("logstorage-query-timeline-" + id);

		queries.putIfAbsent(id, new LogQuery(this, id, queryString));
		return id;
	}

	public void startQuery(int id) throws IOException {
		startQuery(id, 10, null);
	}

	public void startQuery(int id, int pageSize, Integer timelineSize) throws IOException {
		verifyQueryId(id);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);
		params.put("offset", 0);
		params.put("limit", pageSize);

		// timeline may degrade little performance
		params.put("timeline_limit", timelineSize);

		session.rpc("org.araqne.logdb.msgbus.LogQueryPlugin.startQuery", params);
	}

	public void stopQuery(int id) throws IOException {
		verifyQueryId(id);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);

		session.rpc("org.araqne.logdb.msgbus.LogQueryPlugin.stopQuery", params);
	}

	public void removeQuery(int id) throws IOException {
		verifyQueryId(id);

		session.unregisterTrap("logstorage-query-" + id);
		session.unregisterTrap("logstorage-query-timeline-" + id);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);
		session.rpc("org.araqne.logdb.msgbus.LogQueryPlugin.removeQuery", params);

		queries.remove(id);
	}

	public void waitUntil(int id, Long count) {
		verifyQueryId(id);
		queries.get(id).waitUntil(count);
	}

	public Map<String, Object> getResult(int id, long offset, int limit) throws IOException {
		verifyQueryId(id);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);
		params.put("offset", offset);
		params.put("limit", limit);
		params.put("binary_encode", true);

		Message resp = session.rpc("org.araqne.logdb.msgbus.LogQueryPlugin.getResult", params);
		if (resp.getParameters().size() == 0)
			throw new MessageException("query-not-found", "", resp.getParameters());

		// support backward compatibility
		if (!resp.getParameters().containsKey("uncompressed_size"))
			return resp.getParameters();

		// decompress and decode
		int uncompressedSize = (Integer) resp.getParameters().get("uncompressed_size");
		String binary = (String) resp.getParameters().get("binary");
		return decodeBinary(binary, uncompressedSize);
	}

	private Map<String, Object> decodeBinary(String binary, int uncompressedSize) {
		byte[] b = Base64.decode(binary);
		byte[] uncompressed = new byte[uncompressedSize];
		uncompress(uncompressed, b);

		Map<String, Object> m = EncodingRule.decodeMap(ByteBuffer.wrap(uncompressed));

		Object[] resultArray = (Object[]) m.get("result");
		List<Object> resultList = new ArrayList<Object>(resultArray.length);
		for (int i = 0; i < resultArray.length; i++)
			resultList.add(resultArray[i]);

		m.put("result", resultList);

		return m;
	}

	private void uncompress(byte[] output, byte[] b) {
		Inflater inflater = new Inflater();
		inflater.setInput(b, 0, b.length);
		try {
			inflater.inflate(output);
			inflater.reset();
		} catch (DataFormatException e) {
			throw new IllegalStateException(e);
		} finally {
			inflater.end();
		}
	}

	private void verifyQueryId(int id) {
		if (!queries.containsKey(id))
			throw new MessageException("query-not-found", "query [" + id + "] does not exist", null);
	}

	public void close() throws IOException {
		if (session != null)
			session.close();
	}

	@Override
	public void onTrap(Message msg) {
		if (msg.getMethod().startsWith("logstorage-query-timeline")) {
			int id = msg.getInt("id");
			LogQuery q = queries.get(id);
			q.updateCount(msg.getLong("count"));
			if (msg.getString("type").equals("eof"))
				q.updateStatus("Ended");
		} else if (msg.getMethod().startsWith("logstorage-query")) {
			int id = msg.getInt("id");
			LogQuery q = queries.get(id);
			if (msg.getString("type").equals("eof")) {
				q.updateCount(msg.getLong("total_count"));
				q.updateStatus("Ended");
			} else if (msg.getString("type").equals("page_loaded")) {
				q.updateCount(msg.getLong("count"));
				q.updateStatus("Running");
			} else if (msg.getString("type").equals("status_change")) {
				q.updateCount(msg.getLong("count"));
				q.updateStatus(msg.getString("status"));
			}
		}
	}

	@Override
	public void onClose(Throwable t) {
		for (LogQuery q : queries.values())
			q.updateStatus("Cancelled");
	}

	private void checkNotNull(String name, Object o) {
		if (o == null)
			throw new IllegalArgumentException(name + " parameter should be not null");
	}
}

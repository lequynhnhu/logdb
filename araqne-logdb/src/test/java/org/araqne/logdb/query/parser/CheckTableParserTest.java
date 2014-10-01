package org.araqne.logdb.query.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import org.araqne.logdb.QueryContext;
import org.araqne.logdb.QueryParserService;
import org.araqne.logdb.Session;
import org.araqne.logdb.impl.FunctionRegistryImpl;
import org.araqne.logdb.query.command.CheckTable;
import org.araqne.logdb.query.engine.QueryParserServiceImpl;
import org.araqne.logstorage.LogTableRegistry;
import org.junit.Before;
import org.junit.Test;

public class CheckTableParserTest {
	private QueryParserService queryParserService;

	@Before
	public void setup() {
		QueryParserServiceImpl p = new QueryParserServiceImpl();
		p.setFunctionRegistry(new FunctionRegistryImpl());
		queryParserService = p;
	}
	
	@Test
	public void checkAll() {
		String query = "checktable";

		CheckTable c = parse(query);
		assertEquals(3, c.getTableNames().size());
		assertTrue(c.getTableNames().contains("secure_log"));
		assertTrue(c.getTableNames().contains("secure_event"));
		assertTrue(c.getTableNames().contains("text_log"));
		assertNull(c.getFrom());
		assertNull(c.getTo());
		assertEquals("checktable", c.toString());
	}

	@Test
	public void checkAll2() {
		CheckTable c = parse("checktable *");
		assertEquals(3, c.getTableNames().size());
		assertTrue(c.getTableNames().contains("secure_log"));
		assertTrue(c.getTableNames().contains("secure_event"));
		assertTrue(c.getTableNames().contains("text_log"));
		assertNull(c.getFrom());
		assertNull(c.getTo());
		assertEquals("checktable *", c.toString());
	}

	@Test
	public void checkSomeTables() {
		String query = "checktable text_log, secure_log";

		CheckTable c = parse(query);
		assertEquals(2, c.getTableNames().size());
		assertTrue(c.getTableNames().contains("secure_log"));
		assertTrue(c.getTableNames().contains("text_log"));
		assertNull(c.getFrom());
		assertNull(c.getTo());

		assertEquals(query, c.toString());
	}

	@Test
	public void checkWildMatch() {
		String query = "checktable secure_*";
		CheckTable c = parse(query);
		assertEquals(2, c.getTableNames().size());
		assertTrue(c.getTableNames().contains("secure_log"));
		assertTrue(c.getTableNames().contains("secure_event"));
		assertNull(c.getFrom());
		assertNull(c.getTo());

		assertEquals(query, c.toString());
	}

	@Test
	public void checkFromTo() {
		String query = "checktable from=20130806 to=20130808 secure_*";
		CheckTable c = parse(query);

		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		assertEquals(2, c.getTableNames().size());
		assertTrue(c.getTableNames().contains("secure_log"));
		assertTrue(c.getTableNames().contains("secure_event"));
		assertEquals("20130806", df.format(c.getFrom()));
		assertEquals("20130808", df.format(c.getTo()));
		
		assertEquals(query, c.toString());
	}

	private CheckTable parse(String query) {
		LogTableRegistry tableRegistry = mock(LogTableRegistry.class);
		when(tableRegistry.getTableNames()).thenReturn(Arrays.asList("secure_log", "secure_event", "text_log"));

		CheckTableParser parser = new CheckTableParser("checktable", tableRegistry, null, null);
		parser.setQueryParserService(queryParserService);
		
		CheckTable c = (CheckTable) parser.parse(getContext(), query);
		return c;
	}

	private QueryContext getContext() {
		Session session = mock(Session.class);
		when(session.isAdmin()).thenReturn(true);
		QueryContext ctx = new QueryContext(session);
		return ctx;
	}

}
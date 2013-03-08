/*
 * Copyright 2011 Future Systems
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
package org.araqne.logdb.impl;

import java.io.File;
import java.io.IOException;

import org.araqne.api.PathAutoCompleter;
import org.araqne.api.Script;
import org.araqne.api.ScriptArgument;
import org.araqne.api.ScriptContext;
import org.araqne.api.ScriptUsage;
import org.araqne.logdb.AccountService;
import org.araqne.logdb.CsvLookupRegistry;
import org.araqne.logdb.LogQueryScriptFactory;
import org.araqne.logdb.LogQueryScriptRegistry;
import org.araqne.logdb.LogQueryService;
import org.araqne.logdb.LookupHandlerRegistry;
import org.araqne.logdb.Session;

public class LogDBScript implements Script {
	private LogQueryService qs;
	private LogQueryScriptRegistry scriptRegistry;
	private CsvLookupRegistry csvRegistry;
	private ScriptContext context;
	private LookupHandlerRegistry lookup;
	private AccountService accountService;

	public LogDBScript(LogQueryService qs, LogQueryScriptRegistry scriptRegistry, LookupHandlerRegistry lookup,
			CsvLookupRegistry csvRegistry, AccountService accountService) {
		this.qs = qs;
		this.scriptRegistry = scriptRegistry;
		this.lookup = lookup;
		this.csvRegistry = csvRegistry;
		this.accountService = accountService;
	}

	@Override
	public void setScriptContext(ScriptContext context) {
		this.context = context;
	}

	public void sessions(String[] args) {
		context.println("Current Sessions");
		context.println("------------------");
		for (Session session : accountService.getSessions()) {
			context.println(session);
		}
	}

	public void accounts(String[] args) {
		context.println("Accounts");
		context.println("----------");
		for (String loginName : accountService.getAccountNames()) {
			context.println(loginName);
		}
	}

	@ScriptUsage(description = "open console", arguments = { @ScriptArgument(name = "login name", type = "string", description = "db account name") })
	public void console(String[] args) {
		new Console(context, accountService, qs).run(args[0]);
	}

	public void csvLookups(String[] args) {
		context.println("CSV Mapping Files");
		context.println("-------------------");
		for (File f : csvRegistry.getCsvFiles()) {
			context.println(f.getAbsolutePath());
		}
	}

	@ScriptUsage(description = "create new log query script workspace", arguments = { @ScriptArgument(name = "workspace name", type = "string", description = "log query script workspace name") })
	public void createScriptWorkspace(String[] args) {
		scriptRegistry.createWorkspace(args[0]);
		context.println("created");
	}

	@ScriptUsage(description = "remove log query script workspace", arguments = { @ScriptArgument(name = "workspace name", type = "string", description = "log query script workspace name") })
	public void dropScriptWorkspace(String[] args) {
		scriptRegistry.dropWorkspace(args[0]);
		context.println("dropped");
	}

	@ScriptUsage(description = "load csv lookup mapping file", arguments = { @ScriptArgument(name = "path", type = "string", description = "csv (comma separated value) file path. first line should be column headers.", autocompletion = PathAutoCompleter.class) })
	public void loadCsvLookup(String[] args) throws IOException {
		try {
			File f = new File(args[0]);
			csvRegistry.loadCsvFile(f);
			context.println("loaded " + f.getAbsolutePath());
		} catch (IllegalStateException e) {
			context.println(e);
		}
	}

	@ScriptUsage(description = "reload csv lookup mapping file", arguments = { @ScriptArgument(name = "path", type = "string", description = "csv (comma separated value) file path. first line should be column headers.", autocompletion = PathAutoCompleter.class) })
	public void reloadCsvLookup(String[] args) throws IOException {
		try {
			File f = new File(args[0]);
			csvRegistry.unloadCsvFile(f);
			csvRegistry.loadCsvFile(f);
			context.println("reloaded");
		} catch (IllegalStateException e) {
			context.println(e);
		}
	}

	@ScriptUsage(description = "unload csv lookup mapping file", arguments = { @ScriptArgument(name = "path", type = "string", description = "registered csv file path", autocompletion = PathAutoCompleter.class) })
	public void unloadCsvLookup(String[] args) {
		File f = new File(args[0]);
		csvRegistry.unloadCsvFile(f);
		context.println("unloaded" + f.getAbsolutePath());
	}

	public void scripts(String[] args) {
		context.println("Log Scripts");
		context.println("--------------");

		for (String workspace : scriptRegistry.getWorkspaceNames()) {
			context.println("Workspace: " + workspace);
			for (String name : scriptRegistry.getScriptFactoryNames(workspace)) {
				LogQueryScriptFactory factory = scriptRegistry.getScriptFactory(workspace, name);
				context.println("  " + name + " - " + factory);
			}
		}
	}

	/**
	 * print all connected nodes
	 */
	public void nodes(String[] args) {
		context.println("Log DB Nodes");
		context.println("--------------");

	}

	public void lookuphandlers(String[] args) {
		context.println("Lookup Handlers");
		context.println("---------------------");
		for (String name : lookup.getLookupHandlerNames())
			context.println(name);
	}
}

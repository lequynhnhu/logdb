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
package org.araqne.logdb.msgbus;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.araqne.logdb.AccountService;
import org.araqne.logstorage.LogStorage;
import org.araqne.logstorage.LogTableRegistry;
import org.araqne.msgbus.MessageBus;
import org.araqne.msgbus.MsgbusException;
import org.araqne.msgbus.Request;
import org.araqne.msgbus.Response;
import org.araqne.msgbus.Session;
import org.araqne.msgbus.handler.AllowGuestAccess;
import org.araqne.msgbus.handler.CallbackType;
import org.araqne.msgbus.handler.MsgbusMethod;
import org.araqne.msgbus.handler.MsgbusPlugin;

@Component(name = "logdb-management-msgbus")
@MsgbusPlugin
public class ManagementPlugin {

	@Requires
	private AccountService accountService;

	@Requires
	private LogTableRegistry tableRegistry;

	@Requires
	private MessageBus msgbus;

	@Requires
	private LogStorage storage;

	@AllowGuestAccess
	@MsgbusMethod
	public void login(Request req, Response resp) {
		Session session = req.getSession();

		if (session.get("araqne_logdb_session") != null)
			throw new MsgbusException("logdb", "already-logon");

		String loginName = req.getString("login_name");
		String password = req.getString("password");

		org.araqne.logdb.Session dbSession = accountService.login(loginName, password);

		if (session.getOrgDomain() == null && session.getAdminLoginName() == null) {
			session.setProperty("org_domain", "localhost");
			session.setProperty("admin_login_name", loginName);
			session.setProperty("auth", "logdb");
		}

		session.setProperty("araqne_logdb_session", dbSession);
	}

	@MsgbusMethod
	public void logout(Request req, Response resp) {
		Session session = req.getSession();
		org.araqne.logdb.Session dbSession = (org.araqne.logdb.Session) session.get("araqne_logdb_session");
		if (dbSession != null)
			accountService.logout(dbSession);

		String auth = session.getString("auth");
		if (auth != null && auth.equals("logdb"))
			msgbus.closeSession(session);
	}

	@MsgbusMethod(type = CallbackType.SessionClosed)
	public void onClose(Session session) {
		if (session == null)
			return;

		org.araqne.logdb.Session dbSession = (org.araqne.logdb.Session) session.get("araqne_logdb_session");
		if (dbSession != null)
			accountService.logout(dbSession);

	}

	@MsgbusMethod
	public void listTables(Request req, Response resp) {
		for (String tableName : tableRegistry.getTableNames()) {

		}
	}

	@MsgbusMethod
	public void createTable(Request req, Response resp) {
		checkPermission(req);
		String tableName = req.getString("table");
		storage.createTable(tableName);
	}

	@MsgbusMethod
	public void dropTable(Request req, Response resp) {
		checkPermission(req);
		String tableName = req.getString("table");
		storage.dropTable(tableName);
	}

	private void checkPermission(Request req) {
		org.araqne.logdb.Session session = (org.araqne.logdb.Session) req.getSession().get("araqne_logdb_session");
		if (session != null && !session.getLoginName().equals("araqne"))
			throw new SecurityException("logdb management is not allowed to " + session.getLoginName());
	}
}

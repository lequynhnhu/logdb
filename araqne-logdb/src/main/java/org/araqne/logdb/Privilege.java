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
package org.araqne.logdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Privilege {
	private String loginName;
	private String tableName;
	private List<Permission> permissions = new ArrayList<Permission>();

	public Privilege() {
	}

	public Privilege(String loginName, String tableName, Permission... permissions) {
		this.loginName = loginName;
		this.tableName = tableName;
		this.permissions = Arrays.asList(permissions);
	}

	public Privilege(String loginName, String tableName, List<Permission> permissions) {
		this.loginName = loginName;
		this.tableName = tableName;
		this.permissions = permissions;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public boolean hasPermission(Permission permission) {
		for (Permission p : permissions)
			if (p.equals(permission))
				return true;
		return false;
	}

	public List<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(List<Permission> permissions) {
		this.permissions = permissions;
	}
}

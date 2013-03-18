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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.araqne.api.DateFormat;

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
public class LoggerInfo {
	private String factoryName;
	private String namespace;
	private String name;
	private String description;
	private boolean passive;
	private int interval;
	private String status;
	private Date lastStartAt;
	private Date lastRunAt;
	private Date lastLogAt;
	private long logCount;

	private Map<String, String> configs = new HashMap<String, String>();

	public String getFactoryName() {
		return factoryName;
	}

	public void setFactoryName(String factoryName) {
		this.factoryName = factoryName;
	}

	public String getFullName() {
		return namespace + "\\" + name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, String> getConfigs() {
		return configs;
	}

	public void setConfigs(Map<String, String> configs) {
		this.configs = configs;
	}

	public boolean isPassive() {
		return passive;
	}

	public void setPassive(boolean passive) {
		this.passive = passive;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getLastStartAt() {
		return lastStartAt;
	}

	public void setLastStartAt(Date lastStartAt) {
		this.lastStartAt = lastStartAt;
	}

	public Date getLastRunAt() {
		return lastRunAt;
	}

	public void setLastRunAt(Date lastRunAt) {
		this.lastRunAt = lastRunAt;
	}

	public Date getLastLogAt() {
		return lastLogAt;
	}

	public void setLastLogAt(Date lastLogAt) {
		this.lastLogAt = lastLogAt;
	}

	public long getLogCount() {
		return logCount;
	}

	public void setLogCount(long logCount) {
		this.logCount = logCount;
	}

	@Override
	public String toString() {
		String format = "yyyy-MM-dd HH:mm:ss";
		String start = DateFormat.format(format, lastStartAt);
		String run = DateFormat.format(format, lastRunAt);
		String log = DateFormat.format(format, lastLogAt);
		String status = getStatus().toString().toLowerCase();
		if (passive)
			status += " (passive)";
		else
			status += " (interval=" + interval + "ms)";

		return String.format("name=%s, factory=%s, status=%s, log count=%d, last start=%s, last run=%s, last log=%s",
				getFullName(), factoryName, status, getLogCount(), start, run, log);
	}
}
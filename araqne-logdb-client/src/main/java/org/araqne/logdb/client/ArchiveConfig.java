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

import java.util.Map;

/**
 * 로그 수집기에서 수집되는 데이터를 실시간으로 테이블에 저장하도록 하는 테이블 저장 설정을 표현합니다.
 * 
 * @author xeraph@eediom.com
 * 
 */
public class ArchiveConfig {
	private String loggerName;
	private String tableName;
	private String host;

	/**
	 * monitor primary logger and replicate primary logger status.
	 * node-name\namespace\name format.
	 * 
	 * @since 0.8.6
	 */
	private String primaryLogger;

	/**
	 * check backup logger status when system has been recovered.
	 * node-name\namespace\name format.
	 * 
	 * @since 0.8.6
	 */
	private String backupLogger;

	private Map<String, String> metadata;
	private boolean enabled;

	public String getLoggerName() {
		return loggerName;
	}

	public void setLoggerName(String loggerName) {
		this.loggerName = loggerName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPrimaryLogger() {
		return primaryLogger;
	}

	public void setPrimaryLogger(String primaryLogger) {
		this.primaryLogger = primaryLogger;
	}

	public String getBackupLogger() {
		return backupLogger;
	}

	public void setBackupLogger(String backupLogger) {
		this.backupLogger = backupLogger;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, String> metadata) {
		this.metadata = metadata;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public String toString() {
		return "logger=" + loggerName + ", table=" + tableName + ", enabled=" + enabled + ", host=" + host + ", primary logger="
				+ primaryLogger + ", backup logger=" + backupLogger + ", metadata=" + metadata;
	}
}

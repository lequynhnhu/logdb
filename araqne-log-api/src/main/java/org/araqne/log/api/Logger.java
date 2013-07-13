/*
 * Copyright 2010 NCHOVY
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
package org.araqne.log.api;

import java.util.Date;
import java.util.Map;

public interface Logger {
	String getFullName();

	String getNamespace();

	String getName();

	String getFactoryFullName();

	String getFactoryName();

	String getFactoryNamespace();

	String getDescription();

	boolean isPassive();

	void setPassive(boolean isPassive);

	Date getLastStartDate();

	Date getLastRunDate();

	Date getLastLogDate();

	/**
	 * @since 2.3.1
	 */
	long getDropCount();

	long getLogCount();

	boolean isRunning();

	boolean isPending();

	void setPending(boolean pending);

	/**
	 * @since 2.4.0
	 */
	boolean isManualStart();

	/**
	 * @since 2.4.0
	 */
	void setManualStart(boolean manualStart);

	LoggerStatus getStatus();

	int getInterval();

	void start();

	void start(int interval);

	void stop();

	void stop(int maxWaitTime);

	void addLogPipe(LogPipe pipe);

	void removeLogPipe(LogPipe pipe);

	void addEventListener(LoggerEventListener callback);

	void removeEventListener(LoggerEventListener callback);

	void clearEventListeners();

	void updateConfig(Map<String, String> config);

	Map<String, String> getConfig();

	LogTransformer getTransformer();

	void setTransformer(LogTransformer transformer);
}

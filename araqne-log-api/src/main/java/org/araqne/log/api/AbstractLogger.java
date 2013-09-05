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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.araqne.api.DateFormat;

public abstract class AbstractLogger implements Logger, Runnable {
	private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AbstractLogger.class.getName());
	private static final int INFINITE = 0;
	private String fullName;
	private String namespace;
	private String name;
	private String factoryFullName;
	private String factoryNamespace;
	private String factoryName;
	private String description;
	private boolean isPassive;
	private volatile LogPipe[] pipes;
	private Object updateLock = new Object();
	private Thread t;
	private int interval;
	private Map<String, String> config;

	private volatile LoggerStatus status = LoggerStatus.Stopped;
	private volatile boolean doStop = false;
	private volatile boolean stopped = true;
	private volatile boolean pending = false;
	private volatile boolean manualStart = false;

	private volatile Date lastStartDate;
	private volatile Date lastRunDate;
	private volatile Date lastLogDate;
	private AtomicLong logCounter;
	private AtomicLong dropCounter;

	private Set<LoggerEventListener> eventListeners;
	private LogTransformer transformer;

	/**
	 * @since 1.7.0
	 */
	public AbstractLogger(LoggerSpecification spec, LoggerFactory factory) {
		this(spec.getNamespace(), spec.getName(), spec.getDescription(), factory, spec.getLogCount(), spec.getLastLogDate(), spec
				.getConfig());
		this.interval = spec.getInterval();
	}

	@Deprecated
	public AbstractLogger(String name, String description, LoggerFactory loggerFactory) {
		this(name, description, loggerFactory, new HashMap<String, String>());
	}

	@Deprecated
	public AbstractLogger(String name, String description, LoggerFactory loggerFactory, Map<String, String> config) {
		this("local", name, loggerFactory.getNamespace(), loggerFactory.getName(), description, config);
	}

	@Deprecated
	public AbstractLogger(String namespace, String name, String description, LoggerFactory loggerFactory) {
		this(namespace, name, description, loggerFactory, new HashMap<String, String>());
	}

	@Deprecated
	public AbstractLogger(String namespace, String name, String description, LoggerFactory loggerFactory,
			Map<String, String> config) {
		this(namespace, name, loggerFactory.getNamespace(), loggerFactory.getName(), description, config);
	}

	@Deprecated
	public AbstractLogger(String namespace, String name, String description, LoggerFactory loggerFactory, long logCount,
			Date lastLogDate, Map<String, String> config) {
		this(namespace, name, loggerFactory.getNamespace(), loggerFactory.getName(), description, logCount, lastLogDate, config);
	}

	@Deprecated
	public AbstractLogger(String namespace, String name, String factoryNamespace, String factoryName, Map<String, String> config) {
		this(namespace, name, factoryNamespace, factoryName, "", config);
	}

	@Deprecated
	public AbstractLogger(String namespace, String name, String factoryNamespace, String factoryName, String description,
			Map<String, String> config) {
		this(namespace, name, factoryNamespace, factoryName, description, 0, null, config);
	}

	public AbstractLogger(String namespace, String name, String factoryNamespace, String factoryName, String description,
			long logCount, Date lastLogDate, Map<String, String> config) {
		// logger info
		this.namespace = namespace;
		this.name = name;
		this.fullName = namespace + "\\" + name;
		this.description = description;
		this.config = config;

		// logger factory info
		this.factoryNamespace = factoryNamespace;
		this.factoryName = factoryName;
		this.factoryFullName = factoryNamespace + "\\" + factoryName;

		this.logCounter = new AtomicLong(logCount);
		this.dropCounter = new AtomicLong(0);
		this.lastLogDate = lastLogDate;
		this.pipes = new LogPipe[0];

		this.eventListeners = Collections.newSetFromMap(new ConcurrentHashMap<LoggerEventListener, Boolean>());
	}

	@Override
	public String getFullName() {
		return fullName;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getFactoryFullName() {
		return factoryFullName;
	}

	@Override
	public String getFactoryName() {
		return factoryName;
	}

	@Override
	public String getFactoryNamespace() {
		return factoryNamespace;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public boolean isPassive() {
		return isPassive;
	}

	@Override
	public void setPassive(boolean isPassive) {
		if (!stopped)
			throw new IllegalStateException("logger is running");

		this.isPassive = isPassive;
		if (isPassive)
			lastRunDate = null;
	}

	@Override
	public Date getLastStartDate() {
		return lastStartDate;
	}

	@Override
	public Date getLastRunDate() {
		return lastRunDate;
	}

	@Override
	public Date getLastLogDate() {
		return lastLogDate;
	}

	@Override
	public long getLogCount() {
		return logCounter.get();
	}

	@Override
	public long getDropCount() {
		return dropCounter.get();
	}

	@Override
	public boolean isRunning() {
		return !stopped;
	}

	public boolean isPending() {
		return pending;
	}

	@Override
	public void setPending(boolean pending) {
		this.pending = pending;
	}

	@Override
	public boolean isManualStart() {
		return manualStart;
	}

	@Override
	public void setManualStart(boolean manualStart) {
		this.manualStart = manualStart;
		updateConfig(config);
	}

	@Override
	public LoggerStatus getStatus() {
		return status;
	}

	@Override
	public int getInterval() {
		return interval;
	}

	/**
	 * start passive logger
	 */
	@Override
	public void start() {
		verifyTransformer();

		if (!isPassive())
			throw new IllegalStateException("not passive mode. use start(interval)");

		pending = false;
		stopped = false;
		invokeStartCallback();
	}

	/**
	 * start active logger
	 */
	@Override
	public void start(int interval) {
		verifyTransformer();

		if (isPassive()) {
			start();
			return;
		}

		if (!stopped)
			throw new IllegalStateException("logger is already running");

		status = LoggerStatus.Starting;
		this.interval = interval;

		onStart();

		if (getExecutor() == null) {
			t = new Thread(this, "Logger [" + fullName + "]");
			t.start();
		} else {
			stopped = false;
			getExecutor().execute(this);
		}

		pending = false;
		invokeStartCallback();
	}

	private void verifyTransformer() {
		if (config.get("transformer") != null && transformer == null)
			throw new IllegalStateException("pending transformer");
	}

	protected ExecutorService getExecutor() {
		return null;
	}

	private void invokeStartCallback() {
		onStart();

		lastStartDate = new Date();
		status = LoggerStatus.Running;

		for (LoggerEventListener callback : eventListeners) {
			try {
				callback.onStart(this);
			} catch (Exception e) {
				log.warn("logger callback should not throw any exception", e);
			}
		}
	}

	@Override
	public void stop() {
		stop(false);
	}

	private void stop(boolean pending) {
		if (isPassive()) {
			stopped = true;
			status = LoggerStatus.Stopped;
			this.pending = pending;
			invokeStopCallback();
		} else
			stop(INFINITE);
	}

	@Override
	public void stop(int maxWaitTime) {
		stop(maxWaitTime, false);
	}

	private void stop(int maxWaitTime, boolean pending) {
		if (isPassive()) {
			stop(pending);
			return;
		}

		if (t != null) {
			if (!t.isAlive()) {
				t = null;
				return;
			}
			t.interrupt();
			t = null;
		}

		status = LoggerStatus.Stopping;

		if (getExecutor() == null) {
			doStop = true;
			long begin = new Date().getTime();
			try {
				while (true) {
					if (stopped)
						break;

					if (maxWaitTime != 0 && new Date().getTime() - begin > maxWaitTime)
						break;

					Thread.sleep(50);
				}

				status = LoggerStatus.Stopped;
				stopped = true;
			} catch (InterruptedException e) {
			}
		} else {
			status = LoggerStatus.Stopped;
			stopped = true;
		}

		this.pending = pending;
		invokeStopCallback();
	}

	/**
	 * called in explicit stop() call context
	 */
	private void invokeStopCallback() {
		try {
			onStop();
		} catch (Exception e) {
			log.warn("araqne log api: [" + fullName + "] stop callback should not throw any exception", e);
		}

		for (LoggerEventListener callback : eventListeners) {
			try {
				callback.onStop(this);
			} catch (Exception e) {
				log.warn("logger callback should not throw any exception", e);
			}
		}
	}

	protected abstract void runOnce();

	// can be overridden
	protected void onStart() {
	}

	// can be overridden
	protected void onStop() {
	}

	@Override
	public void run() {
		if (getExecutor() == null) {
			stopped = false;
			try {
				while (true) {
					try {
						if (doStop)
							break;
						long startedAt = System.currentTimeMillis();
						runOnce();
						updateConfig(config);
						long elapsed = System.currentTimeMillis() - startedAt;
						lastRunDate = new Date();
						if (interval - elapsed < 0)
							continue;
						Thread.sleep(interval - elapsed);
					} catch (InterruptedException e) {
					}
				}
			} catch (Exception e) {
				log.error("araqne log api: logger stopped", e);
			} finally {
				status = LoggerStatus.Stopped;
				stopped = true;
				doStop = false;

				try {
					onStop();
				} catch (Exception e) {
					log.warn("krane log api: [" + fullName + "] stop callback should not throw any exception", e);
				}
			}
		} else {
			if (!isRunning())
				return;

			if (lastRunDate != null) {
				long millis = lastRunDate.getTime() + (long) interval - System.currentTimeMillis();
				if (millis > 0) {
					try {
						Thread.sleep(Math.min(millis, 500));
					} catch (InterruptedException e) {
					}
					if (millis > 500) {
						ExecutorService executor = getExecutor();
						if (executor != null)
							executor.execute(this);
						return;
					}
				}
			}

			runOnce();
			updateConfig(config);
			lastRunDate = new Date();
			ExecutorService executor = getExecutor();
			if (executor != null)
				executor.execute(this);
		}
	}

	protected void write(Log log) {
		// call method to support overriding (ex. base remote logger)
		if (!isRunning())
			return;

		// update last log date
		lastLogDate = log.getDate();
		logCounter.incrementAndGet();

		// transform
		if (transformer != null)
			log = transformer.transform(log);

		// transform may return null to filter log
		if (log == null) {
			dropCounter.incrementAndGet();
			return;
		}

		// notify all
		LogPipe[] capturedPipes = pipes;
		for (LogPipe pipe : capturedPipes) {
			try {
				pipe.onLog(this, log);
			} catch (LoggerStopException e) {
				this.log.warn("araqne-log-api: stopping logger [" + getFullName() + "] by exception", e);
				if (isPassive())
					stop(false);
				else {
					doStop = true;
					status = LoggerStatus.Stopping;
					invokeStopCallback();
				}
			} catch (Exception e) {
				if (e.getMessage() != null && e.getMessage().startsWith("invalid time"))
					this.log.warn("araqne-log-api: log pipe should not throw exception" + e.getMessage());
				else
					this.log.warn("araqne-log-api: log pipe should not throw exception", e);
			}
		}
	}

	@Override
	public void updateConfig(Map<String, String> config) {
		for (LoggerEventListener callback : eventListeners) {
			try {
				callback.onUpdated(this, config);
			} catch (Exception e) {
				log.error("araqne log api: logger event callback should not throw any exception", e);
			}
		}
	}

	@Override
	public Map<String, String> getConfig() {
		return config;
	}

	@Override
	public LogTransformer getTransformer() {
		return transformer;
	}

	@Override
	public void setTransformer(LogTransformer transformer) {
		this.transformer = transformer;

		if (isPending() && transformer != null)
			start(getInterval());
		if (isRunning() && config.get("transformer") != null && transformer == null) {
			stop(5000, true);
		}
	}

	@Override
	public void addLogPipe(LogPipe pipe) {
		if (pipe == null)
			throw new IllegalArgumentException("pipe should be not null");

		// read-copy-update
		synchronized (updateLock) {
			// check if already exists
			boolean found = false;
			for (int i = 0; i < pipes.length; i++)
				if (pipes[i] == pipe)
					found = true;

			if (found)
				return;

			// copy old items
			LogPipe[] newPipes = new LogPipe[pipes.length + 1];
			for (int i = 0; i < pipes.length; i++)
				newPipes[i] = pipes[i];

			// add new item
			newPipes[pipes.length] = pipe;

			pipes = newPipes;
		}
	}

	@Override
	public void removeLogPipe(LogPipe pipe) {
		if (pipe == null)
			throw new IllegalArgumentException("pipe should be not null");

		// read-copy-update
		synchronized (updateLock) {
			// check if exists
			boolean found = false;
			for (int i = 0; i < pipes.length; i++)
				if (pipes[i] == pipe)
					found = true;

			if (!found)
				return;

			LogPipe[] newPipes = new LogPipe[pipes.length - 1];
			int j = 0;
			for (int i = 0; i < pipes.length; i++) {
				if (pipes[i] == pipe)
					continue;

				newPipes[j++] = pipes[i];
			}

			pipes = newPipes;
		}
	}

	@Override
	public void addEventListener(LoggerEventListener callback) {
		if (callback == null)
			throw new IllegalArgumentException("logger event listener must be not null");

		eventListeners.add(callback);
	}

	@Override
	public void removeEventListener(LoggerEventListener callback) {
		if (callback == null)
			throw new IllegalArgumentException("logger event listener must be not null");

		eventListeners.remove(callback);
	}

	@Override
	public void clearEventListeners() {
		eventListeners.clear();
	}

	@Override
	public String toString() {
		String format = "yyyy-MM-dd HH:mm:ss";
		String start = DateFormat.format(format, lastStartDate);
		String run = DateFormat.format(format, lastRunDate);
		String log = DateFormat.format(format, lastLogDate);
		String status = getStatus().toString().toLowerCase();
		if (isPassive())
			status += " (passive)";
		else
			status += " (interval=" + interval + "ms)";

		return String.format("name=%s, factory=%s, status=%s, log count=%d, last start=%s, last run=%s, last log=%s",
				getFullName(), factoryFullName, status, getLogCount(), start, run, log);
	}
}

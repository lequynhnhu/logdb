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
import java.util.concurrent.CopyOnWriteArraySet;
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
	private CopyOnWriteArraySet<LogPipe> pipes;
	private Thread t;
	private int interval;
	private Map<String, String> config;

	private volatile LoggerStatus status = LoggerStatus.Stopped;
	private volatile boolean doStop = false;
	private volatile boolean stopped = true;
	private volatile boolean pending = false;
	private volatile boolean manualStart = false;
	private volatile boolean stopCallbacked = false;

	private volatile Date lastStartDate;
	private volatile Date lastRunDate;
	private volatile Date lastLogDate;
	private volatile Date lastWriteDate;
	private volatile Log lastLog;
	private AtomicLong logCounter;
	private AtomicLong dropCounter;

	private Set<LoggerEventListener> eventListeners;
	private LogTransformer transformer;
	private LoggerFactory factory;

	/**
	 * @since 1.7.0
	 */
	public AbstractLogger(LoggerSpecification spec, LoggerFactory factory) {
		// logger info
		this.namespace = spec.getNamespace();
		this.name = spec.getName();
		this.fullName = namespace + "\\" + name;
		this.description = spec.getDescription();
		this.config = spec.getConfig();

		// load state
		LastStateService lss = factory.getLastStateService();
		LastState state = lss.getState(fullName);
		long lastLogCount = 0;
		long lastDropCount = 0;
		int interval = 0;

		if (state != null) {
			lastLogCount = state.getLogCount();
			lastDropCount = state.getDropCount();
			lastLogDate = state.getLastLogDate();
		}

		// logger factory info
		this.factoryNamespace = factory.getNamespace();
		this.factoryName = factory.getName();
		this.factoryFullName = factoryNamespace + "\\" + factoryName;

		this.logCounter = new AtomicLong(lastLogCount);
		this.dropCounter = new AtomicLong(lastDropCount);
		this.lastLogDate = lastLogDate;
		this.pipes = new CopyOnWriteArraySet<LogPipe>();

		this.eventListeners = Collections.newSetFromMap(new ConcurrentHashMap<LoggerEventListener, Boolean>());

		this.interval = interval;
		this.factory = factory;
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
		return false;
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
	public Date getLastWriteDate() {
		return lastWriteDate;
	}

	@Override
	public Log getLastLog() {
		return lastLog;
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

	@Override
	public LoggerFactory getFactory() {
		return factory;
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
		stopCallbacked = false;

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
		if (stopCallbacked)
			return;

		stopCallbacked = true;

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
					invokeStopCallback();
				} catch (Exception e) {
					log.warn("araqne log api: [" + fullName + "] stop callback should not throw any exception", e);
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

	protected void writeBatch(Log[] logs) {
		// call method to support overriding (ex. base remote logger)
		if (!isRunning())
			return;

		int addCount = 0;
		int dropCount = 0;

		for (int i = 0; i < logs.length; i++) {
			Log log = logs[i];
			if (log == null)
				continue;

			// update last log date
			lastLogDate = log.getDate();
			lastLog = log;
			addCount++;

			// transform
			if (transformer != null)
				log = transformer.transform(log);

			logs[i] = log;

			// transform may return null to filter log
			if (log == null) {
				dropCount++;
				continue;
			}
		}

		if (addCount > dropCount)
			lastWriteDate = new Date();

		logCounter.addAndGet(addCount);
		dropCounter.addAndGet(dropCount);

		// notify all
		for (LogPipe pipe : pipes) {
			try {
				pipe.onLogBatch(this, logs);
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

	protected void write(Log log) {
		// call method to support overriding (ex. base remote logger)
		if (!isRunning())
			return;

		// update last log date
		lastLogDate = log.getDate();
		lastLog = log;
		logCounter.incrementAndGet();

		// transform
		if (transformer != null)
			log = transformer.transform(log);

		// transform may return null to filter log
		if (log == null) {
			dropCounter.incrementAndGet();
			return;
		}

		lastWriteDate = new Date();

		// notify all
		for (LogPipe pipe : pipes) {
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
	public Map<String, Object> getState() {
		LastStateService lastStateService = factory.getLastStateService();
		if (lastStateService == null)
			return null;

		LastState s = lastStateService.getState(getFullName());
		if (s == null)
			return null;

		return s.getProperties();
	}

	@Override
	public void setState(Map<String, Object> state) {
		LastStateService lastStateService = factory.getLastStateService();
		if (lastStateService == null)
			throw new IllegalStateException("last status service not found");

		LastState s = new LastState();
		s.setLoggerName(getFullName());
		s.setInterval(interval);
		s.setLogCount(logCounter.get());
		s.setDropCount(dropCounter.get());
		s.setLastLogDate(lastLogDate);
		s.setPending(pending);
		s.setRunning(status == LoggerStatus.Running);
		s.setProperties(state);

		lastStateService.setState(s);
		log.trace("araqne log api: running state saved: {}", getFullName());
	}

	@Override
	public void resetState() {
		logCounter.set(0);
		dropCounter.set(0);
		lastLogDate = null;
		setState(new HashMap<String, Object>());
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

		pipes.add(pipe);
	}

	@Override
	public void removeLogPipe(LogPipe pipe) {
		if (pipe == null)
			throw new IllegalArgumentException("pipe should be not null");

		pipes.remove(pipe);
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

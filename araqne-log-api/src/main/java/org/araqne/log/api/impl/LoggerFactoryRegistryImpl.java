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
package org.araqne.log.api.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.log.api.LastStateService;
import org.araqne.log.api.LogTransformerRegistry;
import org.araqne.log.api.Logger;
import org.araqne.log.api.LoggerFactory;
import org.araqne.log.api.LoggerFactoryRegistry;
import org.araqne.log.api.LoggerFactoryRegistryEventListener;
import org.araqne.log.api.LoggerRegistry;
import org.araqne.log.api.LoggerSpecification;
import org.osgi.framework.BundleContext;

@Component(name = "logger-factory-registry")
@Provides(specifications = { LoggerFactoryRegistry.class })
public class LoggerFactoryRegistryImpl implements LoggerFactoryRegistry, LoggerFactoryRegistryEventListener {
	private final org.slf4j.Logger slog = org.slf4j.LoggerFactory.getLogger(LoggerFactoryRegistryImpl.class.getName());

	/**
	 * force dependency, last state service should be alive until any logger or
	 * factory is available.
	 */
	@Requires
	private LastStateService lastStateService;

	private ConcurrentMap<String, LoggerFactory> loggerFactories;
	private BundleContext bc;
	private LoggerFactoryTracker tracker;
	private Set<LoggerFactoryRegistryEventListener> callbacks;

	// force loading
	@Requires
	private LoggerRegistry loggerRegistry;

	// force loading
	@Requires
	private LogTransformerRegistry transformerRegistry;

	public LoggerFactoryRegistryImpl(BundleContext bc) {
		loggerFactories = new ConcurrentHashMap<String, LoggerFactory>();
		this.bc = bc;
		this.callbacks = Collections.newSetFromMap(new ConcurrentHashMap<LoggerFactoryRegistryEventListener, Boolean>());
	}

	@Validate
	public void start() {
		reset();

		tracker = new LoggerFactoryTracker(bc, this);
		tracker.open();
	}

	@Invalidate
	public void stop() {
		if (tracker != null)
			tracker.close();

		reset();
	}

	private void reset() {
		callbacks.clear();
		loggerFactories.clear();
	}

	@Override
	public void factoryAdded(LoggerFactory loggerFactory) {
		if (loggerFactory == null) {
			slog.error("araqne log api: logger factory must be not null");
			return;
		}

		LoggerFactory old = loggerFactories.putIfAbsent(loggerFactory.getFullName(), loggerFactory);
		if (old != null) {
			slog.error("duplicated logger factory name: " + loggerFactory.getFullName());
			return;
		}

		slog.debug("araqne log api: logger factory [{}] added", loggerFactory.getFullName());

		// trigger callbacks
		for (LoggerFactoryRegistryEventListener callback : callbacks) {
			try {
				callback.factoryAdded(loggerFactory);
			} catch (Exception e) {
				slog.warn("araqne log api: factory event callback should not throw any exception", e);
			}
		}

	}

	@Override
	public void factoryRemoved(LoggerFactory loggerFactory) {
		if (loggerFactory == null)
			throw new IllegalArgumentException("logger factory must be not null");

		// remove
		loggerFactories.remove(loggerFactory.getFullName());

		slog.debug("araqne log api: logger factory [{}] removed", loggerFactory.getFullName());

		// trigger callbacks
		for (LoggerFactoryRegistryEventListener callback : callbacks) {
			try {
				callback.factoryRemoved(loggerFactory);
			} catch (Exception e) {
				slog.warn("araqne log api: factory event callback should not throw any exception", e);
			}
		}
	}

	@Override
	public Logger newLogger(String factoryName, LoggerSpecification spec) {
		if (factoryName == null)
			throw new IllegalArgumentException("name must be not null");

		LoggerFactory factory = loggerFactories.get(factoryName);
		if (factory == null)
			throw new IllegalStateException("factory not found: " + factoryName);

		// creates logger and invokes callbacks
		return factory.newLogger(spec);
	}

	@Override
	public Collection<LoggerFactory> getLoggerFactories() {
		return new ArrayList<LoggerFactory>(loggerFactories.values());
	}

	@Override
	public LoggerFactory getLoggerFactory(String name) {
		return getLoggerFactory("local", name);
	}

	@Override
	public LoggerFactory getLoggerFactory(String namespace, String name) {
		return loggerFactories.get(namespace + "\\" + name);
	}

	@Override
	public void addListener(LoggerFactoryRegistryEventListener callback) {
		if (callback == null)
			throw new IllegalArgumentException("callback must not be null");

		callbacks.add(callback);
	}

	@Override
	public void removeListener(LoggerFactoryRegistryEventListener callback) {
		if (callback == null)
			throw new IllegalArgumentException("callback must not be null");

		callbacks.remove(callback);
	}

}

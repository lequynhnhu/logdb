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
package org.araqne.logdb.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.logdb.LogQueryContext;
import org.araqne.logdb.LogQueryParseException;
import org.araqne.logdb.MetadataCallback;
import org.araqne.logdb.MetadataProvider;
import org.araqne.logdb.MetadataService;

@Component(name = "logdb-metadata-service")
@Provides
public class MetadataServiceImpl implements MetadataService {

	private ConcurrentMap<String, MetadataProvider> providers;

	@Validate
	public void start() {
		providers = new ConcurrentHashMap<String, MetadataProvider>();
	}

	@Override
	public void verify(LogQueryContext context, String type, String queryString) {
		MetadataProvider provider = providers.get(type);
		if (provider == null)
			throw new LogQueryParseException("invalid-system-object-type", -1, "type=" + type);
		provider.verify(context, queryString);
	}

	@Override
	public void query(LogQueryContext context, String type, String queryString, MetadataCallback callback) {
		MetadataProvider provider = providers.get(type);
		if (provider == null)
			throw new IllegalStateException("metadata provider not found: " + type);
		provider.query(context, queryString, callback);
	}

	@Override
	public void addProvider(MetadataProvider provider) {
		if (provider == null)
			throw new IllegalArgumentException("metadata provider should not be null");
		providers.putIfAbsent(provider.getType(), provider);
	}

	@Override
	public void removeProvider(MetadataProvider provider) {
		if (provider == null)
			throw new IllegalArgumentException("metadata provider should not be null");
		providers.remove(provider.getType(), provider);
	}

}
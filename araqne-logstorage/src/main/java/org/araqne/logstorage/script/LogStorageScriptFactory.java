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
package org.araqne.logstorage.script;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.araqne.api.Script;
import org.araqne.api.ScriptFactory;
import org.araqne.confdb.ConfigService;
import org.araqne.logstorage.LogStorage;
import org.araqne.logstorage.LogStorageMonitor;
import org.araqne.logstorage.LogTableRegistry;

@Component(name = "logstorage-script-factory")
@Provides
public class LogStorageScriptFactory implements ScriptFactory {
	@SuppressWarnings("unused")
	@ServiceProperty(name = "alias", value = "logstorage")
	private String alias;

	@Requires
	private LogTableRegistry tableRegistry;

	@Requires
	private LogStorage storage;

	@Requires
	private ConfigService conf;

	@Requires
	private LogStorageMonitor monitor;

	@Override
	public Script createScript() {
		return new LogStorageScript(tableRegistry, storage, monitor, conf);
	}
}

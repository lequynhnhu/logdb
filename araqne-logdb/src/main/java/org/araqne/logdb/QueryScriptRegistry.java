/*
 * Copyright 2012 Future Systems
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

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface QueryScriptRegistry {
	Set<String> getWorkspaceNames();
	
	void createWorkspace(String name);

	void dropWorkspace(String name);

	Set<String> getScriptFactoryNames(String workspace);

	List<QueryScriptFactory> getScriptFactories(String workspace);
	
	QueryScriptFactory getScriptFactory(String workspace, String name);

	QueryScript newScript(String workspace, String name, Map<String, String> params);

	void addScriptFactory(String workspace, String name, QueryScriptFactory factory);

	void removeScriptFactory(String workspace, String name);
}

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
package org.araqne.log.api;

import java.util.Map;

/**
 * @since 2.6.0
 * @author xeraph
 * 
 */
public abstract class V2LogParser implements LogParser {

	@Override
	public int getVersion() {
		return 2;
	}

	@Override
	public Map<String, Object> parse(Map<String, Object> params) {
		throw new UnsupportedOperationException();
	}
}

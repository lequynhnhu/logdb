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
package org.araqne.logdb.client.http;

import java.io.IOException;

import org.araqne.logdb.client.LogDbSession;
import org.araqne.logdb.client.LogDbTransport;
import org.araqne.logdb.client.http.impl.WebSocketSession;

/**
 * 메시지버스 RPC 웹 소켓 전송 계층을 구현합니다.
 * 
 * @since 0.5.0
 * @author xeraph@eediom.com
 * 
 */
public class WebSocketTransport implements LogDbTransport {

	@Override
	public LogDbSession newSession(String host, int port) throws IOException {
		return new WebSocketSession(host, port);
	}
}

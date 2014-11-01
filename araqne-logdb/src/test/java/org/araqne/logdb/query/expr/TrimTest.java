/**
 * Copyright 2014 Eediom Inc.
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
package org.araqne.logdb.query.expr;

import static org.junit.Assert.*;

import org.junit.Test;

public class TrimTest {
	@Test
	public void testManual() {
		assertEquals("hello world", FunctionUtil.parseExpr("trim(\" hello world \")").eval(null));
		assertEquals("123", FunctionUtil.parseExpr("trim(123)").eval(null));
		assertNull(FunctionUtil.parseExpr("trim(int(\"invalid\"))").eval(null));
	}
}
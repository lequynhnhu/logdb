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
package org.araqne.logdb.query.expr;

import java.util.Date;
import java.util.List;

import org.araqne.logdb.QueryContext;
import org.araqne.logdb.Row;

/**
 * @since 1.7.2
 * @author xeraph
 * 
 */
public class Now implements Expression {

	public Now(QueryContext ctx, List<Expression> exprs) {
	}

	@Override
	public Object eval(Row map) {
		return new Date();
	}

	@Override
	public String toString() {
		return "now()";
	}
}

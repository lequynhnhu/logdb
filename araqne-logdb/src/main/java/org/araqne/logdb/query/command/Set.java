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
package org.araqne.logdb.query.command;

import org.araqne.logdb.QueryCommand;
import org.araqne.logdb.Row;
import org.araqne.logdb.RowBatch;
import org.araqne.logdb.query.expr.Expression;

/**
 * @since 1.7.3
 * @author xeraph
 * 
 */
public class Set extends QueryCommand {
	private String field;
	private Expression expr;

	public Set(String field, Expression expr) {
		this.field = field;
		this.expr = expr;
	}

	@Override
	public String getName() {
		return "set";
	}

	public String getField() {
		return field;
	}

	public Expression getExpression() {
		return expr;
	}

	@Override
	public void onPush(Row row) {
		pushPipe(row);
	}

	@Override
	public void onPush(RowBatch rowBatch) {
		pushPipe(rowBatch);
	}

	@Override
	public String toString() {
		return "set " + field + "=" + expr;
	}
}

/*
 * Copyright 2013 Future Systems
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

import java.util.List;

import org.araqne.logdb.QueryContext;
import org.araqne.logdb.Row;
import org.araqne.logdb.Strings;

public class Concat implements Expression {
	private List<Expression> exprs;

	public Concat(QueryContext ctx, List<Expression> exprs) {
		this.exprs = exprs;
	}

	@Override
	public Object eval(Row map) {
		StringBuilder sb = new StringBuilder();
		for (Expression expr : exprs)
			sb.append(expr.eval(map));

		return sb.toString();
	}

	@Override
	public String toString() {
		return "concat(" + Strings.join(exprs, ", ") + ")";
	}
}

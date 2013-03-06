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

import org.araqne.logdb.LogMap;

public class Left implements Expression {
	private Expression valueExpr;
	private int length;

	public Left(List<Expression> exprs) {
		this.valueExpr = exprs.get(0);
		this.length = Integer.parseInt(exprs.get(1).eval(null).toString());
	}

	@Override
	public Object eval(LogMap map) {
		Object value = valueExpr.eval(map);
		if (value == null)
			return null;

		String s = value.toString();
		if (s.length() < length)
			return s;

		return s.substring(0, length);
	}

	@Override
	public String toString() {
		return "left(" + valueExpr + ", " + length + ")";
	}

}

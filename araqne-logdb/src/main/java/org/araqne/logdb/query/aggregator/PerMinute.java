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
package org.araqne.logdb.query.aggregator;

import java.util.List;

import org.araqne.logdb.query.expr.Expression;

public class PerMinute extends PerTime {
	public PerMinute(List<Expression> exprs) {
		super(exprs);
	}

	@Override
	protected long getTimeLength() {
		return 60 * 1000L;
	}

	@Override
	public String toString() {
		return "per_minute(" + exprs.get(0) + ")";
	}

}

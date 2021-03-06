/**
 * Copyright 2015 Eediom Inc.
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
import org.araqne.logdb.QueryStopReason;
import org.araqne.logdb.Row;
import org.araqne.logdb.RowBatch;
import org.araqne.logdb.ThreadSafe;

public class Repeat extends QueryCommand implements ThreadSafe {

	private int count;

	private volatile boolean cancelled;

	public Repeat(int count) {
		this.count = count;
	}

	@Override
	public String getName() {
		return "repeat";
	}

	@Override
	public void onPush(Row row) {
		for (int i = 0; i < count; i++) {
			if (cancelled)
				break;

			pushPipe(row.clone());
		}
	}

	@Override
	public void onPush(RowBatch rowBatch) {
		for (int i = 0; i < count; i++) {
			if (cancelled)
				break;

			rowBatch.rebuild();
			pushPipe(rowBatch);
		}
	}

	@Override
	public void onClose(QueryStopReason reason) {
		cancelled = true;
	}

	@Override
	public String toString() {
		return "repeat count=" + count;
	}
}

/*
 * Copyright 2011 Future Systems
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.araqne.logdb.LogMap;
import org.araqne.logdb.LogQueryCommand;
import org.araqne.logdb.ObjectComparator;
import org.araqne.logdb.impl.TopSelector;
import org.araqne.logdb.query.parser.ParseResult;
import org.araqne.logdb.query.parser.QueryTokenizer;
import org.araqne.logdb.sort.CloseableIterator;
import org.araqne.logdb.sort.Item;
import org.araqne.logdb.sort.ParallelMergeSorter;

public class Sort extends LogQueryCommand {
	private static final int TOP_OPTIMIZE_THRESHOLD = 10000;
	private Integer limit;
	private SortField[] fields;
	private ParallelMergeSorter sorter;
	private boolean reverse;
	private TopSelector<Item> top;

	public Sort(SortField[] fields) {
		this(null, fields, false);
	}

	public Sort(Integer limit, SortField[] fields) {
		this(limit, fields, false);
	}

	public Sort(SortField[] fields, boolean reverse) {
		this(null, fields, reverse);
	}

	public Sort(Integer limit, SortField[] fields, boolean reverse) {
		this.limit = limit;
		this.fields = fields;
		this.reverse = reverse;
	}

	@Override
	public void init() {
		super.init();
		if (limit != null && limit <= TOP_OPTIMIZE_THRESHOLD)
			this.top = new TopSelector<Item>(limit, new DefaultComparator());
		else
			this.sorter = new ParallelMergeSorter(new DefaultComparator());
	}

	public Integer getLimit() {
		return limit;
	}

	public SortField[] getFields() {
		return fields;
	}

	public boolean isReverse() {
		return reverse;
	}

	@Override
	public void push(LogMap m) {
		try {
			if (top != null)
				top.add(new Item(m.map(), null));
			else
				sorter.add(new Item(m.map(), null));
		} catch (IOException e) {
			throw new IllegalStateException("sort failed, query " + logQuery, e);
		}
	}

	@Override
	public boolean isReducer() {
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void eof(boolean canceled) {
		this.status = Status.Finalizing;

		if (top != null) {
			Iterator<Item> it = top.getTopEntries();
			while (it.hasNext()) {
				Item item = it.next();
				write(new LogMap((Map<String, Object>) item.getKey()));
			}

			// support sorter cache GC when query processing is ended
			top = null;

		} else {
			// TODO: use LONG instead!
			int count = limit != null ? limit : Integer.MAX_VALUE;

			CloseableIterator it = null;
			try {
				it = sorter.sort();

				while (it.hasNext()) {
					Object o = it.next();
					if (--count < 0)
						break;

					Map<String, Object> value = (Map<String, Object>) ((Item) o).getKey();
					write(new LogMap(value));
				}

			} catch (IOException e) {
			} finally {
				// close and delete sorted run file
				if (it != null) {
					try {
						it.close();
					} catch (IOException e) {
					}
				}
			}

			// support sorter cache GC when query processing is ended
			sorter = null;
		}

		super.eof(false);
	}

	private class DefaultComparator implements Comparator<Item> {
		private ObjectComparator cmp = new ObjectComparator();

		@SuppressWarnings("unchecked")
		@Override
		public int compare(Item o1, Item o2) {
			Map<String, Object> m1 = (Map<String, Object>) o1.getKey();
			Map<String, Object> m2 = (Map<String, Object>) o2.getKey();

			for (SortField field : fields) {
				Object v1 = m1.get(field.name);
				Object v2 = m2.get(field.name);

				if (v1 == null && v2 == null)
					continue;
				else if (v1 == null && v2 != null)
					return 1;

				if (!v1.equals(v2)) {
					int result = cmp.compare(v1, v2);

					if (!field.asc)
						result *= -1;
					if (reverse)
						result *= -1;

					return result;
				}
			}

			return 0;
		}
	}

	public static class SortField {
		private String name;
		private boolean asc;

		public static List<SortField> parseSortFields(String line, ParseResult r) {
			List<SortField> fields = new ArrayList<SortField>();
			int next = r.next;
			while (true) {
				r = QueryTokenizer.nextString(line, next, ',');
				String token = (String) r.value;
				boolean asc = true;
				char sign = token.charAt(0);
				if (sign == '-') {
					token = token.substring(1);
					asc = false;
				} else if (sign == '+') {
					token = token.substring(1);
				}

				SortField field = new SortField(token.trim(), asc);
				fields.add(field);
				next = r.next;

				if (line.length() == r.next)
					break;
			}

			return fields;
		}

		public SortField(String name) {
			this(name, true);
		}

		public SortField(String name, boolean asc) {
			this.name = name;
			this.asc = asc;
		}

		public String getName() {
			return name;
		}

		public boolean isAsc() {
			return asc;
		}

		public void reverseAsc() {
			asc = !asc;
		}

		@Override
		public String toString() {
			return "SortField [name=" + name + ", asc=" + asc + "]";
		}
	}
}

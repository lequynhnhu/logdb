package org.araqne.logdb.query.engine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.araqne.logdb.Query;
import org.araqne.logdb.QueryResult;
import org.araqne.logdb.QueryResultConfig;
import org.araqne.logdb.QueryResultFactory;
import org.araqne.logdb.QueryResultStorage;
import org.araqne.logstorage.file.LogFileReader;
import org.araqne.logstorage.file.LogFileReaderV2;
import org.araqne.logstorage.file.LogFileWriter;
import org.araqne.logstorage.file.LogFileWriterV2;

@Component(name = "logdb-query-result-factory")
@Provides
public class QueryResultFactoryImpl implements QueryResultFactory {
	private static File BASE_DIR = new File(System.getProperty("araqne.data.dir"), "araqne-logdb/query/");

	private QueryResultStorageV2 embedded = new QueryResultStorageV2();

	private CopyOnWriteArrayList<QueryResultStorage> storages = new CopyOnWriteArrayList<QueryResultStorage>();

	public QueryResultFactoryImpl() {
		storages.add(embedded);
	}

	@Override
	public QueryResult createResult(QueryResultConfig config) throws IOException {
		QueryResultStorage lastStorage = null;
		for (QueryResultStorage storage : storages)
			lastStorage = storage;

		return new QueryResultImpl(config, lastStorage);
	}

	@Override
	public void registerStorage(QueryResultStorage storage) {
		storages.add(storage);
	}

	@Override
	public void unregisterStorage(QueryResultStorage storage) {
		storages.remove(storage);
	}

	private static class QueryResultStorageV2 implements QueryResultStorage {

		@Override
		public LogFileWriter createWriter(QueryResultConfig config) throws IOException {
			BASE_DIR.mkdirs();

			String filePrefix = getFileNamePrefix(config);
			File indexPath = new File(BASE_DIR, filePrefix + ".idx");
			File dataPath = new File(BASE_DIR, filePrefix + ".dat");
			return new LogFileWriterV2(indexPath, dataPath, 1024 * 1024, 1);
		}

		@Override
		public LogFileReader createReader(QueryResultConfig config) throws IOException {
			String filePrefix = getFileNamePrefix(config);
			File indexPath = new File(BASE_DIR, filePrefix + ".idx");
			File dataPath = new File(BASE_DIR, filePrefix + ".dat");
			return new LogFileReaderV2(null, indexPath, dataPath);
		}

		private String getFileNamePrefix(QueryResultConfig config) {
			Query query = config.getQuery();
			String tag = config.getTag();

			SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
			if (tag == null || tag.isEmpty())
				tag = query.getId() + "_" + df.format(config.getCreated());

			return "result_v2_" + tag;
		}

	}
}

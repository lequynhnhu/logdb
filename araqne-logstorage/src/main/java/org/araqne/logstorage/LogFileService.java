package org.araqne.logstorage;

import java.util.List;
import java.util.Map;

import org.araqne.logstorage.file.LogFileReader;
import org.araqne.logstorage.file.LogFileWriter;
import org.araqne.storage.api.FilePath;

public interface LogFileService {
	String getType();

	/**
	 * @since 1.17.0
	 * @param f
	 *            .idx file path
	 * @return log count
	 */
	long count(FilePath f);

	LogFileWriter newWriter(Map<String, Object> options);

	LogFileReader newReader(String tableName, Map<String, Object> options);

	/**
	 * config specifications for table data file
	 * 
	 * @since add-storage-layer branch
	 * @return config specifications, at least empty list
	 */
	List<TableConfigSpec> getConfigSpecs();

	/**
	 * @return service specific global settings
	 */
	Map<String, String> getConfigs();

	void setConfig(String key, String value);

	void unsetConfig(String key);
}

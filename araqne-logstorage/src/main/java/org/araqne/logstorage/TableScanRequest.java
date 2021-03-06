package org.araqne.logstorage;

import java.util.Date;

import org.araqne.log.api.LogParserBuilder;

public class TableScanRequest {
	private String tableName;
	private Date from;
	private Date to;
	private long minId = -1;
	private long maxId = -1;
	private LogParserBuilder parserBuilder;
	private LogTraverseCallback callback;
	private boolean useSerialScan;
	private boolean isAsc;
	
	public TableScanRequest() {
	}
	
	public TableScanRequest(String tableName, long minId, long maxId, LogParserBuilder builder, LogTraverseCallback callback) {
		this.tableName = tableName;
		this.minId = minId;
		this.maxId = maxId;
		this.parserBuilder = builder;
		this.callback = callback;
	}
	
	public TableScanRequest(String tableName, Date from, Date to, LogParserBuilder builder, LogTraverseCallback callback) {
		this.tableName = tableName;
		this.from = from;
		this.to = to;
		this.parserBuilder = builder;
		this.callback = callback;
	}
	
	public TableScanRequest(String tableName, Date from, Date to, long minId, long maxId, LogParserBuilder builder, LogTraverseCallback callback) {
		this.tableName = tableName;
		this.from = from;
		this.to = to;
		this.minId = minId;
		this.maxId = maxId;
		this.parserBuilder = builder;
		this.callback = callback;
	}
	
	public TableScanRequest clone() {
		TableScanRequest cloned = new TableScanRequest();
		cloned.tableName = tableName;
		cloned.from = from;
		cloned.to = to;
		cloned.minId = minId;
		cloned.maxId = maxId;
		cloned.parserBuilder = parserBuilder;
		cloned.callback = callback;
		cloned.useSerialScan = useSerialScan;
		cloned.isAsc = isAsc;
		return cloned;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public Date getFrom() {
		return from;
	}

	public void setFrom(Date from) {
		this.from = from;
	}

	public Date getTo() {
		return to;
	}

	public void setTo(Date to) {
		this.to = to;
	}

	public long getMinId() {
		return minId;
	}

	public void setMinId(long minId) {
		this.minId = minId;
	}

	public long getMaxId() {
		return maxId;
	}

	public void setMaxId(long maxId) {
		this.maxId = maxId;
	}

	public LogParserBuilder getParserBuilder() {
		return parserBuilder;
	}

	public void setParserBuilder(LogParserBuilder parserBuilder) {
		this.parserBuilder = parserBuilder;
	}

	public LogTraverseCallback getTraverseCallback() {
		return callback;
	}

	public void setTraverseCallback(LogTraverseCallback callback) {
		this.callback = callback;
	}

	public boolean isUseSerialScan() {
		return useSerialScan;
	}

	public void setUseSerialScan(boolean useSerialScan) {
		this.useSerialScan = useSerialScan;
	}

	public boolean isAsc() {
		return isAsc;
	}

	public void setAsc(boolean isAsc) {
		this.isAsc = isAsc;
	}
}

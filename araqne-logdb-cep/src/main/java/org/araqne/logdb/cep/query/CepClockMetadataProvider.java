package org.araqne.logdb.cep.query;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.araqne.logdb.MetadataCallback;
import org.araqne.logdb.MetadataProvider;
import org.araqne.logdb.MetadataService;
import org.araqne.logdb.QueryContext;
import org.araqne.logdb.Row;
import org.araqne.logdb.cep.EventClock;
import org.araqne.logdb.cep.EventContextService;
import org.araqne.logdb.cep.EventContextStorage;

@Component(name = "cep-clock-meta-provider")
public class CepClockMetadataProvider implements MetadataProvider {
	@Requires
	private MetadataService metadataService;

	@Requires
	private EventContextService eventContextService;

	@Override
	public String getType() {
		return "cepclocks";
	}

	@Validate
	public void start() {
		metadataService.addProvider(this);
	}

	@Invalidate
	public void stop() {
		if (metadataService != null)
			metadataService.removeProvider(this);
	}

	@Override
	public void verify(QueryContext context, String queryString) {
	}

	@Override
	public void query(QueryContext context, String queryString, MetadataCallback callback) {
		EventContextStorage storage = eventContextService.getStorage("mem");

		for (String host : storage.getHosts()) {
			EventClock clock = storage.getClock(host);
			if (clock == null)
				continue;

			Row row = new Row();
			row.put("host", host);
			row.put("time", clock.getTime());
			row.put("expire_queue_len", clock.getExpireQueueLength());
			row.put("timeout_queue_len", clock.getTimeoutQueueLength());
			callback.onPush(row);
		}
	}

}

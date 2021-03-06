package org.araqne.storage.api;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class RCDirectBuffer {
	private AtomicInteger refCount = new AtomicInteger(0);
	private ByteBuffer buffer;
	private boolean destroyed = false;

	private boolean isDestroyed;

	private RCDirectBufferManager manager;

	public RCDirectBuffer(RCDirectBufferManager manager, ByteBuffer buffer) {
		this.manager = manager;
		this.buffer = buffer;
	}

	public ByteBuffer get() {
		return buffer;
	}

	private void destroy() {
		try {
			if (buffer == null)
				return;
			if (!buffer.isDirect())
				return;
			isDestroyed = true;
			manager.clean(buffer);
		} catch (Throwable t) {

		}
	}

	public RCDirectBuffer addRef() {
		synchronized (this) {
			if (!isDestroyed) {
				refCount.incrementAndGet();
				return this;
			} else {
				return null;
			}
		}
	}

	public RCDirectBuffer release() {
		synchronized (this) {
			int afterDec = refCount.decrementAndGet();
			if (afterDec == 0 && !destroyed) {
				destroy();
			}
		}
		return null;
	}

}

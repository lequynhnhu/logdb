package org.araqne.logdb.query.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

import org.araqne.logdb.RowPipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * physical plan
 * 
 * @author xeraph
 * 
 */
public abstract class QueryTask implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(QueryTask.class);

	public enum TaskStatus {
		INIT, RUNNING, FINALIZING, COMPLETED, CANCELED
	}

	private TaskStatus status = TaskStatus.INIT;
	private Throwable failure;
	private QueryTask parentTask;
	private CopyOnWriteArraySet<QueryTask> subTasks = new CopyOnWriteArraySet<QueryTask>();
	private CopyOnWriteArraySet<QueryTask> dependencies = new CopyOnWriteArraySet<QueryTask>();
	private CopyOnWriteArraySet<QueryTaskListener> listeners = new CopyOnWriteArraySet<QueryTaskListener>();

	public abstract RowPipe getOutput();

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	public Throwable getFailure() {
		return failure;
	}

	public void setFailure(Throwable failure) {
		this.failure = failure;
	}

	public Collection<QueryTask> getSubTasks() {
		return new ArrayList<QueryTask>(subTasks);
	}

	public QueryTask getParentTask() {
		return parentTask;
	}

	public void setParentTask(QueryTask parentTask) {
		this.parentTask = parentTask;
	}

	public void addSubTask(QueryTask task) {
		if (task == null)
			throw new IllegalArgumentException("null task is not allowed");
		task.setParentTask(this);
		subTasks.add(task);
	}

	public void removeSubTask(QueryTask task) {
		if (task == null)
			throw new IllegalArgumentException("null task is not allowed");
		subTasks.remove(task);
	}

	public boolean isRunnable() {
		for (QueryTask t : dependencies)
			if (t.getStatus() != TaskStatus.COMPLETED)
				return false;

		return status == TaskStatus.INIT;
	}

	// invoked when task is started
	protected void onStart() {
	}

	// like finally block, regardless of normal complete or cancel
	protected void onCleanUp() {
	}

	public Collection<QueryTask> getDependencies() {
		return dependencies;
	}

	public void addDependency(QueryTask task) {
		checkNotNull(task, "task");

		if (logger.isDebugEnabled())
			logger.debug("araqne logdb: [{}] depends on [{}] task", this, task);

		dependencies.add(task);
	}

	public void removeDependency(QueryTask task) {
		checkNotNull(task, "task");
		dependencies.remove(task);
	}

	public Collection<QueryTaskListener> getListeners() {
		return listeners;
	}

	public void addListener(QueryTaskListener listener) {
		checkNotNull(listener, "listener");
		listeners.add(listener);
	}

	public void removeListener(QueryTaskListener listener) {
		checkNotNull(listener, "listener");
		listeners.remove(listener);
	}

	private void checkNotNull(Object o, String name) {
		if (o == null)
			throw new IllegalArgumentException("null " + name + " is not allowed");
	}
}
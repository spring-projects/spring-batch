package org.springframework.batch.repeat.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

/**
 * ThreadPoolTaskExecutorRepeatTemplate without throttleLimit setting.
 *
 * @author linus.yan
 * @since 2025-04-25
 */
public class ThreadPoolTaskExecutorRepeatTemplate extends RepeatTemplate {

	private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTaskExecutorRepeatTemplate.class);

	private ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();

	// setter of taskExecutor
	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		Assert.notNull(taskExecutor, "taskExecutor must not be null");
		this.taskExecutor = taskExecutor;
	}

	private RepeatStatus status = RepeatStatus.CONTINUABLE;

	protected RepeatStatus getNextResult(RepeatContext context, RepeatCallback callback, RepeatInternalState state)
			throws Throwable {
		RepeatStatusInternalState internalState = (RepeatStatusInternalState) state;

		do {
			ExecutingRunnable runnable = new ExecutingRunnable(callback, context, internalState);
			this.taskExecutor.execute(runnable);
			this.update(context);
		}
		while (internalState.getStatus().isContinuable() && !this.isComplete(context));

		while (taskExecutor.getActiveCount() > 0) {
			// wait for all tasks to finish
		}

		return internalState.getStatus();
	}

	protected boolean waitForResults(RepeatInternalState state) {
		return ((RepeatStatusInternalState) state).getStatus().isContinuable();
	}

	protected RepeatInternalState createInternalState(RepeatContext context) {
		return new RepeatStatusInternalState();
	}

	private class ExecutingRunnable implements Runnable {

		private final RepeatCallback callback;

		private final RepeatContext context;

		private volatile RepeatStatusInternalState internalState;

		private volatile Throwable error;

		public ExecutingRunnable(RepeatCallback callback, RepeatContext context,
				RepeatStatusInternalState internalState) {
			this.callback = callback;
			this.context = context;
			this.internalState = internalState;
		}

		public void run() {
			boolean clearContext = false;
			RepeatStatus result = null;
			try {
				if (RepeatSynchronizationManager.getContext() == null) {
					clearContext = true;
					RepeatSynchronizationManager.register(this.context);
				}

				if (ThreadPoolTaskExecutorRepeatTemplate.this.logger.isDebugEnabled()) {
					ThreadPoolTaskExecutorRepeatTemplate.this.logger
						.debug("Repeat operation about to start at count=" + this.context.getStartedCount());
				}

				result = callback.doInIteration(context);
			}
			catch (Throwable e) {
				this.error = e;
			}
			finally {
				if (result == null) {
					result = RepeatStatus.FINISHED;
				}

				internalState.setStatus(status.and(result.isContinuable()));

				if (clearContext) {
					RepeatSynchronizationManager.clear();
				}
			}

		}

		public Throwable getError() {
			return this.error;
		}

		public RepeatContext getContext() {
			return this.context;
		}

	}

	private static class RepeatStatusInternalState extends RepeatInternalStateSupport {

		private RepeatStatus status = RepeatStatus.CONTINUABLE;

		public void setStatus(RepeatStatus status) {
			this.status = status;
		}

		public RepeatStatus getStatus() {
			return status;
		}

	}

}

package org.springframework.batch.core.step.builder;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.interceptor.TransactionAttribute;

public abstract class AbstractTaskletStepBuilder<B extends AbstractTaskletStepBuilder<B>> extends
		StepBuilderHelper<AbstractTaskletStepBuilder<B>> {

	private Set<ChunkListener> listeners = new LinkedHashSet<ChunkListener>();

	private RepeatOperations stepOperations;

	private TransactionAttribute transactionAttribute;

	private Set<ItemStream> streams = new LinkedHashSet<ItemStream>();

	private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();

	private int throttleLimit = TaskExecutorRepeatTemplate.DEFAULT_THROTTLE_LIMIT;

	private TaskExecutor taskExecutor;

	public AbstractTaskletStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	protected abstract Tasklet createTasklet();

	public TaskletStep build() {

		TaskletStep step = new TaskletStep(getName());

		super.enhance(step);

		step.setChunkListeners(listeners.toArray(new ChunkListener[0]));

		if (transactionAttribute != null) {
			step.setTransactionAttribute(transactionAttribute);
		}

		if (stepOperations == null) {

			stepOperations = new RepeatTemplate();

			if (taskExecutor != null) {
				TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
				repeatTemplate.setTaskExecutor(taskExecutor);
				repeatTemplate.setThrottleLimit(throttleLimit);
				stepOperations = repeatTemplate;
			}

			((RepeatTemplate) stepOperations).setExceptionHandler(exceptionHandler);

		}
		step.setStepOperations(stepOperations);
		step.setTasklet(createTasklet());

		step.setStreams(getStreams());
		
		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}

		return step;

	}

	public AbstractTaskletStepBuilder<B> listener(ChunkListener listener) {
		listeners.add(listener);
		return this;
	}

	public AbstractTaskletStepBuilder<B> stream(ItemStream stream) {
		streams.add(stream);
		return this;
	}

	public AbstractTaskletStepBuilder<B> taskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	public AbstractTaskletStepBuilder<B> throttleLimit(int throttleLimit) {
		this.throttleLimit = throttleLimit;
		return this;
	}
	
	public AbstractTaskletStepBuilder<B> exceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	public AbstractTaskletStepBuilder<B> stepOperations(RepeatOperations repeatTemplate) {
		this.stepOperations = repeatTemplate;
		return this;
	}

	public AbstractTaskletStepBuilder<B> transactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
		return this;
	}

	protected ItemStream[] getStreams() {
		return streams.toArray(new ItemStream[0]);
	}
	
	protected RepeatOperations getStepOperations() {
		return stepOperations;
	}
	
	protected ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}
	
	protected boolean concurrent() {
		boolean concurrent = taskExecutor != null && !(taskExecutor instanceof SyncTaskExecutor);
		return concurrent;
	}

}
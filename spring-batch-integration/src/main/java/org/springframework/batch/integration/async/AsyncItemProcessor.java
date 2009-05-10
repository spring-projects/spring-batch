package org.springframework.batch.integration.async;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

/**
 * An {@link ItemProcessor} that delegates to a nested processor and in the
 * background. To allow for background processing the return value from the
 * processor is a {@link Future} which needs to be unpacked before the item can
 * be used by a client.
 * 
 * @author Dave Syer
 * 
 * @param <I> the input object type
 * @param <O> the output object type (will be wrapped in a Future)
 */
public class AsyncItemProcessor<I, O> implements ItemProcessor<I, Future<O>>, InitializingBean {

	private ItemProcessor<I, O> delegate;

	private TaskExecutor taskExecutor = new SyncTaskExecutor();

	/**
	 * Check mandatory properties (the {@link #setDelegate(ItemProcessor)}).
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(delegate, "The delegate must be set.");
	}

	/**
	 * The {@link ItemProcessor} to use to delegate processing to in a
	 * background thread.
	 * 
	 * @param delegate the {@link ItemProcessor} to use as a delegate
	 */
	public void setDelegate(ItemProcessor<I, O> delegate) {
		this.delegate = delegate;
	}

	/**
	 * The {@link TaskExecutor} to use to allow the item processing to proceed
	 * in the background. Defaults to a {@link SyncTaskExecutor} so no threads
	 * are created unless this is overridden.
	 * 
	 * @param taskExecutor a {@link TaskExecutor}
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Transform the input by delegating to the provided item processor. The
	 * return value is wrapped in a {@link Future} so that clients can unpack it
	 * later.
	 * 
	 * @see ItemProcessor#process(Object)
	 */
	public Future<O> process(final I item) throws Exception {
		FutureTask<O> task = new FutureTask<O>(new Callable<O>() {
			public O call() throws Exception {
				return delegate.process(item);
			}
		});
		taskExecutor.execute(task);
		return task;
	}

}

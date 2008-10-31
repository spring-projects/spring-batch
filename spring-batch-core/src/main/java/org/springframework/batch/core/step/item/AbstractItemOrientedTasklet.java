package org.springframework.batch.core.step.item;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;

/**
 * Superclass for {@link Tasklet}s implementing variations on read-process-write
 * item handling. Encapsulates listener registration and bundles listener
 * callbacks with relevant method calls.
 * 
 * @author Robert Kasanicky
 * 
 * @param <I> input item type
 * @param <O> output item type
 */
public abstract class AbstractItemOrientedTasklet<I, O> implements Tasklet {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final ItemReader<? extends I> itemReader;

	protected final ItemProcessor<? super I, ? extends O> itemProcessor;

	protected final ItemWriter<? super O> itemWriter;

	protected final MulticasterBatchListener<I, O> listener = new MulticasterBatchListener<I, O>();

	public AbstractItemOrientedTasklet(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter) {
		this.itemReader = itemReader;
		this.itemProcessor = itemProcessor;
		this.itemWriter = itemWriter;
	}

	/**
	 * Register some {@link StepListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage.
	 * 
	 * @param listeners
	 */
	public void setListeners(StepListener[] listeners) {
		for (StepListener listener : listeners) {
			registerListener(listener);
		}
	}

	/**
	 * Register a listener for callbacks at the appropriate stages in a process.
	 * 
	 * @param listener a {@link StepListener}
	 */
	public void registerListener(StepListener listener) {
		this.listener.register(listener);
	}

	/**
	 * Surrounds the read call with listener callbacks.
	 * @return item
	 * @throws Exception
	 */
	protected final I doRead() throws Exception {
		try {
			listener.beforeRead();
			I item = itemReader.read();
			listener.afterRead(item);
			return item;
		}
		catch (Exception e) {
			listener.onReadError(e);
			throw e;
		}
	}

	/**
	 * @param item the input item
	 * @return the result of the processing
	 * @throws Exception
	 */
	protected final O doProcess(I item) throws Exception {
		try {
			listener.beforeProcess(item);
			O result = itemProcessor.process(item);
			listener.afterProcess(item, result);
			return result;
		}
		catch (Exception e) {
			listener.onProcessError(item, e);
			throw e;
		}
	}

	/**
	 * Surrounds the actual write call with listener callbacks.
	 * @param items
	 * @throws Exception
	 */
	protected final void doWrite(List<O> items) throws Exception {
		try {
			listener.beforeWrite(items);
			itemWriter.write(items);
			listener.afterWrite(items);
		}
		catch (Exception e) {
			listener.onWriteError(e, items);
			throw e;
		}
	}

}

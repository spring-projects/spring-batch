package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.AttributeAccessor;

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

	/**
	 * Call all skip listeners in read-process-write order
	 * @param skippedReads read exceptions
	 * @param skippedInputs items and corresponding exceptions skipped in
	 * processing phase
	 * @param skippedOutputs items and corresponding exceptions skipped in write
	 * phase
	 */
	protected void callSkipListeners(final List<Exception> skippedReads, final Map<I, Exception> skippedInputs,
			final Map<O, Exception> skippedOutputs) {

		for (Exception e : skippedReads) {
			try {
				listener.onSkipInRead(e);
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
			}
		}
		for (Entry<I, Exception> skip : skippedInputs.entrySet()) {
			try {
				listener.onSkipInProcess(skip.getKey(), skip.getValue());
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, skip.getValue());
			}
		}

		for (Entry<O, Exception> skip : skippedOutputs.entrySet()) {
			try {
				listener.onSkipInWrite(skip.getKey(), skip.getValue());
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in skip listener", ex, skip.getValue());
			}
		}
	}

	/**
	 * Return a list stored in the attributes under the key. Create an empty
	 * list and store it if the list is not stored yet.
	 */
	protected static <T> List<T> getBufferedList(AttributeAccessor attributes, String key) {
		List<T> buffer;
		if (!attributes.hasAttribute(key)) {
			buffer = new ArrayList<T>();
			attributes.setAttribute(key, buffer);
		}
		else {
			@SuppressWarnings("unchecked")
			List<T> casted = (List<T>) attributes.getAttribute(key);
			buffer = casted;
		}
		return buffer;
	}

	/**
	 * Return a map of items to exceptions stored in the attributes under the
	 * key, Create an empty map and store it if the list is not stored yet.
	 */
	protected static <T> Map<T, Exception> getBufferedSkips(AttributeAccessor attributes, String key) {
		Map<T, Exception> buffer;
		if (!attributes.hasAttribute(key)) {
			buffer = new LinkedHashMap<T, Exception>();
			attributes.setAttribute(key, buffer);
		}
		else {
			@SuppressWarnings("unchecked")
			Map<T, Exception> casted = (Map<T, Exception>) attributes.getAttribute(key);
			buffer = casted;
		}
		return buffer;
	}
}

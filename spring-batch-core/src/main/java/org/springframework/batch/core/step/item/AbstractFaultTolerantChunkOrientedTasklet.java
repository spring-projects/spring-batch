package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.AttributeAccessor;

public abstract class AbstractFaultTolerantChunkOrientedTasklet<I, O> extends AbstractItemOrientedTasklet<I, O> {

	public AbstractFaultTolerantChunkOrientedTasklet(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter) {
		super(itemReader, itemProcessor, itemWriter);
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

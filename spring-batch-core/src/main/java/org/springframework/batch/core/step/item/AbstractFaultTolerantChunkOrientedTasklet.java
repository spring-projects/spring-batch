package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.support.DefaultRetryState;
import org.springframework.batch.support.Classifier;
import org.springframework.core.AttributeAccessor;

/**
 * Fault-tolerant implementation of the process and write phase of chunk
 * processing.
 * 
 * @param <I> input item type
 * @param <O> output item type
 * 
 * @see FaultTolerantChunkOrientedTasklet
 * @see NonbufferingFaultTolerantChunkOrientedTasklet
 * 
 * @author Robert Kasanicky
 */
public abstract class AbstractFaultTolerantChunkOrientedTasklet<I, O> extends AbstractItemOrientedTasklet<I, O> {

	final static protected String SKIPPED_INPUTS_KEY = "SKIPPED_INPUTS_KEY";

	final static protected String SKIPPED_OUTPUTS_KEY = "SKIPPED_OUTPUTS_KEY";

	final static protected String SKIPPED_READS_KEY = "SKIPPED_READS_KEY";

	final private RetryOperations retryOperations;

	final private RepeatOperations repeatOperations;

	final private SkipPolicy writeSkipPolicy;

	final private SkipPolicy processSkipPolicy;

	final private SkipPolicy readSkipPolicy;

	final private Classifier<Throwable, Boolean> rollbackClassifier;

	public AbstractFaultTolerantChunkOrientedTasklet(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter,
			RetryOperations retryOperations, SkipPolicy readSkipPolicy, SkipPolicy processSkipPolicy,
			SkipPolicy writeSkipPolicy, Classifier<Throwable, Boolean> rollbackClassifier,
			RepeatOperations repeatTemplate) {

		super(itemReader, itemProcessor, itemWriter);
		this.retryOperations = retryOperations;
		this.readSkipPolicy = readSkipPolicy;
		this.processSkipPolicy = processSkipPolicy;
		this.writeSkipPolicy = writeSkipPolicy;
		this.rollbackClassifier = rollbackClassifier;
		this.repeatOperations = repeatTemplate;
	}

	protected SkipPolicy getReadSkipPolicy() {
		return readSkipPolicy;
	}

	protected RepeatOperations getRepeatOperations() {
		return repeatOperations;
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

	/**
	 * Incorporate retry into the item processor stage. If item processor
	 * returns null for an input item, it is considered filtered and is not
	 * added to outputs.
	 * 
	 * @param inputs the items to process
	 * @param outputs the items to write
	 * @param contribution current context
	 */
	protected void process(final StepContribution contribution, final List<I> inputs, final List<O> outputs,
			final Map<I, Exception> skippedInputs) throws Exception {

		int filtered = 0;

		for (final I item : inputs) {

			RetryCallback<O> retryCallback = new RetryCallback<O>() {

				public O doWithRetry(RetryContext context) throws Exception {
					O output = doProcess(item);
					return output;
				}

			};

			RecoveryCallback<O> recoveryCallback = new RecoveryCallback<O>() {

				public O recover(RetryContext context) throws Exception {
					Exception e = (Exception) context.getLastThrowable();
					if (processSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
						contribution.incrementProcessSkipCount();
						skippedInputs.put(item, e);
						logger.debug("Skipping after failed process", e);
						return null;
					}
					else {
						throw new RetryException("Non-skippable exception in recoverer while processing", e);
					}
				}

			};

			O output = retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(item,
					rollbackClassifier));
			if (output != null) {
				outputs.add(output);
			}
			else {
				filtered++;
			}

		}

		contribution.incrementFilterCount(filtered);

	}

	/**
	 * Execute the business logic, delegating to the writer.<br/>
	 * 
	 * Process the items with the {@link ItemWriter} in a stateful retry. Any
	 * {@link SkipListener} provided is called when retry attempts are
	 * exhausted. The listener callback (on write failure) will happen in the
	 * next transaction automatically.<br/>
	 */
	protected void write(final List<O> chunk, final StepContribution contribution, final Map<O, Exception> skipped)
			throws Exception {

		RetryCallback<Object> retryCallback = new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext context) throws Exception {
				doWrite(chunk);
				contribution.incrementWriteCount(chunk.size());
				return null;
			}
		};

		RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

			public Object recover(RetryContext context) throws Exception {
				Exception le = (Exception) context.getLastThrowable();
				if (!writeSkipPolicy.shouldSkip(le, contribution.getSkipCount())) {
					throw new RetryException("Non-skippable exception in recoverer", le);
				}
				if (chunk.size() == 1) {
					O item = chunk.get(0);
					checkSkipPolicy(item, le, contribution);
					return null;
				}
				if (!rollbackClassifier.classify(le)) {
					throw new RetryException(
							"Invalid retry state during write caused by exception that does not classify for rollback: ",
							le);
				}
				for (O item : chunk) {
					try {
						doWrite(Collections.singletonList(item));
						contribution.incrementWriteCount(1);
					}
					catch (Exception e) {
						checkSkipPolicy(item, e, contribution);
						if (rollbackClassifier.classify(e)) {
							throw e;
						}
						else {
							throw new RetryException(
									"Invalid retry state during recovery caused by exception that does not classify for rollback: ",
									e);
						}
					}
				}

				return null;

			}

			private void checkSkipPolicy(O item, Exception e, StepContribution contribution) {
				if (writeSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
					contribution.incrementWriteSkipCount();
					skipped.put(item, e);
					logger.debug("Skipping after failed write", e);
				}
				else {
					throw new RetryException("Non-skippable exception in recoverer", e);
				}
			}

		};

		retryOperations.execute(retryCallback, recoveryCallback, new DefaultRetryState(chunk, rollbackClassifier));

	}
}

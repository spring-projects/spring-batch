package org.springframework.batch.core.step.item;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.support.DefaultRetryState;
import org.springframework.batch.support.Classifier;

public class FaultTolerantChunkProcessor<I, O> extends SimpleChunkProcessor<I, O> {

	private SkipPolicy itemProcessSkipPolicy = new LimitCheckingItemSkipPolicy(0);

	private SkipPolicy itemWriteSkipPolicy = new LimitCheckingItemSkipPolicy(0);

	private final BatchRetryTemplate batchRetryTemplate;

	private Classifier<Throwable, Boolean> rollbackClassifier;

	private Log logger = LogFactory.getLog(getClass());

	private boolean buffering;

	public void setProcessSkipPolicy(SkipPolicy SkipPolicy) {
		this.itemProcessSkipPolicy = SkipPolicy;
	}

	public void setWriteSkipPolicy(SkipPolicy SkipPolicy) {
		this.itemWriteSkipPolicy = SkipPolicy;
	}

	public void setRollbackClassifier(Classifier<Throwable, Boolean> rollbackClassifier) {
		this.rollbackClassifier = rollbackClassifier;
	}

	public void setBuffering(boolean buffering) {
		this.buffering = buffering;
	}

	public FaultTolerantChunkProcessor(ItemProcessor<? super I, ? extends O> itemProcessor,
			ItemWriter<? super O> itemWriter, BatchRetryTemplate batchRetryTemplate) {
		super(itemProcessor, itemWriter);
		this.batchRetryTemplate = batchRetryTemplate;
	}

	@Override
	protected Chunk<O> transform(final StepContribution contribution, Chunk<I> inputs) throws Exception {

		Chunk<O> outputs = new Chunk<O>();

		for (final Chunk<I>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {

			final I item = iterator.next();

			RetryCallback<O> retryCallback = new RetryCallback<O>() {

				public O doWithRetry(RetryContext context) throws Exception {
					O output = doProcess(item);
					if (output == null) {
						// No need to re-process filtered items
						iterator.remove();
					}
					return output;
				}

			};

			RecoveryCallback<O> recoveryCallback = new RecoveryCallback<O>() {

				public O recover(RetryContext context) throws Exception {
					Exception e = (Exception) context.getLastThrowable();
					if (itemProcessSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
						contribution.incrementProcessSkipCount();
						iterator.remove(e);
						logger.debug("Skipping after failed process", e);
						return null;
					}
					else {
						throw new RetryException("Non-skippable exception in recoverer while processing", e);
					}
				}

			};

			// TODO: is it OK to use the item as a key for the retry state?
			O output = batchRetryTemplate.execute(retryCallback, recoveryCallback, new DefaultRetryState(item,
					rollbackClassifier));
			if (output != null) {
				outputs.add(output);
			}

		}

		return outputs;

	}

	@Override
	protected void write(final StepContribution contribution, final Chunk<I> inputs, final Chunk<O> outputs)
			throws Exception {

		RetryCallback<Object> retryCallback = new RetryCallback<Object>() {
			public Object doWithRetry(RetryContext context) throws Exception {
				doWrite(outputs.getItems());
				contribution.incrementWriteCount(outputs.size());
				return null;
			}
		};

		RecoveryCallback<Object> recoveryCallback = new RecoveryCallback<Object>() {

			public Object recover(RetryContext context) throws Exception {

				Exception le = (Exception) context.getLastThrowable();
				if (outputs.size() > 1 && !rollbackClassifier.classify(le)) {
					throw new RetryException("Invalid retry state during write caused by "
							+ "exception that does not classify for rollback: ", le);
				}

				boolean singleton = outputs.size() == 1;

				Chunk<I>.ChunkIterator inputIterator = inputs.iterator();
				for (Chunk<O>.ChunkIterator outputIterator = outputs.iterator(); outputIterator.hasNext();) {

					inputIterator.next();
					O item = outputIterator.next();
					if (singleton) {
						checkSkipPolicy(inputIterator, outputIterator, le, contribution);
						return null;
					}

					try {
						doWrite(Collections.singletonList(item));
						contribution.incrementWriteCount(1);
					}
					catch (Exception e) {
						checkSkipPolicy(inputIterator, outputIterator, e, contribution);
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

		};

		RecoveryCallback<Object> batchRecoveryCallback = new RecoveryCallback<Object>() {

			public Object recover(RetryContext context) throws Exception {

				Exception e = (Exception) context.getLastThrowable();
				if (outputs.size() > 1 && !rollbackClassifier.classify(e)) {
					throw new RetryException("Invalid retry state during write caused by "
							+ "exception that does not classify for rollback: ", e);
				}

				Chunk<I>.ChunkIterator inputIterator = inputs.iterator();
				for (Chunk<O>.ChunkIterator outputIterator = outputs.iterator(); outputIterator.hasNext();) {

					inputIterator.next();
					outputIterator.next();

					checkSkipPolicy(inputIterator, outputIterator, e, contribution);
					if (!rollbackClassifier.classify(e)) {
						throw new RetryException(
								"Invalid retry state during recovery caused by exception that does not classify for rollback: ",
								e);
					}

				}

				return null;

			}

		};

		if (!buffering) {
			batchRetryTemplate.execute(retryCallback, batchRecoveryCallback, BatchRetryTemplate.createState(inputs
					.getItems(), rollbackClassifier));
		}
		else {
			batchRetryTemplate.execute(retryCallback, recoveryCallback, new DefaultRetryState(inputs,
					rollbackClassifier));
		}

	}

	private void checkSkipPolicy(Chunk<I>.ChunkIterator inputIterator, Chunk<O>.ChunkIterator outputIterator,
			Exception e, StepContribution contribution) {
		if (itemWriteSkipPolicy.shouldSkip(e, contribution.getStepSkipCount())) {
			contribution.incrementWriteSkipCount();
			inputIterator.remove();
			outputIterator.remove(e);
			logger.debug("Skipping after failed write", e);
		}
		else {
			throw new RetryException("Non-skippable exception in recoverer", e);
		}
	}

}

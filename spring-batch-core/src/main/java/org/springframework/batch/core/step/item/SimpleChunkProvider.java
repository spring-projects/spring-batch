package org.springframework.batch.core.step.item;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * 
 * @author Dave Syer
 * 
 * @param <I> input item type
 */
public class SimpleChunkProvider<I> implements ChunkProvider<I> {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final ItemReader<? extends I> itemReader;

	private final MulticasterBatchListener<I, ?> listener = new MulticasterBatchListener<I, Object>();

	private final RepeatOperations repeatOperations;

	public SimpleChunkProvider(ItemReader<? extends I> itemReader, RepeatOperations repeatOperations) {
		this.itemReader = itemReader;
		this.repeatOperations = repeatOperations;
	}

	/**
	 * Register some {@link StepListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage.
	 * 
	 * @param listeners
	 */
	public void setListeners(List<? extends StepListener> listeners) {
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

	public Chunk<I> provide(final StepContribution contribution) throws Exception {

		final Chunk<I> inputs = new Chunk<I>();
		repeatOperations.iterate(new RepeatCallback() {

			public RepeatStatus doInIteration(final RepeatContext context) throws Exception {
				I item = read(contribution, inputs);
				if (item == null) {
					inputs.setEnd();
					return RepeatStatus.FINISHED;
				}
				inputs.add(item);
				contribution.incrementReadCount();
				return RepeatStatus.CONTINUABLE;
			}

		});

		return inputs;

	}
	
	public void postProcess(StepContribution contribution, Chunk<I> chunk) {
		for (Exception e : chunk.getErrors()) {
			try {
				listener.onSkipInRead(e);
			}
			catch (RuntimeException ex) {
				throw new SkipListenerFailedException("Fatal exception in SkipListener.", ex, e);
			}
		}		
	}

	protected I read(StepContribution contribution, Chunk<I> chunk) throws Exception {
		return doRead();
	}

}

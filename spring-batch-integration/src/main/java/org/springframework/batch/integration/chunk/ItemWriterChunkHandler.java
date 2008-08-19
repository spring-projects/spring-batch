package org.springframework.batch.integration.chunk;

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.listener.CompositeSkipListener;
import org.springframework.batch.core.step.skip.ItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.integration.annotation.Handler;
import org.springframework.transaction.annotation.Transactional;

public class ItemWriterChunkHandler<T> implements ChunkHandler<T> {

	private static final Log logger = LogFactory.getLog(ItemWriterChunkHandler.class);

	private ItemWriter<? super T> itemWriter;

	private ItemSkipPolicy itemSkipPolicy = new NeverSkipItemSkipPolicy();

	private CompositeSkipListener skipListener = new CompositeSkipListener();

	public void setItemSkipPolicy(ItemSkipPolicy itemSkipPolicy) {
		this.itemSkipPolicy = itemSkipPolicy;
	}

	public void setItemWriter(ItemWriter<? super T> itemWriter) {
		this.itemWriter = itemWriter;
	}

	public void registerSkipListener(SkipListener listener) {
		skipListener.register(listener);
	}

	public void setSkipListeners(SkipListener[] skipListeners) {
		for (SkipListener listener : skipListeners) {
			registerSkipListener(listener);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.integration.batch.slave.ChunkHandler#handleChunk(java.util.Collection)
	 */
	@Handler
	@Transactional
	public ChunkResponse handleChunk(ChunkRequest<? extends T> chunk) {

		logger.debug("Handling chunk: " + chunk);

		int parentSkipCount = chunk.getSkipCount();
		int skipCount = 0;

		try {
			for (T item : chunk.getItems()) {
				try {
					itemWriter.write(Collections.singletonList(item));
				}
				catch (Exception e) {
					if (itemSkipPolicy.shouldSkip(e, parentSkipCount + skipCount)) {
						logger.debug("Skipping item on exception", e);
						skipCount++;
						skipListener.onSkipInWrite(item, e);
					} else {
						logger.debug("Cannot skip, re-throwing");
						throw e;
					}
				}
			}
			itemWriter.flush();
		}
		catch (Exception e) {
			logger.debug("Failed chunk", e);
			itemWriter.clear();
			// TODO: need to force rollback as well
			return new ChunkResponse(ExitStatus.FAILED.addExitDescription(e.getClass().getName() + ": "
					+ e.getMessage()), chunk.getJobId(), skipCount);
		}

		logger.debug("Completed chunk handling with " + skipCount + " skips");
		return new ChunkResponse(ExitStatus.CONTINUABLE, chunk.getJobId(), skipCount);

	}
}

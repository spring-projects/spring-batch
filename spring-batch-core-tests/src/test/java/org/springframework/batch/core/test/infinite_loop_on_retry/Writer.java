package org.springframework.batch.core.test.infinite_loop_on_retry;

import java.util.List;

import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.ItemWriter;

public class Writer implements ItemWriter<Item>, ItemStreamWriter<Item> {

	private static final org.slf4j.Logger log = LoggerFactory
			.getLogger(Writer.class);

	private int cpt = 0;

	private ItemStreamWriter<Item> delegate;

	@Override
	public void write(List<? extends Item> items) throws Exception {
		log.debug("write " + items);

		cpt++;

		for (Item item : items) {
			item.incNbWritten();
		}

		delegate.write(items);

		if (cpt == 1) {
			throw new TestException("Error during write");
		}
	}

	public void setDelegate(ItemStreamWriter<Item> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void open(ExecutionContext executionContext)
			throws ItemStreamException {
		delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext)
			throws ItemStreamException {
		delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		delegate.close();
	}

}

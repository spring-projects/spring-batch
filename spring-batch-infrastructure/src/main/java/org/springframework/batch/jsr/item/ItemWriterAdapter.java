package org.springframework.batch.jsr.item;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.chunk.ItemWriter;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

@SuppressWarnings("rawtypes")
public class ItemWriterAdapter extends ItemStreamSupport implements org.springframework.batch.item.ItemWriter {

	private static final String CHECKPOINT_KEY = "writer.checkpoint";

	private ItemWriter delegate;

	public ItemWriterAdapter(ItemWriter writer) {
		Assert.notNull(writer, "An ItemWriter implementation is required");
		this.delegate = writer;
		super.setExecutionContextName(ClassUtils.getShortName(delegate.getClass()));
	}

	@Override
	public void open(ExecutionContext executionContext)
			throws ItemStreamException {
		try {
			delegate.open((Serializable) executionContext.get(getExecutionContextKey(CHECKPOINT_KEY)));
		} catch (Exception e) {
			throw new ItemStreamException(e);
		}
	}

	@Override
	public void update(ExecutionContext executionContext)
			throws ItemStreamException {
		try {
			Serializable checkpoint = delegate.checkpointInfo();
			executionContext.put(getExecutionContextKey(CHECKPOINT_KEY), checkpoint);
		} catch (Exception e) {
			throw new ItemStreamException(e);
		}
	}

	@Override
	public void close() throws ItemStreamException {
		try {
			delegate.close();
		} catch (Exception e) {
			throw new ItemStreamException(e);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void write(List items) throws Exception {
		delegate.writeItems(items);
	}
}

package org.springframework.batch.jsr.item;

import java.io.Serializable;

import javax.batch.api.chunk.ItemReader;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

@SuppressWarnings("rawtypes")
public class ItemReaderAdapter extends ItemStreamSupport implements org.springframework.batch.item.ItemReader {

	private static final String CHECKPOINT_KEY = "reader.checkpoint";

	private ItemReader delegate;

	public ItemReaderAdapter(ItemReader reader) {
		Assert.notNull(reader, "An ItemReader implementation is required");
		this.delegate = reader;
		setExecutionContextName(ClassUtils.getShortName(delegate.getClass()));
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
	public Object read() throws Exception, UnexpectedInputException, ParseException,
	NonTransientResourceException {
		return delegate.readItem();
	}
}

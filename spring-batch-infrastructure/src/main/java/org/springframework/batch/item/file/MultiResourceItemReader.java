package org.springframework.batch.item.file;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Reads items from multiple resources sequentially - resource list is given by
 * {@link #setResources(Resource[])}, the actual reading is delegated to
 * {@link #setDelegate(ResourceAwareItemReaderItemStream)}.
 * 
 * Reset (rollback) capability is implemented by item buffering. To restart
 * correctly resource ordering needs to be preserved between runs.
 * 
 * @see SortedMultiResourceItemReader
 * 
 * @author Robert Kasanicky
 */
public class MultiResourceItemReader extends ExecutionContextUserSupport implements ItemReader, ItemStream,
		InitializingBean {

	/**
	 * Unique object instance that marks resource boundaries in the item buffer
	 */
	private static final Object END_OF_RESOURCE_MARKER = new Object();

	private ResourceAwareItemReaderItemStream delegate;

	private Resource[] resources;

	private MultiResourceIndex index = new MultiResourceIndex();

	private List itemBuffer = new ArrayList();

	private ListIterator itemBufferIterator = null;

	private boolean shouldReadBuffer = false;

	private boolean saveState = false;

	public MultiResourceItemReader() {
		setName(ClassUtils.getShortName(MultiResourceItemReader.class));
	}

	/**
	 * Reads the next item, jumping to next resource if necessary.
	 */
	public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {

		Object item;
		if (shouldReadBuffer) {
			item = readBufferedItem();
		}
		else {
			item = readNextItem();
		}

		index.incrementItemCount();

		return item;
	}

	/**
	 * Use the delegate to read the next item, jump to next resource if current
	 * one is exhausted. Items are appended to the buffer.
	 * @return next item from input
	 */
	private Object readNextItem() throws Exception {

		Object item = delegate.read();

		while (item == null) {

			index.incrementResourceCount();

			if (index.currentResource >= resources.length) {
				return null;
			}
			itemBuffer.add(END_OF_RESOURCE_MARKER);

			delegate.close(new ExecutionContext());
			delegate.setResource(resources[index.currentResource]);
			delegate.open(new ExecutionContext());

			item = delegate.read();
		}

		itemBuffer.add(item);

		return item;
	}

	/**
	 * Read next item from buffer while keeping track of the position within the
	 * input for possible restart.
	 * @return next item from buffer
	 */
	private Object readBufferedItem() {

		Object buffered = itemBufferIterator.next();
		while (buffered == END_OF_RESOURCE_MARKER) {
			index.incrementResourceCount();
			buffered = itemBufferIterator.next();
		}

		if (!itemBufferIterator.hasNext()) {
			// buffer is exhausted, continue reading from file
			shouldReadBuffer = false;
			itemBufferIterator = null;
		}
		return buffered;
	}

	public void mark() throws MarkFailedException {
		emptyBuffer();

		index.mark();

		delegate.mark();
	}

	/**
	 * Discard the buffered items that have already been read.
	 */
	private void emptyBuffer() {
		if (!shouldReadBuffer) {
			itemBuffer.clear();
			itemBufferIterator = null;
		}
		else {
			itemBuffer = itemBuffer.subList(itemBufferIterator.nextIndex(), itemBuffer.size());
			itemBufferIterator = itemBuffer.listIterator();
		}
	}

	/**
	 * Switches to 'read from buffer' state.
	 * 
	 * @see ItemReader#reset()
	 */
	public void reset() throws ResetFailedException {
		if (!itemBuffer.isEmpty()) {
			shouldReadBuffer = true;
			itemBufferIterator = itemBuffer.listIterator();
		}
		index.reset();
	}

	/**
	 * Close the {@link #setDelegate(ResourceAwareItemReaderItemStream)} reader
	 * and reset instance variable values.
	 */
	public void close(ExecutionContext executionContext) throws ItemStreamException {
		shouldReadBuffer = false;
		itemBufferIterator = null;
		index = new MultiResourceIndex();
		itemBuffer.clear();
		delegate.close(new ExecutionContext());
	}

	/**
	 * Figure out which resource to start with in case of restart, open the
	 * delegate and restore delegate's position in the resource.
	 */
	public void open(ExecutionContext executionContext) throws ItemStreamException {

		index.open(executionContext);

		delegate.setResource(resources[index.currentResource]);

		delegate.open(new ExecutionContext());

		try {
			for (int i = 0; i < index.currentItem; i++) {
				delegate.read();
				delegate.mark();
			}
		}
		catch (Exception e) {
			throw new ItemStreamException("Could not restore position on restart", e);
		}
	}

	/**
	 * Store the current resource index and position in the resource.
	 */
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (saveState) {
			index.update(executionContext);
		}
	}

	/**
	 * @param delegate reads items from single {@link Resource}.
	 */
	public void setDelegate(ResourceAwareItemReaderItemStream delegate) {
		this.delegate = delegate;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notEmpty(resources, "There must be at least one input resource");
	}

	/**
	 * @param resources input resources
	 */
	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	/**
	 * Set the boolean indicating whether or not state should be saved in the
	 * provided {@link ExecutionContext} during the {@link ItemStream} call to
	 * update.
	 * 
	 * @param saveState
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * Facilitates keeping track of the position within multi-resource input.
	 */
	private class MultiResourceIndex {

		private static final String RESOURCE_KEY = "resourceIndex";

		private static final String ITEM_KEY = "itemIndex";

		private int currentResource = 0;

		private int markedResource = 0;

		private long currentItem = 0;

		private long markedItem = 0;

		public void incrementItemCount() {
			currentItem++;
		}

		public void incrementResourceCount() {
			currentResource++;
			currentItem = 0;
		}

		public void mark() {
			markedResource = currentResource;
			markedItem = currentItem;
		}

		public void reset() {
			currentResource = markedResource;
			currentItem = markedItem;
		}

		public void open(ExecutionContext ctx) {
			if (ctx.containsKey(getKey(RESOURCE_KEY))) {
				currentResource = Long.valueOf(ctx.getLong(getKey(RESOURCE_KEY))).intValue();
			}

			if (ctx.containsKey(getKey(ITEM_KEY))) {
				currentItem = ctx.getLong(getKey(ITEM_KEY));
			}
		}

		public void update(ExecutionContext ctx) {
			ctx.putLong(getKey(RESOURCE_KEY), index.currentResource);
			ctx.putLong(getKey(ITEM_KEY), index.currentItem);
		}
	}

}

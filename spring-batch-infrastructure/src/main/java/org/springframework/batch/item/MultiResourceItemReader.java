package org.springframework.batch.item;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
	 * Key for the index of the current resource
	 */
	private static final String RESOURCE_INDEX = "resourceIndex";

	/**
	 * Key for item count within current resource
	 */
	private static final String ITEM_COUNT = "itemIndex";

	/**
	 * Unique object instance that marks resource boundaries in the item buffer
	 */
	private static final Object END_OF_RESOURCE_MARKER = new Object();

	private ResourceAwareItemReaderItemStream delegate;

	private Resource[] resources;

	private int currentResourceIndex = 0;

	private int lastMarkedResourceIndex = 0;

	private long currentItemIndex = 0;

	private long lastMarkedItemIndex = 0;

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
			if (itemBufferIterator.hasNext()) {
				item = readBufferedItem();
			}
			else {
				// buffer is exhausted, continue reading from file
				shouldReadBuffer = false;
				itemBufferIterator = null;
				item = readNextItem();
			}
		}
		else {
			item = readNextItem();
		}

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

			incrementResourceIndex();

			if (currentResourceIndex >= resources.length) {
				return null;
			}
			itemBuffer.add(END_OF_RESOURCE_MARKER);

			delegate.close(new ExecutionContext());
			delegate.setResource(resources[currentResourceIndex]);
			delegate.open(new ExecutionContext());

			item = delegate.read();
		}

		itemBuffer.add(item);

		currentItemIndex++;

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
			incrementResourceIndex();
			buffered = itemBufferIterator.next();
		}
		currentItemIndex++;
		return buffered;
	}

	/**
	 * Adjust indexes for next resource.
	 */
	private void incrementResourceIndex() {
		currentResourceIndex++;
		currentItemIndex = 0;
	}

	/**
	 * Clears the item buffer and cancels reading from buffer if it applies.
	 * 
	 * @see ItemReader#mark()
	 */
	public void mark() throws MarkFailedException {
		emptyBuffer();

		lastMarkedResourceIndex = currentResourceIndex;
		lastMarkedItemIndex = currentItemIndex;

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
		shouldReadBuffer = true;
		itemBufferIterator = itemBuffer.listIterator();
		currentResourceIndex = lastMarkedResourceIndex;
		currentItemIndex = lastMarkedItemIndex;
	}

	/**
	 * Close the {@link #setDelegate(ResourceAwareItemReaderItemStream)} reader
	 * and reset instance variable values.
	 */
	public void close(ExecutionContext executionContext) throws ItemStreamException {
		shouldReadBuffer = false;
		lastMarkedResourceIndex = 0;
		currentResourceIndex = 0;
		currentItemIndex = 0;
		lastMarkedItemIndex = 0;
		itemBufferIterator = null;
		itemBuffer.clear();
		delegate.close(executionContext);
	}

	/**
	 * Figure out which resource to start with in case of restart, open the
	 * delegate and restore delegate's position in the resource.
	 */
	public void open(ExecutionContext executionContext) throws ItemStreamException {

		if (executionContext.containsKey(getKey(RESOURCE_INDEX))) {
			currentResourceIndex = Long.valueOf(executionContext.getLong(getKey(RESOURCE_INDEX))).intValue();
			lastMarkedResourceIndex = currentResourceIndex;
		}

		if (executionContext.containsKey(getKey(ITEM_COUNT))) {
			currentItemIndex = executionContext.getLong(getKey(ITEM_COUNT));
			lastMarkedItemIndex = currentItemIndex;
		}

		delegate.setResource(resources[currentResourceIndex]);

		delegate.open(new ExecutionContext());

		try {
			for (int i = 0; i < currentItemIndex; i++) {
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
			executionContext.putLong(getKey(RESOURCE_INDEX), currentResourceIndex);
			executionContext.putLong(getKey(ITEM_COUNT), currentItemIndex);
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

}

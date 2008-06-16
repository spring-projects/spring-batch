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

	private static final String RESOURCE_INDEX = "resourceIndex";
	
	private static final String ITEM_COUNT = "itemCount";
	
	private static final Object END_OF_RESOURCE_MARKER = new Object();

	private ResourceAwareItemReaderItemStream delegate;

	private Resource[] resources;

	private int currentResourceIndex = 0;
	
	private int lastMarkedResourceIndex = 0;
	
	private long currentResourceItemCount = 0;
	
	private long lastMarkedResourceItemCount = 0;

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

		if (shouldReadBuffer) {
			if (itemBufferIterator.hasNext()) {
				Object buffered = itemBufferIterator.next();
				while (buffered == END_OF_RESOURCE_MARKER) {
					currentResourceIndex++;
					buffered = itemBufferIterator.next();
				}
				currentResourceItemCount++;
				return buffered;
			}
			else {
				// buffer is exhausted, continue reading from file
				shouldReadBuffer = false;
				itemBufferIterator = null;
			}
		}

		Object item = delegate.read();
		currentResourceItemCount++;

		while (item == null) {

			if (++currentResourceIndex >= resources.length) {
				currentResourceItemCount = 0;
				return null;
			}
			delegate.close(new ExecutionContext());
			delegate.setResource(resources[currentResourceIndex]);
			delegate.open(new ExecutionContext());
			item = delegate.read();
			itemBuffer.add(END_OF_RESOURCE_MARKER);
			currentResourceItemCount = 1;

		}

		itemBuffer.add(item);

		return item;
	}

	/**
	 * Clears the item buffer and cancels reading from buffer if it applies.
	 * 
	 * @see ItemReader#mark()
	 */
	public void mark() throws MarkFailedException {
		if (!shouldReadBuffer) {
			itemBuffer.clear();
			itemBufferIterator = null;
		}
		else {
			itemBuffer = itemBuffer.subList(itemBufferIterator.nextIndex(), itemBuffer.size());
			itemBufferIterator = itemBuffer.listIterator();
		}
		
		lastMarkedResourceIndex = currentResourceIndex;
		lastMarkedResourceItemCount = currentResourceItemCount;
		delegate.mark();
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
		currentResourceItemCount = lastMarkedResourceItemCount;
	}

	/**
	 * Close the {@link #setDelegate(ResourceAwareItemReaderItemStream)} reader
	 * and reset instance variable values.
	 */
	public void close(ExecutionContext executionContext) throws ItemStreamException {
		shouldReadBuffer = false;
		lastMarkedResourceIndex = 0;
		currentResourceIndex = 0;
		currentResourceItemCount = 0;
		lastMarkedResourceItemCount = 0;
		itemBufferIterator = null;
		itemBuffer.clear();
		delegate.close(executionContext);
	}

	/**
	 * Figure out which resource to start with in case of restart and open the
	 * delegate.
	 */
	public void open(ExecutionContext executionContext) throws ItemStreamException {

		if (executionContext.containsKey(getKey(RESOURCE_INDEX))) {
			currentResourceIndex = Long.valueOf(executionContext.getLong(getKey(RESOURCE_INDEX))).intValue();
		}
		
		if (executionContext.containsKey(getKey(ITEM_COUNT))) {
			currentResourceItemCount = executionContext.getLong(getKey(ITEM_COUNT));
		}

		delegate.setResource(resources[currentResourceIndex]);

		delegate.open(new ExecutionContext());
		
		for (int i = 0; i < currentResourceItemCount; i++) {
			try {
				delegate.read();
				delegate.mark();
			}
			catch (Exception e) {
				throw new ItemStreamException("Could not restore position on restart", e);
			}
		}
	}

	/**
	 * Store the current resource index and delegate's data.
	 */
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (saveState) {
			executionContext.putLong(getKey(RESOURCE_INDEX), currentResourceIndex);
			executionContext.putLong(getKey(ITEM_COUNT), currentResourceItemCount);
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

package org.springframework.batch.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

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

	private ResourceAwareItemReaderItemStream delegate;

	private Resource[] resources;

	private int currentResourceIndex;

	private List itemBuffer = new ArrayList();

	private Iterator itemBufferIterator = null;

	private boolean shouldReadBuffer = false;

	private boolean saveState = false;

	public MultiResourceItemReader() {
		setName(MultiResourceItemReader.class.getSimpleName());
	}

	/**
	 * Reads the next item, jumping to next resource if necessary.
	 */
	public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {

		if (shouldReadBuffer) {
			if (itemBufferIterator.hasNext()) {
				return itemBufferIterator.next();
			}
			else {
				// buffer is exhausted, continue reading from file
				shouldReadBuffer = false;
				itemBufferIterator = null;
			}
		}

		Object item = delegate.read();

		while (item == null) {

			if (++currentResourceIndex >= resources.length) {
				return null;
			}
			delegate.close(new ExecutionContext());
			delegate.setResource(resources[currentResourceIndex]);
			delegate.open(new ExecutionContext());
			item = delegate.read();

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
		delegate.mark();
		itemBuffer.clear();
		shouldReadBuffer = false;
	}

	/**
	 * Switches to 'read from buffer' state.
	 * 
	 * @see ItemReader#reset()
	 */
	public void reset() throws ResetFailedException {
		shouldReadBuffer = true;
		itemBufferIterator = itemBuffer.listIterator();
	}

	/**
	 * Close the {@link #setDelegate(ResourceAwareItemReaderItemStream)} reader
	 * and reset instance variable values.
	 */
	public void close(ExecutionContext executionContext) throws ItemStreamException {
		shouldReadBuffer = false;
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
			int index = Long.valueOf(executionContext.getLong(getKey(RESOURCE_INDEX))).intValue();
			currentResourceIndex = index;
		}

		delegate.setResource(resources[currentResourceIndex]);

		delegate.open(executionContext);
	}

	/**
	 * Store the current resource index and delegate's data.
	 */
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (saveState) {
			executionContext.putLong(getKey(RESOURCE_INDEX), currentResourceIndex);
			delegate.update(executionContext);
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

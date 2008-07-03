package org.springframework.batch.item.support;

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
import org.springframework.util.Assert;

/**
 * Abstract superclass for {@link ItemReader}s which use item buffering to
 * support reset/rollback. Supports restart by storing item count in the
 * {@link ExecutionContext} (therefore requires item ordering to be preserved
 * between runs).
 * 
 * Subclasses are inherently *not* thread-safe.
 * 
 * @author Robert Kasanicky
 */
public abstract class AbstractBufferedItemReaderItemStream implements ItemReader, ItemStream {

	private static final String READ_COUNT = "read.count";

	private int currentItemCount = 0;

	private int lastMarkedItemCount = 0;

	private boolean shouldReadBuffer = false;

	private List itemBuffer = new ArrayList();

	private ListIterator itemBufferIterator = null;

	private int lastMarkedBufferIndex = 0;

	private ExecutionContextUserSupport ecSupport = new ExecutionContextUserSupport();

	private boolean saveState = false;

	/**
	 * Read next item from input.
	 * @return item
	 * @throws Exception
	 */
	protected abstract Object doRead() throws Exception;
	
	/**
	 * Open resources necessary to start reading input.
	 */
	protected abstract void doOpen() throws Exception;

	/**
	 * Close the resources opened in {@link #doOpen()}.
	 */
	protected abstract void doClose() throws Exception;

	/**
	 * Move to the given item index. Subclasses should override this method if
	 * there is a more efficient way of moving to given index than re-reading
	 * the input using {@link #doRead()}.
	 */
	protected void jumpToItem(int itemIndex) throws Exception {
		for (int i = 0; i < itemIndex; i++) {
			doRead();
		}
	}

	public Object read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {

		currentItemCount++;

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

		Object item = doRead();
		itemBuffer.add(item);

		return item;
	}

	/**
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.support.AbstractItemReader#mark()
	 */
	public void mark() throws MarkFailedException {

		if (!shouldReadBuffer) {
			itemBuffer.clear();
			itemBufferIterator = null;
			lastMarkedBufferIndex = 0;
		}
		else {
			lastMarkedBufferIndex = itemBufferIterator.nextIndex();
		}

		lastMarkedItemCount = currentItemCount;
	}

	public void reset() throws ResetFailedException {

		currentItemCount = lastMarkedItemCount;
		shouldReadBuffer = true;
		itemBufferIterator = itemBuffer.listIterator(lastMarkedBufferIndex);
	}

	protected int getCurrentItemCount() {
		return currentItemCount;
	}

	protected void setCurrentItemCount(int count) {
		this.currentItemCount = count;
	}

	public void close(ExecutionContext executionContext) throws ItemStreamException {
		currentItemCount = 0;
		lastMarkedItemCount = 0;
		lastMarkedBufferIndex = 0;
		itemBufferIterator = null;
		shouldReadBuffer = false;
		itemBuffer.clear();

		try {
			doClose();
		}
		catch (Exception e) {
			throw new ItemStreamException("Error while closing item reader", e);
		}
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {

		try {
			doOpen();
		}
		catch (Exception e) {
			throw new ItemStreamException("Failed to initialize the reader", e);
		}

		if (executionContext.containsKey(ecSupport.getKey(READ_COUNT))) {
			int itemCount = new Long(executionContext.getLong(ecSupport.getKey(READ_COUNT))).intValue();

			try {
				jumpToItem(itemCount);
			}
			catch (Exception e) {
				throw new ItemStreamException("Could not move to stored position on restart", e);
			}

			currentItemCount = itemCount;
		}

	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putLong(ecSupport.getKey(READ_COUNT), currentItemCount);
		}

	}

	public void setName(String name) {
		ecSupport.setName(name);
	}

	/**
	 * Set the flag that determines whether to save internal data for
	 * {@link ExecutionContext}. Only switch this to false if you don't want to
	 * save any state from this stream, and you don't need it to be restartable.
	 * 
	 * @param saveState flag value (default true)
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

}

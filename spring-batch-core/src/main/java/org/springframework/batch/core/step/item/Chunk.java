package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
class Chunk<W> implements Iterable<W> {

	private List<W> items = new ArrayList<W>();

	private int current = 0;

	private int last = 0;

	private Exception exception = null;

	private boolean skipped = false;

	/**
	 * Add the item to the chunk.
	 * @param item
	 */
	public void add(W item) {
		items.add(item);
		last++;
	}

	/**
	 * Clear the items down to signal that we are done.
	 */
	public void clear() {
		items.clear();
		last = 0;
		current = 0;
	}

	/**
	 * @return a copy of the items to be processed as an unmodifiable list
	 */
	public List<W> getItems() {
		return Collections.unmodifiableList(new ArrayList<W>(items.subList(current, last)));
	}

	/**
	 * @return true if there are no items in the chunk
	 */
	public boolean isEmpty() {
		return items.isEmpty();
	}

	/**
	 * Get an unmodifiable iterator for the underlying items.
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<W> iterator() {
		return getItems().iterator();
	}

	/**
	 * @return true if the chunk is ready for a retry attempt
	 */
	public boolean canRetry() {
		return exception == null || canSkip();
	}

	/**
	 * Re-throw the last exception if there was one, and reset. Subsequent calls
	 * would do nothing until {@link #rethrow(Exception)} is called.
	 * 
	 * @throws Exception if there is a last exception
	 */
	public void rethrow() throws Exception {
		int size = items.size();
		Exception throwable = exception;
		if (exception != null && !skipped) {
			if (current == 0 && last == size) {
				// we tried all items and there was no exception
				exception = null;
			}
			else {
				// we tried some but not all elements with no exception
				current = last;
			}
		}
		if (skipped) {
			skipped = false;
		}
		last = size; // reset end point of scan
		if (current == size) {
			// we scanned all the elements
			current = 0;
			exception = null;
		}
		if (throwable != null) {
			throw throwable;
		}
	}

	/**
	 * Get the skipped item and remove it from the backing list.
	 * @return the item that can be skipped
	 */
	public W getSkippedItem() {
		Assert.state(canSkip(), "To remove a skipped item it has to be unique");
		W item = items.remove(current);
		if (last > items.size()) {
			last = items.size();
		}
		skipped = true;
		return item;
	}

	/**
	 * @param e an exception to register and re-throw
	 * @throws Exception the exception passed in
	 */
	public void rethrow(Exception e) throws Exception {
		exception = e;
		// narrow the search for the failed item
		last = current + (last - current) / 2;
		// ... unless it would lead to processing no data
		if (last==current) {
			last = current + 1;
		}
		throw e;
	}

	/**
	 * @return true if there is a single item waiting, so it can be identified
	 * and passed to listeners
	 */
	public boolean canSkip() {
		return current == last - 1 && current < items.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("items=%s, canSkip=%s, current=%d, last=%d", items, canSkip(), current, last);
	}

}
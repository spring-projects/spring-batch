package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Encapsulation of a list of items to be processed and possibly a list of
 * failed items to be skipped. To mark an item as skipped clients should iterate
 * over the chunk using the {@link #iterator()} method, and if there is a
 * failure call {@link ChunkIterator#remove(Exception)} on the iterator.
 * The skipped items are then available through the chunk.
 * 
 * @author Dave Syer
 * 
 */
class Chunk<W> implements Iterable<W> {

	private List<W> items = new ArrayList<W>();

	private List<SkipWrapper<W>> skips = new ArrayList<SkipWrapper<W>>();

	private List<Exception> errors = new ArrayList<Exception>();
	
	private Object userData;

	private boolean end;

	public Chunk() {
		this(null,null);
	}
	
	public Chunk(List<W> items, List<SkipWrapper<W>> skips) {
		super();
		if (items!=null) {
			this.items = new ArrayList<W>(items);
		}
		if (skips!=null) {
			this.skips = new ArrayList<SkipWrapper<W>>(skips);
		}
	}

	/**
	 * Add the item to the chunk.
	 * @param item
	 */
	public void add(W item) {
		items.add(item);
	}

	/**
	 * Clear the items down to signal that we are done.
	 */
	public void clear() {
		items.clear();
		skips.clear();
		userData = null;
	}

	/**
	 * @return a copy of the items to be processed as an unmodifiable list
	 */
	public List<W> getItems() {
		return Collections.unmodifiableList(new ArrayList<W>(items));
	}

	/**
	 * @return a copy of the skips as an unmodifiable list
	 */
	public List<SkipWrapper<W>> getSkips() {
		return Collections.unmodifiableList(skips);
	}

	/**
	 * @return a copy of the anonymous errros as an unmodifiable list
	 */
	public List<Exception> getErrors() {
		return Collections.unmodifiableList(errors);
	}

	/**
	 * Register an anonymous skip. To skip an individual item, use
	 * {@link ChunkIterator#remove()}.
	 * 
	 * @param e the exception that caused the skip
	 */
	public void skip(Exception e) {
		errors.add(e);
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
	public ChunkIterator iterator() {
		return new ChunkIterator(items);
	}

	/**
	 * @return the number of items (excluding skips)
	 */
	public int size() {
		return items.size();
	}

	public boolean isEnd() {
		return end;
	}

	public void setEnd() {
		this.end = true;
	}

	public Object getUserData() {
		return userData;
	}

	public void setUserData(Object userData) {
		this.userData = userData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("[items=%s, skips=%s]", items, skips);
	}

	/**
	 * Special iterator for a chunk providing the
	 * {@link #remove(Exception)} method for dynamically removing an
	 * item and adding it to the skips.
	 * 
	 * @author Dave Syer
	 * 
	 */
	public class ChunkIterator implements Iterator<W> {

		final private Iterator<W> iterator;

		private W next;

		public ChunkIterator(List<W> items) {
			iterator = items.iterator();
		}

		public boolean hasNext() {
			return iterator.hasNext();
		}

		public W next() {
			next = iterator.next();
			return next;
		}

		public void remove(Exception e) {
			remove();
			skips.add(new SkipWrapper<W>(next, e));
		}

		public void remove() {
			if (next == null) {
				if (iterator.hasNext()) {
					next = iterator.next();
				}
				else {
					return;
				}
			}
			iterator.remove();
		}

	}

}
package org.springframework.batch.core.step.item;

/**
 * Wrapper for an item and its exception if it failed processing.
 * 
 * @author Dave Syer
 * 
 */
public class ItemWrapper<T> {

	final private Exception exception;

	final private T item;

	final private int skipCount;

	/**
	 * @param item
	 */
	public ItemWrapper(T item) {
		this(item, null, 0);
	}

	/**
	 * @param item
	 * @param skipCount
	 */
	public ItemWrapper(T item, int skipCount) {
		this(item, null, skipCount);
	}

	public ItemWrapper(T item, Exception e) {
		this(item, e, 0);
	}

	public ItemWrapper(T item, Exception e, int skipCount) {
		this.item = item;
		this.exception = e;
		this.skipCount = skipCount;
	}

	/**
	 * Public getter for the skipCount.
	 * @return the skipCount
	 */
	public int getSkipCount() {
		return skipCount;
	}

	/**
	 * Public getter for the exception.
	 * @return the exception
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Public getter for the item.
	 * @return the item
	 */
	public T getItem() {
		return item;
	}

	@Override
	public String toString() {
		return String.format("[exception=%s, item=%s, skips=%d]", exception, item, skipCount);
	}

}
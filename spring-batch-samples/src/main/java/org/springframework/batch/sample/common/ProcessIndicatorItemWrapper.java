package org.springframework.batch.sample.common;

/**
 * Item wrapper useful in "process indicator" usecase, where input is marked as
 * processed by the processor/writer. This requires passing a technical
 * identifier of the input data so that it can be modified in later stages.
 * 
 * @param <T> item type
 * 
 * @see StagingItemReader
 * @see StagingItemProcessor
 * 
 * @author Robert Kasanicky
 */
public class ProcessIndicatorItemWrapper<T> {

	private long id;

	private T item;

	public ProcessIndicatorItemWrapper(long id, T item) {
		this.id = id;
		this.item = item;
	}

	/**
	 * @return id identifying the input data (typically row in database)
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return item (domain object for business processing)
	 */
	public T getItem() {
		return item;
	}
}

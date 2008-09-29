package org.springframework.batch.item.file.transform;


public class PassThroughLineAggregator<T> implements LineAggregator<T> {

	/**
	 * Simply convert to a String with toString().
	 * 
	 * @see org.springframework.batch.item.file.transform.LineAggregator#process(java.lang.Object)
	 */
	public String process(T item) {
		return item.toString();
	}

}

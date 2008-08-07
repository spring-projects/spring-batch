package org.springframework.batch.item.file.transform;


public class PassThroughLineAggregator<T> implements LineAggregator<T> {

	/**
	 * Simply convert to a String with toString().
	 * 
	 * @see org.springframework.batch.item.file.transform.LineAggregator#aggregate(java.lang.Object)
	 */
	public String aggregate(T item) {
		return item.toString();
	}

}

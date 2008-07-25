package org.springframework.batch.sample.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.support.AbstractItemWriter;
import org.springframework.batch.item.validator.ValidationException;

/**
 * Remembers all items written - useful for testing.
 */
public class ItemTrackingItemWriter<T> extends AbstractItemWriter<T> {

	private List<T> items = new ArrayList<T>();
	
	private int failure = -1;
	
	private int counter = 0;
	
	public void write(T item) throws Exception {
		
		items.add(item);
		if (++counter == failure) {
			throw new ValidationException("validation failed");
		}
	}

	public List<T> getItems() {
		return items;
	}

	public void setValidationFailure(int failure) {
		this.failure = failure;
	}

}

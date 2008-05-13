package org.springframework.batch.sample.item.writer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.AbstractItemWriter;
import org.springframework.batch.item.validator.ValidationException;

/**
 * Remembers all items written - useful for testing.
 */
public class ItemTrackingItemWriter extends AbstractItemWriter {

	private List items = new ArrayList();
	
	private int failure = -1;
	
	private int counter = 0;
	
	public void write(Object item) throws Exception {
		
		items.add(item);
		if (++counter == failure) {
			throw new ValidationException("validation failed");
		}
	}

	public List getItems() {
		return items;
	}

	public void setValidationFailure(int failure) {
		this.failure = failure;
	}

}

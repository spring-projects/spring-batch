package org.springframework.batch.item.validator;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.Assert;

/**
 * Simple implementation of {@link ItemProcessor} validates and input and
 * returns it without modifications.
 * 
 * @author Robert Kasanicky
 * 
 */
public class ValidatingItemProcessor<T> implements ItemProcessor<T, T> {

	private Validator validator;
	
	public ValidatingItemProcessor(Validator validator){
		Assert.notNull(validator, "Validator must not be null.");
		this.validator = validator;
	}

	/**
	 * Set the validator used to validate each item.
	 * 
	 * @param validator
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Validate the item and return it unmodified
	 * 
	 * @return the input item
	 * @throws ValidationException if validation fails
	 */
	public T process(T item) throws ValidationException {
		validator.validate(item);
		return item;
	}

}

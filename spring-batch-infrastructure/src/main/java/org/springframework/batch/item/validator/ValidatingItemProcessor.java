/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

	private Validator<? super T> validator;
	
	public ValidatingItemProcessor(Validator<? super T> validator){
		Assert.notNull(validator, "Validator must not be null.");
		this.validator = validator;
	}

	/**
	 * Set the validator used to validate each item.
	 * 
	 * @param validator
	 */
	public void setValidator(Validator<? super T> validator) {
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

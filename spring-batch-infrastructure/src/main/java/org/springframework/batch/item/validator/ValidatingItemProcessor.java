/*
 * Copyright 2006-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.validator;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple implementation of {@link ItemProcessor} that validates input and
 * returns it without modifications. Should the given {@link Validator} throw a
 * {@link ValidationException} this processor will re-throw it to indicate the
 * item should be skipped, unless {@link #setFilter(boolean)} is set to
 * <code>true</code>, in which case <code>null</code> will be returned to
 * indicate the item should be filtered.
 * 
 * @author Robert Kasanicky
 */
public class ValidatingItemProcessor<T> implements ItemProcessor<T, T>, InitializingBean {

	private Validator<? super T> validator;

	private boolean filter = false;

	/**
	 * Default constructor
	 */
	public ValidatingItemProcessor() {
	}

	/**
	 * Creates a ValidatingItemProcessor based on the given Validator.
	 *
	 * @param validator the {@link Validator} instance to be used.
	 */
	public ValidatingItemProcessor(Validator<? super T> validator) {
		this.validator = validator;
	}

	/**
	 * Set the validator used to validate each item.
	 * 
	 * @param validator the {@link Validator} instance to be used.
	 */
	public void setValidator(Validator<? super T> validator) {
		this.validator = validator;
	}

	/**
	 * Should the processor filter invalid records instead of skipping them?
	 * 
	 * @param filter if set to {@code true}, items that fail validation are filtered
	 * ({@code null} is returned).  Otherwise, a {@link ValidationException} will be
	 * thrown.
	 */
	public void setFilter(boolean filter) {
		this.filter = filter;
	}

	/**
	 * Validate the item and return it unmodified
	 * 
	 * @return the input item
	 * @throws ValidationException if validation fails
	 */
    @Nullable
	@Override
	public T process(T item) throws ValidationException {
		try {
			validator.validate(item);
		}
		catch (ValidationException e) {
			if (filter) {
				return null; // filter the item
			}
			else {
				throw e; // skip the item
			}
		}
		return item;
	}

    @Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(validator, "Validator must not be null.");
	}

}

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

import java.util.Collection;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

/**
 * Adapts the {@link org.springframework.validation.Validator} interface to
 * {@link org.springframework.batch.item.validator.Validator}.
 * 
 * @author Tomas Slanina
 * @author Robert Kasanicky
 */
public class SpringValidator<T> implements Validator<T>, InitializingBean {

	private org.springframework.validation.Validator validator;

	/**
	 * @see Validator#validate(Object)
	 */
	public void validate(T item) throws ValidationException {

		if (!validator.supports(item.getClass())) {
			throw new ValidationException("Validation failed for " + item + ": " + item.getClass().getName()
					+ " class is not supported by validator.");
		}

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(item, "item");

		validator.validate(item, errors);

		if (errors.hasErrors()) {
			throw new ValidationException("Validation failed for " + item + ": " + errorsToString(errors), new BindException(errors));
		}
	}

	/**
	 * @return string of field errors followed by global errors.
	 */
	private String errorsToString(Errors errors) {
		StringBuffer builder = new StringBuffer();

		appendCollection(errors.getFieldErrors(), builder);
		appendCollection(errors.getGlobalErrors(), builder);

		return builder.toString();
	}

	/**
	 * Append the string representation of elements of the collection (separated
	 * by new lines) to the given StringBuilder.
	 */
	private void appendCollection(Collection<?> collection, StringBuffer builder) {
		for (Object value : collection) {
			builder.append("\n");
			builder.append(value.toString());
		}
	}

	public void setValidator(org.springframework.validation.Validator validator) {
		this.validator = validator;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(validator, "validator must be set");

	}
}

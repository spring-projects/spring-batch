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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

/**
 * Adapter for the spring validator interface.
 * 
 * @see Validator
 */
public class SpringValidator implements Validator {
	private static final Log log = LogFactory.getLog(SpringValidator.class);

	private org.springframework.validation.Validator validator;

	/**
	 * @see Validator#validate(Object)
	 */
	public void validate(Object value) throws ValidationException {
		if (validator == null) {
			throw new ValidationException("Validator not specified.");
		}

		BeanPropertyBindingResult errors = new BeanPropertyBindingResult(value, "object");

		if (validator.supports(value.getClass())) {
			validator.validate(value, errors);
		}
		else {
			throw new ValidationException(value.getClass() + " is not supported by validator.");
		}

		if (errors.hasErrors()) {
			log.debug(errors);
			throw new ValidationException("SpringValidator >> validation failed on: " + getInvalidColumnNames(errors));
		}
	}

	public void setValidator(org.springframework.validation.Validator validator) {
		this.validator = validator;
	}

	private String getInvalidColumnNames(BeanPropertyBindingResult errors) {
		StringBuffer stringBuffer = new StringBuffer();
		List list = errors.getFieldErrors();

		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				stringBuffer.append(", ");
			}

			stringBuffer.append(((FieldError) list.get(i)).getField());
		}

		return stringBuffer.toString();
	}
}

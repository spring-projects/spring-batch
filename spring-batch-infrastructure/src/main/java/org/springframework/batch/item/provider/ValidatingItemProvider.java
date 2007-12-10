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
package org.springframework.batch.item.provider;

import org.springframework.batch.item.validator.Validator;
import org.springframework.util.Assert;

/**
 * Simple extension of InputsourceItemProvider that provides for
 * validation before returning input.
 *
 * @author Lucas Ward
 *
 */
public class ValidatingItemProvider extends InputSourceItemProvider {

	private Validator validator;

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.provider.InputSourceItemProvider#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(validator, "Validator must not be null.");
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.provider.InputSourceItemProvider#next()
	 */
	public Object next() {
		Object input = super.next();
		if(input != null){
			validator.validate(input);
		}
		return input;
	}

	/**
	 * Set the validator used to validate each item.
	 *
	 * @param validator
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}
}

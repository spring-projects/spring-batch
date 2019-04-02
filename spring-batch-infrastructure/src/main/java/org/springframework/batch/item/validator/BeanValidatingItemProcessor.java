/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.validator;

import javax.validation.Validator;

import org.springframework.util.Assert;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

/**
 * A {@link ValidatingItemProcessor} that uses the Bean Validation API (JSR-303)
 * to validate items.
 *
 * @param <T> type of items to validate
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class BeanValidatingItemProcessor<T> extends ValidatingItemProcessor<T> {

	private Validator validator;

	/**
	 * Create a new instance of {@link BeanValidatingItemProcessor} with the
	 * default configuration.
	 */
	public BeanValidatingItemProcessor() {
		LocalValidatorFactoryBean localValidatorFactoryBean = new LocalValidatorFactoryBean();
		localValidatorFactoryBean.afterPropertiesSet();
		this.validator = localValidatorFactoryBean.getValidator();
	}

	/**
	 * Create a new instance of {@link BeanValidatingItemProcessor}.
	 * @param localValidatorFactoryBean used to configure the Bean Validation validator
	 */
	public BeanValidatingItemProcessor(LocalValidatorFactoryBean localValidatorFactoryBean) {
		Assert.notNull(localValidatorFactoryBean, "localValidatorFactoryBean must not be null");
		this.validator = localValidatorFactoryBean.getValidator();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SpringValidatorAdapter springValidatorAdapter = new SpringValidatorAdapter(this.validator);
		SpringValidator<T> springValidator = new SpringValidator<>();
		springValidator.setValidator(springValidatorAdapter);
		springValidator.afterPropertiesSet();
		setValidator(springValidator);
		super.afterPropertiesSet();
	}
}

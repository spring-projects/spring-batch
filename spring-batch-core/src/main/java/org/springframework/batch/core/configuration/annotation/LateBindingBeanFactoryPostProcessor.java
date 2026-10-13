/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Bean factory post processor that configures the autowire candidate resolver that
 * supports the annotations used to late bind values to job and step scoped beans, such as
 * {@link JobParameter}.
 *
 * @author Yanming Zhou
 * @author Mahmoud Ben Hassine
 * @since 6.1
 */
public class LateBindingBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof DefaultListableBeanFactory dlbf) {
			dlbf.setAutowireCandidateResolver(
					new LateBindingAutowireCandidateResolver(dlbf.getAutowireCandidateResolver()));
		}
	}

}

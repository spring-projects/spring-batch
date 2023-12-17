/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

public class BatchBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private static final String JOB_REGISTRY = "jobRegistry";

	private static final String BEAN_POST_PROCESSOR = "jobRegistryBeanPostProcessor";

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(JOB_REGISTRY)) {
			var beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(MapJobRegistry.class)
				.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
				.getBeanDefinition();
			registry.registerBeanDefinition(JOB_REGISTRY, beanDefinition);
		}

		if (!registry.containsBeanDefinition(BEAN_POST_PROCESSOR)) {
			var beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(JobRegistryBeanPostProcessor.class)
				.addPropertyReference(JOB_REGISTRY, JOB_REGISTRY)
				.getBeanDefinition();
			registry.registerBeanDefinition(BEAN_POST_PROCESSOR, beanDefinition);
		}
	}

}

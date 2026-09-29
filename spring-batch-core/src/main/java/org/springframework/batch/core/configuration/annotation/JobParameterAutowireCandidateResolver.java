/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.core.MethodParameter;

/**
 * {@link AutowireCandidateResolver} implementation that matches bean definition
 * qualifiers against {@link JobParameter} on the field or parameter to be autowired.
 *
 * @author Yanming Zhou
 * @since 6.1
 * @see JobParameter
 */
class JobParameterAutowireCandidateResolver implements AutowireCandidateResolver {

	private final AutowireCandidateResolver delegate;

	public JobParameterAutowireCandidateResolver(AutowireCandidateResolver delegate) {
		this.delegate = delegate;
	}

	@Override
	public @Nullable Object getSuggestedValue(DependencyDescriptor descriptor) {
		JobParameter jobParameter = descriptor.getAnnotation(JobParameter.class);
		if (jobParameter == null) {
			MethodParameter methodParam = descriptor.getMethodParameter();
			if (methodParam != null) {
				jobParameter = methodParam.getParameterAnnotation(JobParameter.class);
			}
		}
		if (jobParameter != null) {
			String key = jobParameter.value();
			if (key.isEmpty()) {
				key = descriptor.getDependencyName();
				if (key == null) {
					throw new IllegalStateException(
							"Value of @JobParameter on method parameter is not specified, and parameter name information not available via reflection. Ensure that the compiler uses the '-parameters' flag.");
				}
			}
			return String.format("#{jobParameters['%s']}", key);
		}

		return delegate.getSuggestedValue(descriptor);
	}

	@Override
	public AutowireCandidateResolver cloneIfNecessary() {
		return new JobParameterAutowireCandidateResolver(this.delegate.cloneIfNecessary());
	}

	@Override
	public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
		return delegate.isAutowireCandidate(bdHolder, descriptor);
	}

	@Override
	public boolean isRequired(DependencyDescriptor descriptor) {
		return delegate.isRequired(descriptor);
	}

	@Override
	public boolean hasQualifier(DependencyDescriptor descriptor) {
		return delegate.hasQualifier(descriptor);
	}

	@Override
	public @Nullable String getSuggestedName(DependencyDescriptor descriptor) {
		return delegate.getSuggestedName(descriptor);
	}

	@Override
	public @Nullable Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor,
			@Nullable String beanName) {
		return delegate.getLazyResolutionProxyIfNecessary(descriptor, beanName);
	}

	@Override
	public @Nullable Class<?> getLazyResolutionProxyClass(DependencyDescriptor descriptor, @Nullable String beanName) {
		return delegate.getLazyResolutionProxyClass(descriptor, beanName);
	}

}

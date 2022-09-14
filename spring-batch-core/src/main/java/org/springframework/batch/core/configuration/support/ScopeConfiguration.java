/*
 * Copyright 2021-2022 the original author or authors.
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

import org.springframework.batch.core.scope.JobScope;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code Configuration} class that provides {@link StepScope} and {@link JobScope}.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
@Configuration(proxyBeanMethods = false)
public class ScopeConfiguration {

	private static StepScope stepScope;

	private static JobScope jobScope;

	static {
		jobScope = new JobScope();
		jobScope.setAutoProxy(false);

		stepScope = new StepScope();
		stepScope.setAutoProxy(false);
	}

	/**
	 * @return The instance of {@link StepScope}.
	 */
	@Bean
	public static StepScope stepScope() {
		return stepScope;
	}

	/**
	 * @return The instance of {@link JobScope}.
	 */
	@Bean
	public static JobScope jobScope() {
		return jobScope;
	}

}

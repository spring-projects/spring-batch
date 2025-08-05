/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.batch.core.repository.persistence.converter;

import org.springframework.batch.core.repository.persistence.JobParameter;

/**
 * @author Mahmoud Ben Hassine
 * @since 5.2.0
 */
public class JobParameterConverter {

	public <T> org.springframework.batch.core.job.parameters.JobParameter<T> toJobParameter(JobParameter<T> source) {
		try {
			return new org.springframework.batch.core.job.parameters.JobParameter<>(source.value(),
					(Class<T>) Class.forName(source.type()), source.identifying());
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public <T> JobParameter<T> fromJobParameter(org.springframework.batch.core.job.parameters.JobParameter<T> source) {
		return new JobParameter<>(source.getValue(), source.getType().getName(), source.isIdentifying());
	}

}

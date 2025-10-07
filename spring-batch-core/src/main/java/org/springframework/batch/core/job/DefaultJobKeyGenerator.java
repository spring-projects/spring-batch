/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.job;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

/**
 * Default implementation of the {@link JobKeyGenerator} interface. This implementation
 * provides a single hash value based on the {@link JobParameters} object passed in. Only
 * identifying parameters (as per {@link JobParameter#identifying()}) are used in the
 * calculation of the key.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.2
 */
public class DefaultJobKeyGenerator implements JobKeyGenerator {

	/**
	 * Generates the job key to be used based on the {@link JobParameters} instance
	 * provided.
	 */
	@Override
	public String generateKey(JobParameters source) {

		Assert.notNull(source, "source must not be null");
		Set<JobParameter<?>> parameters = source.parameters();
		StringBuilder stringBuffer = new StringBuilder();
		List<String> keys = parameters.stream().map(JobParameter::name).sorted().toList();
		for (String key : keys) {
			JobParameter<?> jobParameter = source.getParameter(key);
			if (jobParameter != null && jobParameter.identifying()) {
				stringBuffer.append(jobParameter);
			}
		}
		return DigestUtils.md5DigestAsHex(stringBuffer.toString().getBytes(StandardCharsets.UTF_8));
	}

}

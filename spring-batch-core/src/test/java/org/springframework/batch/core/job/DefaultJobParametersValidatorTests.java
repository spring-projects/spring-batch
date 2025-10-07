/*
 * Copyright 2009-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultJobParametersValidatorTests {

	private final DefaultJobParametersValidator validator = new DefaultJobParametersValidator();

	@Test
	void testValidateNull() {
		assertThrows(InvalidJobParametersException.class, () -> validator.validate(null));
	}

	@Test
	void testValidateNoRequiredValues() throws Exception {
		validator.validate(new JobParametersBuilder().addString("name", "foo").toJobParameters());
	}

	@Test
	void testValidateRequiredValues() throws Exception {
		validator.setRequiredKeys(new String[] { "name", "value" });
		validator
			.validate(new JobParametersBuilder().addString("name", "foo").addLong("value", 111L).toJobParameters());
	}

	@Test
	void testValidateRequiredValuesMissing() {
		validator.setRequiredKeys(new String[] { "name", "value" });
		assertThrows(InvalidJobParametersException.class, () -> validator.validate(new JobParameters()));
	}

	@Test
	void testValidateOptionalValues() throws Exception {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.validate(new JobParameters());
	}

	@Test
	void testValidateOptionalWithImplicitRequiredKey() {
		validator.setOptionalKeys(new String[] { "name", "value" });
		JobParameters jobParameters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
		assertDoesNotThrow(() -> validator.validate(jobParameters));
	}

	@Test
	void testValidateOptionalWithExplicitRequiredKey() throws Exception {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.setRequiredKeys(new String[] { "foo" });
		validator.validate(new JobParametersBuilder().addString("foo", "bar").toJobParameters());
	}

	@Test
	void testOptionalValuesAlsoRequired() {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.setRequiredKeys(new String[] { "foo", "value" });
		assertThrows(IllegalStateException.class, validator::afterPropertiesSet);
	}

}

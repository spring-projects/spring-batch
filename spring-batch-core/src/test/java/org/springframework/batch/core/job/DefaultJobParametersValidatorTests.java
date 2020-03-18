/*
 * Copyright 2009 the original author or authors.
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

import org.junit.Test;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

public class DefaultJobParametersValidatorTests {

	private DefaultJobParametersValidator validator = new DefaultJobParametersValidator();

	@Test(expected = JobParametersInvalidException.class)
	public void testValidateNull() throws Exception {
		validator.validate(null);
	}

	@Test
	public void testValidateNoRequiredValues() throws Exception {
		validator.validate(new JobParametersBuilder().addString("name", "foo").toJobParameters());
	}

	@Test
	public void testValidateRequiredValues() throws Exception {
		validator.setRequiredKeys(new String[] { "name", "value" });
		validator
				.validate(new JobParametersBuilder().addString("name", "foo").addLong("value", 111L).toJobParameters());
	}

	@Test(expected = JobParametersInvalidException.class)
	public void testValidateRequiredValuesMissing() throws Exception {
		validator.setRequiredKeys(new String[] { "name", "value" });
		validator.validate(new JobParameters());
	}

	@Test
	public void testValidateOptionalValues() throws Exception {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.validate(new JobParameters());
	}

	@Test(expected = JobParametersInvalidException.class)
	public void testValidateOptionalWithImplicitRequiredKey() throws Exception {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.validate(new JobParametersBuilder().addString("foo", "bar").toJobParameters());
	}

	@Test
	public void testValidateOptionalWithExplicitRequiredKey() throws Exception {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.setRequiredKeys(new String[] { "foo" });
		validator.validate(new JobParametersBuilder().addString("foo", "bar").toJobParameters());
	}

	@Test(expected = IllegalStateException.class)
	public void testOptionalValuesAlsoRequired() throws Exception {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.setRequiredKeys(new String[] { "foo", "value" });
		validator.afterPropertiesSet();
	}

}

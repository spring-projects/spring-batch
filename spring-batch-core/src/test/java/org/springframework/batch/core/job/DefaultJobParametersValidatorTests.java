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

	@Test(expected = IllegalStateException.class)
	public void testOptionalValuesAlsoRequired() throws Exception {
		validator.setOptionalKeys(new String[] { "name", "value" });
		validator.setRequiredKeys(new String[] { "foo", "value" });
		validator.afterPropertiesSet();
	}

}

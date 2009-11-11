package org.springframework.batch.core.job;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link JobParametersValidator}.
 * 
 * @author Dave Syer
 * 
 */
public class DefaultJobParametersValidator implements JobParametersValidator, InitializingBean {

	private Collection<String> requiredKeys = new HashSet<String>();

	private Collection<String> optionalKeys = new HashSet<String>();

	/**
	 * Check that there are no overlaps between required and optional keys.
	 * @throws IllegalStateException if there is an overlap
	 */
	public void afterPropertiesSet() throws IllegalStateException {
		for (String key : requiredKeys) {
			Assert.state(!optionalKeys.contains(key), "Optional keys canot be required: "+key);
		}
	}

	/**
	 * Check the parameters meet the specification provided.
	 * 
	 * @see JobParametersValidator#validate(JobParameters)
	 */
	public void validate(JobParameters parameters) throws JobParametersInvalidException {

		if (parameters == null) {
			throw new JobParametersInvalidException("The JobParameters can not be null");
		}

		Collection<String> missingKeys = new HashSet<String>();
		for (String key : requiredKeys) {
			if (!parameters.getParameters().containsKey(key)) {
				missingKeys.add(key);
			}
		}
		if (!missingKeys.isEmpty()) {
			throw new JobParametersInvalidException("The JobParameters do not contain required keys: " + missingKeys);
		}

	}

	/**
	 * The keys that are required in the parameters.
	 * 
	 * @param requiredKeys the required key values
	 */
	public void setRequiredKeys(String[] requiredKeys) {
		this.requiredKeys = new HashSet<String>(Arrays.asList(requiredKeys));
	}

	/**
	 * The keys that are optional in the parameters.
	 * 
	 * @param optionalKeys the optional key values
	 */
	public void setOptionalKeys(String[] optionalKeys) {
		this.optionalKeys = new HashSet<String>(Arrays.asList(optionalKeys));
	}

}

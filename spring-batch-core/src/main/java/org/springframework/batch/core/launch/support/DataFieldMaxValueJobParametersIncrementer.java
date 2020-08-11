/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * This incrementer uses a {@link DataFieldMaxValueIncrementer} to generate
 * the sequence of values to use as job instance discriminator.
 * 
 * @author Gregory D. Hopkins
 * @author Mahmoud Ben Hassine
 */
public class DataFieldMaxValueJobParametersIncrementer implements JobParametersIncrementer {

	/**
	 * Default key used as a job parameter.
	 */
	public static final String DEFAULT_KEY = "run.id";

	private String key = DEFAULT_KEY;
	private DataFieldMaxValueIncrementer dataFieldMaxValueIncrementer;

	/**
	 * Create a new {@link DataFieldMaxValueJobParametersIncrementer}.
	 * 
	 * @param dataFieldMaxValueIncrementer the incrementer to use to generate
	 * the sequence of values. Must not be {@code null}.
	 */
	public DataFieldMaxValueJobParametersIncrementer(DataFieldMaxValueIncrementer dataFieldMaxValueIncrementer) {
		Assert.notNull(dataFieldMaxValueIncrementer, "dataFieldMaxValueIncrementer must not be null");
		this.dataFieldMaxValueIncrementer = dataFieldMaxValueIncrementer;
	}

	@Override
	public JobParameters getNext(JobParameters jobParameters) {
		return new JobParametersBuilder(jobParameters == null ? new JobParameters() : jobParameters)
				.addLong(this.key, this.dataFieldMaxValueIncrementer.nextLongValue())
				.toJobParameters();
	}

	/**
	 * Get the key. Defaults to {@link #DEFAULT_KEY}.
	 * 
	 * @return the key
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * The name of the key to use as a job parameter. Defaults to {@link #DEFAULT_KEY}.
	 * Must not be {@code null} or empty.
	 *
	 * @param key the key to set
	 */
	public void setKey(String key) {
		Assert.hasText(key, "key must not be null or empty");
		this.key = key;
	}

	/**
	 * Get the incrementer.
	 * 
	 * @return the incrementer
	 */
	public DataFieldMaxValueIncrementer getDataFieldMaxValueIncrementer() {
		return this.dataFieldMaxValueIncrementer;
	}

	/**
	 * The incrementer to generate the sequence of values. Must not be {@code null}.
	 *
	 * @param dataFieldMaxValueIncrementer the incrementer to generate the sequence of values
	 */
	public void setDataFieldMaxValueIncrementer(DataFieldMaxValueIncrementer dataFieldMaxValueIncrementer) {
		Assert.notNull(dataFieldMaxValueIncrementer, "dataFieldMaxValueIncrementer must not be null");
		this.dataFieldMaxValueIncrementer = dataFieldMaxValueIncrementer;
	}

}
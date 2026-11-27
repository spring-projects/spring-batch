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

package org.springframework.batch.samples.common;

import java.util.ListIterator;

import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.SerializationUtils;

import javax.sql.DataSource;

/**
 * Database {@link ItemWriter} implementing the process indicator pattern.
 */
public class StagingItemWriter<T> implements StepExecutionListener, ItemWriter<T>, InitializingBean {

	protected static final String NEW = "N";

	protected static final String DONE = "Y";

	private DataFieldMaxValueIncrementer incrementer;

	private StepExecution stepExecution;

	private JdbcClient jdbcClient;

	public void setDataSource(DataSource dataSource) {
		jdbcClient = JdbcClient.create(dataSource);
	}

	/**
	 * Check mandatory properties.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(incrementer, "DataFieldMaxValueIncrementer is required - set the incrementer property in the "
				+ ClassUtils.getShortName(StagingItemWriter.class));
	}

	/**
	 * Setter for the key generator for the staging table.
	 * @param incrementer the {@link DataFieldMaxValueIncrementer} to set
	 */
	public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
		this.incrementer = incrementer;
	}

	/**
	 * Serialize the item to the staging table, and add a NEW processed flag.
	 *
	 * @see ItemWriter#write(Chunk)
	 */
	@Override
	public void write(Chunk<? extends T> chunk) {
		final ListIterator<? extends T> itemIterator = chunk.getItems().listIterator();

		JdbcClient.BatchSpec batch = jdbcClient
			.sql("INSERT into BATCH_STAGING (ID, JOB_ID, VALUE, PROCESSED) values (?,?,?,?)")
			.batch();
		while (itemIterator.hasNext()) {
			batch.entry(entry -> {
				entry.param(1, incrementer.nextLongValue())
					.param(2, stepExecution.getJobExecution().getJobInstance().getId())
					.param(3, SerializationUtils.serialize(itemIterator.next()))
					.param(4, NEW);
			});
		}
		batch.update();
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

}

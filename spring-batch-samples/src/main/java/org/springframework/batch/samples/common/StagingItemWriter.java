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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ListIterator;

import org.springframework.batch.core.ExitStatus;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.SerializationUtils;

/**
 * Database {@link ItemWriter} implementing the process indicator pattern.
 */
public class StagingItemWriter<T> extends JdbcDaoSupport implements StepExecutionListener, ItemWriter<T> {

	protected static final String NEW = "N";

	protected static final String DONE = "Y";

	private DataFieldMaxValueIncrementer incrementer;

	private StepExecution stepExecution;

	/**
	 * Check mandatory properties.
	 *
	 * @see org.springframework.dao.support.DaoSupport#initDao()
	 */
	@Override
	protected void initDao() throws Exception {
		super.initDao();
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

		getJdbcTemplate().batchUpdate("INSERT into BATCH_STAGING (ID, JOB_ID, VALUE, PROCESSED) values (?,?,?,?)",
				new BatchPreparedStatementSetter() {
					@Override
					public int getBatchSize() {
						return chunk.size();
					}

					@Override
					public void setValues(PreparedStatement ps, int i) throws SQLException {
						Assert.state(itemIterator.nextIndex() == i,
								"Item ordering must be preserved in batch sql update");

						ps.setLong(1, incrementer.nextLongValue());
						ps.setLong(2, stepExecution.getJobExecution().getJobInstanceId());
						ps.setBytes(3, SerializationUtils.serialize(itemIterator.next()));
						ps.setString(4, NEW);
					}
				});
	}

	@Override
	public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

}

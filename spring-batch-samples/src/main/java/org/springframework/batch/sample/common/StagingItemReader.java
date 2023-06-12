/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.sample.common;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Thread-safe database {@link ItemReader} implementing the process indicator pattern.
 *
 * To achieve restartability use together with {@link StagingItemProcessor}.
 */
public class StagingItemReader<T>
		implements ItemReader<ProcessIndicatorItemWrapper<T>>, StepExecutionListener, InitializingBean, DisposableBean {

	private static Log logger = LogFactory.getLog(StagingItemReader.class);

	private StepExecution stepExecution;

	private final Object lock = new Object();

	private volatile boolean initialized = false;

	private volatile Iterator<Long> keys;

	private JdbcOperations jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public void destroy() throws Exception {
		initialized = false;
		keys = null;
	}

	@Override
	public final void afterPropertiesSet() throws Exception {
		Assert.state(jdbcTemplate != null, "You must provide a DataSource.");
	}

	private List<Long> retrieveKeys() {

		synchronized (lock) {

			return jdbcTemplate.query(

					"SELECT ID FROM BATCH_STAGING WHERE JOB_ID=? AND PROCESSED=? ORDER BY ID",

					(rs, rowNum) -> rs.getLong(1),

					stepExecution.getJobExecution().getJobId(), StagingItemWriter.NEW);

		}

	}

	@Nullable
	@Override
	public ProcessIndicatorItemWrapper<T> read() {
		if (!initialized) {
			throw new ReaderNotOpenException("Reader must be open before it can be used.");
		}

		Long id = null;
		synchronized (lock) {
			if (keys.hasNext()) {
				id = keys.next();
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Retrieved key from list: " + id);
		}
		if (id == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		T result = (T) jdbcTemplate.queryForObject("SELECT VALUE FROM BATCH_STAGING WHERE ID=?",
				(rs, rowNum) -> deserialize(rs.getBinaryStream(1)), id);

		return new ProcessIndicatorItemWrapper<>(id, result);
	}

	private static Object deserialize(InputStream inputStream) {
		if (inputStream == null) {
			return null;
		}
		try (var objectInputStream = new ObjectInputStream(inputStream)) {
			return objectInputStream.readObject();
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Failed to deserialize object", e);
		}
	}

	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
		synchronized (lock) {
			if (keys == null) {
				keys = retrieveKeys().iterator();
				logger.info("Keys obtained for staging.");
				initialized = true;
			}
		}
	}

}

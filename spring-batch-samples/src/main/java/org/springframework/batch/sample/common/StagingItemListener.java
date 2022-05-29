/*
 * Copyright 2006-2012 the original author or authors.
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

import javax.sql.DataSource;

import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

/**
 * Thread-safe database {@link ItemReader} implementing the process indicator pattern.
 */
public class StagingItemListener extends StepListenerSupport<Long, Long> implements InitializingBean {

	private JdbcOperations jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public final void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "You must provide a DataSource.");
	}

	@Override
	public void afterRead(Long id) {
		int count = jdbcTemplate.update("UPDATE BATCH_STAGING SET PROCESSED=? WHERE ID=? AND PROCESSED=?",
				StagingItemWriter.DONE, id, StagingItemWriter.NEW);
		if (count != 1) {
			throw new OptimisticLockingFailureException("The staging record with ID=" + id
					+ " was updated concurrently when trying to mark as complete (updated " + count + " records.");
		}
	}

}

/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.core.jsr;

import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.batch.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Provides default conversion methodology for JSR-352's implementation.
 *
 * Since Spring Batch uses job parameters as a way of identifying a job
 * instance, this converter will add an additional identifying parameter if
 * it does not exist already in the list.  The id for the identifying parameter
 * will come from the JOB_SEQ sequence as used to generate the unique ids
 * for BATCH_JOB_INSTANCE records.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class JsrJobParametersConverter implements JobParametersConverter, InitializingBean {

	public static final String JOB_RUN_ID = "jsr_batch_run_id";
	public DataFieldMaxValueIncrementer incrementer;
	public String tablePrefix = AbstractJdbcBatchMetadataDao.DEFAULT_TABLE_PREFIX;
	public DataSource dataSource;

	/**
	 * Main constructor.
	 *
	 * @param dataSource used to gain access to the database to get unique ids.
	 */
	public JsrJobParametersConverter(DataSource dataSource) {
		Assert.notNull(dataSource, "A DataSource is required");
		this.dataSource = dataSource;
	}

	/**
	 * The table prefix used in the current {@link JobRepository}
	 *
	 * @param tablePrefix the table prefix used for the job repository tables
	 */
	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		DataFieldMaxValueIncrementerFactory factory = new DefaultDataFieldMaxValueIncrementerFactory(dataSource);

		this.incrementer = factory.getIncrementer(DatabaseType.fromMetaData(dataSource).name(), tablePrefix + "JOB_SEQ");
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getJobParameters(java.util.Properties)
	 */
	@Override
	public JobParameters getJobParameters(@Nullable Properties properties) {
		JobParametersBuilder builder = new JobParametersBuilder();
		boolean runIdFound = false;

		if(properties != null) {
			for (Map.Entry<Object, Object> curParameter : properties.entrySet()) {
				if(curParameter.getValue() != null) {
					if(curParameter.getKey().equals(JOB_RUN_ID)) {
						runIdFound = true;
						builder.addLong(curParameter.getKey().toString(), Long.valueOf((String) curParameter.getValue()), true);
					} else {
						builder.addString(curParameter.getKey().toString(), curParameter.getValue().toString(), false);
					}
				}
			}
		}

		if(!runIdFound) {
			builder.addLong(JOB_RUN_ID, incrementer.nextLongValue());
		}

		return builder.toJobParameters();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getProperties(org.springframework.batch.core.JobParameters)
	 */
	@Override
	public Properties getProperties(@Nullable JobParameters params) {
		Properties properties = new Properties();
		boolean runIdFound = false;

		if(params != null) {
			for(Map.Entry<String, JobParameter> curParameter: params.getParameters().entrySet()) {
				if(curParameter.getKey().equals(JOB_RUN_ID)) {
					runIdFound = true;
				}

				properties.setProperty(curParameter.getKey(), curParameter.getValue().getValue().toString());
			}
		}

		if(!runIdFound) {
			properties.setProperty(JOB_RUN_ID, String.valueOf(incrementer.nextLongValue()));
		}

		return properties;
	}
}

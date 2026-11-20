/*
 * Copyright 2006-present the original author or authors.
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

package org.springframework.batch.core.repository.dao.jdbc;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.repository.dao.AbstractJdbcBatchMetadataDao;
import org.springframework.batch.core.repository.dao.JobParameterDao;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.util.ClassUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * JDBC implementation of {@link JobParameterDao}.<br>
 *
 * Allows managing job parameters stored in the batch metadata tables.<br>
 *
 * Provides operations to retrieve, insert, and delete job parameters associated with job
 * executions. Parameter values are converted between their stored string representation
 * and their corresponding Java types using the configured conversion service.<br>
 *
 * @author Moad Elfatihi
 * @see JobParameterDao
 */
public class JdbcJobParameterDao extends AbstractJdbcBatchMetadataDao implements JobParameterDao, InitializingBean {

	private static final String FIND_PARAMS_FROM_ID = """
			SELECT JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING
			FROM %PREFIX%JOB_EXECUTION_PARAMS
			WHERE JOB_EXECUTION_ID = :jobExecutionId
			""";

	private static final String FIND_PARAMS_FROM_IDS = """
			SELECT JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING
			FROM %PREFIX%JOB_EXECUTION_PARAMS
			WHERE JOB_EXECUTION_ID IN (:jobExecutionIds)
			""";

	private static final String CREATE_JOB_PARAMETERS = """
			INSERT INTO %PREFIX%JOB_EXECUTION_PARAMS(JOB_EXECUTION_ID, PARAMETER_NAME, PARAMETER_TYPE, PARAMETER_VALUE, IDENTIFYING)
				VALUES (:jobExecutionId, :parameterName, :parameterType, :parameterValue, :identifying)
			""";

	private static final String DELETE_JOB_EXECUTION_PARAMETERS = """
			DELETE FROM %PREFIX%JOB_EXECUTION_PARAMS
			WHERE JOB_EXECUTION_ID = :jobExecutionId
			""";

	/**
	 * Create a new {@link JdbcJobParameterDao}.
	 * @param jdbcClient the client to use to interact with the batch metadata tables
	 * @since 6.1
	 */
	public JdbcJobParameterDao(JdbcClient jdbcClient) {
		super(jdbcClient);
	}

	@Override
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	public JobParameters getJobParameters(Long executionId) {
		final Set<JobParameter<?>> jobParameters = new HashSet<>();
		RowCallbackHandler handler = rs -> jobParameters.add(mapJobParameter(rs));

		getJdbcClient().sql(getQuery(FIND_PARAMS_FROM_ID)).param("jobExecutionId", executionId).query(handler);

		return new JobParameters(jobParameters);
	}

	@Override
	public Map<Long, JobParameters> getJobParameters(List<Long> executionIds) {
		if (executionIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, Set<JobParameter<?>>> jobParameters = new HashMap<>();
		RowCallbackHandler handler = rs -> jobParameters
			.computeIfAbsent(rs.getLong("JOB_EXECUTION_ID"), executionId -> new HashSet<>())
			.add(mapJobParameter(rs));

		getJdbcClient().sql(getQuery(FIND_PARAMS_FROM_IDS)).param("jobExecutionIds", executionIds).query(handler);

		return jobParameters.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> new JobParameters(entry.getValue())));

	}

	private JobParameter<?> mapJobParameter(ResultSet rs) throws SQLException {
		String parameterName = rs.getString("PARAMETER_NAME");

		Class<?> parameterType;
		try {
			parameterType = ClassUtils.forName(rs.getString("PARAMETER_TYPE"), null);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		String stringValue = rs.getString("PARAMETER_VALUE");
		Object typedValue = getConversionService().convert(stringValue, parameterType);

		boolean identifying = rs.getString("IDENTIFYING").equalsIgnoreCase("Y");

		return new JobParameter(parameterName, typedValue, parameterType, identifying);

	}

	@Override
	public void deleteParametersOf(JobExecution jobExecution) {
		getJdbcClient().sql(getQuery(DELETE_JOB_EXECUTION_PARAMETERS))
			.param("jobExecutionId", jobExecution.getId())
			.update();
	}

	@Override
	public void insertJobParameters(long executionId, JobParameters jobParameters) {
		if (jobParameters.isEmpty()) {
			return;
		}
		JdbcClient.BatchSpec batch = getJdbcClient().sql(getQuery(CREATE_JOB_PARAMETERS)).batch();
		for (JobParameter<?> jobParameter : jobParameters) {
			batch.entry(entry -> {
				entry.param("jobExecutionId", executionId);
				entry.param("parameterName", jobParameter.name());
				entry.param("parameterType", jobParameter.type().getName());
				entry.param("parameterValue", getConversionService().convert(jobParameter.value(), String.class));
				entry.param("identifying", jobParameter.identifying() ? "Y" : "N");
			});
		}
		batch.update();
	}

}

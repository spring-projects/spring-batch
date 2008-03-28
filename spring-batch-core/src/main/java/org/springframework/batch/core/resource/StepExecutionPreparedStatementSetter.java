/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.resource;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link PreparedStatementSetter} interface that also implements
 * {@link StepExecutionListener} and uses {@link JobParameters} to set the parameters on a 
 * PreparedStatement.
 * 
 * @author Lucas Ward
 *
 */
public class StepExecutionPreparedStatementSetter extends StepExecutionListenerSupport implements
		PreparedStatementSetter, InitializingBean {

	private List parameterKeys;
	private JobParameters jobParameters;
	
	public void setValues(PreparedStatement ps) throws SQLException {
		Map parameters = jobParameters.getParameters();
		for(int i = 0; i < parameterKeys.size(); i++){
			Object arg = parameters.get(parameterKeys.get(i));
			if(arg == null){
				throw new IllegalStateException("No job parameter found for with key of: [" + parameterKeys.get(i) + "]");
			}
			StatementCreatorUtils.setParameterValue(ps, i + 1, SqlTypeValue.TYPE_UNKNOWN, arg);
		}
	}
	
	public void beforeStep(StepExecution stepExecution) {
		this.jobParameters = stepExecution.getJobParameters();
	}
	
	/**
	 * The parameter names that will be pulled from the {@link JobParameters}.  It is
	 * assumed that their order in the List is the order of the parameters in the 
	 * PreparedStatement.
	 * 
	 * @return
	 */
	public void setParameterKeys(List parameterKeys) {
		this.parameterKeys = parameterKeys;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(parameterKeys, "Parameters names must be provided");
	}
}

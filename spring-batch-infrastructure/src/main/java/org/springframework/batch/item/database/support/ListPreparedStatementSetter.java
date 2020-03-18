/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.item.database.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.StatementCreatorUtils;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link PreparedStatementSetter} interface that accepts
 * a list of values to be set on a PreparedStatement.  This is usually used in
 * conjunction with the {@link JdbcCursorItemReader} to allow for the replacement
 * of bind variables when generating the cursor.  The order of the list will be
 * used to determine the ordering of setting variables.  For example, the first
 * item in the list will be the first bind variable set.  (i.e. it will
 * correspond to the first '?' in the SQL statement)
 *
 * @deprecated use {@link org.springframework.jdbc.core.ArgumentPreparedStatementSetter}
 * instead.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 */
@Deprecated
public class ListPreparedStatementSetter implements
PreparedStatementSetter, InitializingBean {

	private List<?> parameters;

	public ListPreparedStatementSetter() {}

	public ListPreparedStatementSetter(List<?> parameters) {
		this.parameters = parameters;
	}

	@Override
	public void setValues(PreparedStatement ps) throws SQLException {
		for (int i = 0; i < parameters.size(); i++) {
			StatementCreatorUtils.setParameterValue(ps, i + 1, SqlTypeValue.TYPE_UNKNOWN, parameters.get(i));
		}
	}

	/**
	 * The parameter values that will be set on the PreparedStatement.
	 * It is assumed that their order in the List is the order of the parameters
	 * in the PreparedStatement.
	 *
	 * @param parameters list containing the parameter values to be used.
	 * @deprecated In favor of the constructor
	 */
	@Deprecated
	public void setParameters(List<?> parameters) {
		this.parameters = parameters;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(parameters, "Parameters must be provided");
	}
}

/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.execution.repository.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * User type object to help Hibernate persist (@link {@link ExitStatus})
 * objects.  The continuable flag is mapped to a String with value Y/N.
 * 
 * @author Dave Syer
 * 
 */
public class ExitStatusUserType extends ImmutableValueUserType {

	/**
	 * Convert a result set to an {@link ExitStatus}.
	 * 
	 * @see org.springframework.batch.execution.repository.dao.ImmutableValueUserType#nullSafeGet(java.sql.ResultSet, java.lang.String[], java.lang.Object)
	 */
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws HibernateException, SQLException {
		boolean continuable = "Y".equals(rs.getString(names[0]));
		String code = rs.getString(names[1]);
		String message = rs.getString(names[2]);
		return new ExitStatus(continuable, code, message);
	}

	/**
	 * Convert the value (as an {@link ExitStatus}) to the columns in the
	 * prepared statement.
	 * 
	 * @see org.springframework.batch.execution.repository.dao.ImmutableValueUserType#nullSafeSet(java.sql.PreparedStatement,
	 *      java.lang.Object, int)
	 */
	public void nullSafeSet(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException {
		ExitStatus status = (ExitStatus) value;
		st.setString(index, status.isContinuable() ? "Y" : "N");
		st.setString(index + 1, status.getExitCode());
		st.setString(index + 2, status.getExitDescription());
	}

	public Class returnedClass() {
		return ExitStatus.class;
	}

	public int[] sqlTypes() {
		return new int[] { Types.CHAR, Types.VARCHAR, Types.VARCHAR };
	}

}

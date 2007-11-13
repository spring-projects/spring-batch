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
import org.springframework.batch.core.domain.BatchStatus;

/**
 * User type object to help Hibernate to persist {@link BatchStatus} objects
 * (just plonking it a String).
 * 
 * @author Dave Syer
 * 
 */
public class BatchStatusUserType extends ImmutableValueUserType {

	/* (non-Javadoc)
	 * @see org.springframework.batch.execution.repository.dao.ImmutableValueUserType#sqlTypes()
	 */
	public int[] sqlTypes() {
		return new int[] { Types.VARCHAR };
	}

	/**
	 * Get the 
	 * @see org.springframework.batch.execution.repository.dao.ImmutableValueUserType#nullSafeGet(java.sql.ResultSet, java.lang.String[], java.lang.Object)
	 */
	public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
			throws HibernateException, SQLException {
		return BatchStatus.getStatus(rs.getString(names[0]));
	}



	/** 
	 * Plonk a String representation of the status in the prepared statement.
	 * 
	 * @see org.springframework.batch.execution.repository.dao.ImmutableValueUserType#nullSafeSet(java.sql.PreparedStatement, java.lang.Object, int)
	 */
	public void nullSafeSet(PreparedStatement st, Object value, int index)
			throws HibernateException, SQLException {
		st.setString(index, value!=null ? value.toString() : null);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.execution.repository.dao.ImmutableValueUserType#returnedClass()
	 */
	public Class returnedClass() {
		return BatchStatus.class;
	}

}

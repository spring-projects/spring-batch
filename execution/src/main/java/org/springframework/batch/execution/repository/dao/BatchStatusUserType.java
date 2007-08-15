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

import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.orm.hibernate3.support.ClobStringType;

/**
 * User type object to help Hibernate to persist {@link BatchStatus} objects
 * (just plonking it a Clob).
 * 
 * @author tomas.slanina
 * 
 */
public class BatchStatusUserType extends ClobStringType {

	/**
	 * Get a {@link BatchStatus} from a Clob.
	 * 
	 * @return a {@link BatchStatus} object whose string representation is the
	 * same as the database value.
	 * 
	 * @see org.springframework.orm.hibernate3.support.ClobStringType#nullSafeGetInternal(java.sql.ResultSet,
	 * java.lang.String[], java.lang.Object,
	 * org.springframework.jdbc.support.lob.LobHandler)
	 */
	protected Object nullSafeGetInternal(ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException {
		String status = (String) super.nullSafeGetInternal(rs, names, owner, lobHandler);
		return BatchStatus.getStatus(status);
	}

	/**
	 * Convert an object to a string and then pop it in a Clob.
	 * 
	 * @see org.springframework.orm.hibernate3.support.ClobStringType#nullSafeSetInternal(java.sql.PreparedStatement,
	 * int, java.lang.Object, org.springframework.jdbc.support.lob.LobCreator)
	 */
	protected void nullSafeSetInternal(PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException {
		String status = (value == null) ? "" : value.toString();
		super.nullSafeSetInternal(ps, index, status, lobCreator);
	}

}

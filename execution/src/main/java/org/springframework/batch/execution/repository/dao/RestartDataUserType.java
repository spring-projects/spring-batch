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
import java.util.Properties;

import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.orm.hibernate3.support.ClobStringType;

/**
 * User type object to help Hibernate persist (@link RestartData) objects by setting
 * a string in a clob.
 * 
 * @author Lucas Ward
 *
 */
public class RestartDataUserType extends ClobStringType {

	/**
	 * Get a {@link Properties} from a Clob.
	 * 
	 * @return a {@link GenericRestartData} object whose internal properties string representation is the
	 * same as the database value.
	 * 
	 * @see org.springframework.orm.hibernate3.support.ClobStringType#nullSafeGetInternal(java.sql.ResultSet,
	 * java.lang.String[], java.lang.Object,
	 * org.springframework.jdbc.support.lob.LobHandler)
	 */
	protected Object nullSafeGetInternal(ResultSet rs, String[] names, Object owner, LobHandler lobHandler)
			throws SQLException {
		final String value = (String) super.nullSafeGetInternal(rs, names, owner, lobHandler); 
		return new GenericRestartData(PropertiesConverter.stringToProperties(value));
	}

	/**
	 * Convert a {@link RestartData} object to a string and then pop it in a Clob.
	 * 
	 * @see org.springframework.orm.hibernate3.support.ClobStringType#nullSafeSetInternal(java.sql.PreparedStatement, int, java.lang.Object, org.springframework.jdbc.support.lob.LobCreator)
	 */
	protected void nullSafeSetInternal(PreparedStatement ps, int index, Object value, LobCreator lobCreator)
			throws SQLException {
		final RestartData restartData = (RestartData)value;
		String string = (restartData == null) ? "" 
				:PropertiesConverter.propertiesToString(restartData.getProperties());
		super.nullSafeSetInternal(ps, index, string, lobCreator);
	}
}

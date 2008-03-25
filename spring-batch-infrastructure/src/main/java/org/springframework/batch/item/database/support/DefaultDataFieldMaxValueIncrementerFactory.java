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
package org.springframework.batch.item.database.support;

import javax.sql.DataSource;

import org.springframework.jdbc.support.incrementer.DB2SequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgreSQLSequenceMaxValueIncrementer;

/**
 * Default implementation of the {@link DataFieldMaxValueIncrementerFactory} interface. Valid types
 * are:
 * 
 * Valid values are:
 * 
 * <ul>
 * <li>db2</li>
 * <li>derby</li>
 * <li>hsql</li>
 * <li>mysql</li>
 * <li>oracle</li>
 * <li>postgres</li>
 * </ul>
 * 
 * @author Lucas Ward
 * 
 */
public class DefaultDataFieldMaxValueIncrementerFactory implements DataFieldMaxValueIncrementerFactory {

	static final String DB_TYPE_DB2 = "db2";

	static final String DB_TYPE_DERBY = "derby";

	static final String DB_TYPE_HSQL = "hsql";

	static final String DB_TYPE_MYSQL = "mysql";

	static final String DB_TYPE_ORACLE = "oracle";

	static final String DB_TYPE_POSTGRES = "postgres";

	private DataSource dataSource;

	public DefaultDataFieldMaxValueIncrementerFactory(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public DataFieldMaxValueIncrementer getIncrementer(String incrementerType, String incrementerName) {
		if (DB_TYPE_DB2.equals(incrementerType)) {
			return new DB2SequenceMaxValueIncrementer(dataSource, incrementerName);
		} else if (DB_TYPE_DERBY.equals(incrementerType)) {
			return new DerbyMaxValueIncrementer(dataSource, incrementerName, "id");
		} else if (DB_TYPE_HSQL.equals(incrementerType)) {
			return new HsqlMaxValueIncrementer(dataSource, incrementerName, "id");
		} else if (DB_TYPE_MYSQL.equals(incrementerType)) {
			return new MySQLMaxValueIncrementer(dataSource, incrementerName, "id");
		} else if (DB_TYPE_ORACLE.equals(incrementerType)) {
			return new OracleSequenceMaxValueIncrementer(dataSource, incrementerName);
		} else if (DB_TYPE_POSTGRES.equals(incrementerType)) {
			return new PostgreSQLSequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		throw new IllegalArgumentException("databaseType argument was not on the approved list");

	}

	public boolean isSupportedIncrementerType(String incrementerType) {
		if (!DB_TYPE_DB2.equals(incrementerType) && !DB_TYPE_DERBY.equals(incrementerType)
		        && !DB_TYPE_HSQL.equals(incrementerType) && !DB_TYPE_MYSQL.equals(incrementerType)
		        && !DB_TYPE_ORACLE.equals(incrementerType) && !DB_TYPE_POSTGRES.equals(incrementerType)) {

			return false;
		} else {
			return true;
		}
	}

	public String[] getSupportedIncrementerTypes() {
		return new String[] { DB_TYPE_DB2, DB_TYPE_DERBY, DB_TYPE_HSQL, DB_TYPE_MYSQL, DB_TYPE_ORACLE, DB_TYPE_POSTGRES };
	}
}

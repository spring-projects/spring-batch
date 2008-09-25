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

import static org.springframework.batch.support.DatabaseType.*;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.support.DatabaseType;
import org.springframework.jdbc.support.incrementer.DB2SequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgreSQLSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SqlServerMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SybaseMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DB2MainframeSequenceMaxValueIncrementer;

/**
 * Default implementation of the {@link DataFieldMaxValueIncrementerFactory}
 * interface. Valid types are:
 * 
 * Valid values are:
 * 
 * <ul>
 * <li>db2</li>
 * <li>db2zos</li>
 * <li>derby</li>
 * <li>hsql</li>
 * <li>mysql</li>
 * <li>oracle</li>
 * <li>postgres</li>
 * <li>sqlserver</li>
 * <li>sybase</li>
 * </ul>
 * 
 * @author Lucas Ward
 * @see DatabaseType
 */
public class DefaultDataFieldMaxValueIncrementerFactory implements DataFieldMaxValueIncrementerFactory {

	private DataSource dataSource;

	private String incrementerColumnName = "ID";

	/**
	 * Public setter for the column name (defaults to "ID") in the incrementer.
	 * Only used by some platforms (Derby, HSQL, MySQL, SQL Server and Sybase),
	 * and should be fine for use with Spring Batch meta data as long as the default
	 * batch schema hasn't been changed.
	 * 
	 * @param incrementerColumnName the primary key column name to set
	 */
	public void setIncrementerColumnName(String incrementerColumnName) {
		this.incrementerColumnName = incrementerColumnName;
	}

	public DefaultDataFieldMaxValueIncrementerFactory(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public DataFieldMaxValueIncrementer getIncrementer(String incrementerType, String incrementerName) {
		DatabaseType databaseType = DatabaseType.valueOf(incrementerType.toUpperCase());
		
		if (databaseType == DB2) {
			return new DB2SequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == DB2ZOS) {
			return new DB2MainframeSequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == DERBY) {
			return new DerbyMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == HSQL) {
			return new HsqlMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == MYSQL) {
			return new MySQLMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == ORACLE) {
			return new OracleSequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == POSTGRES) {
			return new PostgreSQLSequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == SQLSERVER) {
			return new SqlServerMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == SYBASE) {
			return new SybaseMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		throw new IllegalArgumentException("databaseType argument was not on the approved list");

	}

	public boolean isSupportedIncrementerType(String incrementerType) {
		for(DatabaseType type : DatabaseType.values()){
			if(type.name().equals(incrementerType.toUpperCase())){
				return true;
			}
		}
		
		return false;
	}

	public String[] getSupportedIncrementerTypes() {
		
		List<String> types = new ArrayList<String>();

		for(DatabaseType type : DatabaseType.values()){
			types.add(type.name());
		}

		return types.toArray(new String[types.size()]);
	}
}

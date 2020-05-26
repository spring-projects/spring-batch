/*
 * Copyright 2006-2018 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.batch.support.DatabaseType;
import org.springframework.jdbc.support.incrementer.Db2LuwMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.Db2MainframeMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.DerbyMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.H2SequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SqlServerMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.SybaseMaxValueIncrementer;

import static org.springframework.batch.support.DatabaseType.DB2;
import static org.springframework.batch.support.DatabaseType.DB2AS400;
import static org.springframework.batch.support.DatabaseType.DB2ZOS;
import static org.springframework.batch.support.DatabaseType.DERBY;
import static org.springframework.batch.support.DatabaseType.H2;
import static org.springframework.batch.support.DatabaseType.HSQL;
import static org.springframework.batch.support.DatabaseType.MYSQL;
import static org.springframework.batch.support.DatabaseType.ORACLE;
import static org.springframework.batch.support.DatabaseType.POSTGRES;
import static org.springframework.batch.support.DatabaseType.SQLITE;
import static org.springframework.batch.support.DatabaseType.SQLSERVER;
import static org.springframework.batch.support.DatabaseType.SYBASE;

/**
 * Default implementation of the {@link DataFieldMaxValueIncrementerFactory}
 * interface. Valid database types are given by the {@link DatabaseType} enum.
 *
 * Note: For MySql databases, the
 * {@link MySQLMaxValueIncrementer#setUseNewConnection(boolean)} will be set to true.
 * 
 * @author Lucas Ward
 * @author Michael Minella
 * @author Drummond Dawson
 * @see DatabaseType
 */
public class DefaultDataFieldMaxValueIncrementerFactory implements DataFieldMaxValueIncrementerFactory {

	private DataSource dataSource;

	private String incrementerColumnName = "ID";

	/**
	 * Public setter for the column name (defaults to "ID") in the incrementer.
	 * Only used by some platforms (Derby, HSQL, MySQL, SQL Server and Sybase),
	 * and should be fine for use with Spring Batch meta data as long as the
	 * default batch schema hasn't been changed.
	 * 
	 * @param incrementerColumnName the primary key column name to set
	 */
	public void setIncrementerColumnName(String incrementerColumnName) {
		this.incrementerColumnName = incrementerColumnName;
	}

	public DefaultDataFieldMaxValueIncrementerFactory(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public DataFieldMaxValueIncrementer getIncrementer(String incrementerType, String incrementerName) {
		DatabaseType databaseType = DatabaseType.valueOf(incrementerType.toUpperCase());

		if (databaseType == DB2 || databaseType == DB2AS400) {
			return new Db2LuwMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == DB2ZOS) {
			return new Db2MainframeMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == DERBY) {
			return new DerbyMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == HSQL) {
			return new HsqlMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == H2) {
			return new H2SequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == MYSQL) {
			MySQLMaxValueIncrementer mySQLMaxValueIncrementer = new MySQLMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
			mySQLMaxValueIncrementer.setUseNewConnection(true);
			return mySQLMaxValueIncrementer;
		}
		else if (databaseType == ORACLE) {
			return new OracleSequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == POSTGRES) {
			return new PostgresSequenceMaxValueIncrementer(dataSource, incrementerName);
		}
		else if (databaseType == SQLITE) {
			return new SqliteMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == SQLSERVER) {
			return new SqlServerMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		else if (databaseType == SYBASE) {
			return new SybaseMaxValueIncrementer(dataSource, incrementerName, incrementerColumnName);
		}
		throw new IllegalArgumentException("databaseType argument was not on the approved list");
	}
	
    @Override
	public boolean isSupportedIncrementerType(String incrementerType) {
		for (DatabaseType type : DatabaseType.values()) {
			if (type.name().equalsIgnoreCase(incrementerType)) {
				return true;
			}
		}

		return false;
	}

    @Override
	public String[] getSupportedIncrementerTypes() {

		List<String> types = new ArrayList<>();

		for (DatabaseType type : DatabaseType.values()) {
			types.add(type.name());
		}

		return types.toArray(new String[types.size()]);
	}
}
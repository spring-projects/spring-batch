/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.support;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;


/**
 * Enum representing a database type, such as DB2 or oracle.  The type also
 * contains a product name, which is expected to be the same as the product name
 * provided by the database driver's metadata.
 *
 * @author Lucas Ward
 * @since 2.0
 */
public enum DatabaseType {

	DERBY("Apache Derby"),
	DB2("DB2"),
	DB2VSE("DB2VSE"),
	DB2ZOS("DB2ZOS"),
	DB2AS400("DB2AS400"),
	HSQL("HSQL Database Engine"),
	SQLSERVER("Microsoft SQL Server"),
	MYSQL("MySQL"),
	ORACLE("Oracle"),
	POSTGRES("PostgreSQL"),
	SYBASE("Sybase"),
	H2("H2"),
	SQLITE("SQLite");

	private static final Map<String, DatabaseType> nameMap;

	static{
		nameMap = new HashMap<>();
		for(DatabaseType type: values()){
			nameMap.put(type.getProductName(), type);
		}
	}
	//A description is necessary due to the nature of database descriptions
	//in metadata.
	private final String productName;

	private DatabaseType(String productName) {
		this.productName = productName;
	}

	public String getProductName() {
		return productName;
	}

	/**
	 * Static method to obtain a DatabaseType from the provided product name.
	 *
	 * @param productName {@link String} containing the product name.
	 * @return the {@link DatabaseType} for given product name.
	 *
	 * @throws IllegalArgumentException if none is found.
	 */
	public static DatabaseType fromProductName(String productName){
		if(productName.equals("MariaDB"))
			productName = "MySQL";
		if(!nameMap.containsKey(productName)){
			throw new IllegalArgumentException("DatabaseType not found for product name: [" +
					productName + "]");
		}
		else{
			return nameMap.get(productName);
		}
	}

	/**
	 * Convenience method that pulls a database product name from the DataSource's metadata.
	 *
	 * @param dataSource {@link DataSource} to the database to be used.
	 * @return {@link DatabaseType} for the {@link DataSource} specified.
	 *
	 * @throws MetaDataAccessException thrown if error occured during Metadata lookup.
	 */
	public static DatabaseType fromMetaData(DataSource dataSource) throws MetaDataAccessException {
		String databaseProductName =
				JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName").toString();
		if (StringUtils.hasText(databaseProductName) && databaseProductName.startsWith("DB2")) {
			String databaseProductVersion =
					JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductVersion").toString();
			if (databaseProductVersion.startsWith("ARI")) {
				databaseProductName = "DB2VSE";
			}
			else if (databaseProductVersion.startsWith("DSN")) {
				databaseProductName = "DB2ZOS";
			}
			else if (databaseProductName.indexOf("AS") != -1 && (databaseProductVersion.startsWith("QSQ") ||
					databaseProductVersion.substring(databaseProductVersion.indexOf('V')).matches("V\\dR\\d[mM]\\d"))) {
				databaseProductName = "DB2AS400";
			}
			else {
				databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
			}
		}
		else {
			databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
		}
		return fromProductName(databaseProductName);
	}
}

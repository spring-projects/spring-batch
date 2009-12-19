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

package org.springframework.batch.support;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;


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
	DB2ZOS("DB2ZOS"), 
	HSQL("HSQL Database Engine"),
	SQLSERVER("Microsoft SQL Server"),
	MYSQL("MySQL"),
	ORACLE("Oracle"),
	POSTGRES("PostgreSQL"),
	SYBASE("Sybase"), H2("H2");
	
	private static final Map<String, DatabaseType> nameMap;
	
	static{
		nameMap = new HashMap<String, DatabaseType>();
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
	 * @param productName
	 * @return DatabaseType for given product name.
	 * @throws IllegalArgumentException if none is found.
	 */
	public static DatabaseType fromProductName(String productName){
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
	 * @param dataSource
	 * @return DatabaseType
	 * @throws MetaDataAccessException
	 */
	public static DatabaseType fromMetaData(DataSource dataSource) throws MetaDataAccessException {
		String databaseProductName =
				JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName").toString();
		if ("DB2".equals(databaseProductName)) {
			String databaseProductVersion =
					JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductVersion").toString();
			if (!databaseProductVersion.startsWith("SQL")) {
				databaseProductName = "DB2ZOS";
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

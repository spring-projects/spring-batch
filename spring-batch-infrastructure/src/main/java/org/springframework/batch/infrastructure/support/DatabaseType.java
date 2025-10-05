/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.support;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enum representing a database type, such as DB2 or oracle. The type also contains a
 * product name, which is expected to be the same as the product name provided by the
 * database driver's metadata.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @since 2.0
 */
public enum DatabaseType {

	DERBY("Apache Derby"), DB2("DB2"), DB2VSE("DB2VSE"), DB2ZOS("DB2ZOS"), DB2AS400("DB2AS400"),
	HSQL("HSQL Database Engine"), SQLSERVER("Microsoft SQL Server"), MYSQL("MySQL"), ORACLE("Oracle"),
	POSTGRES("PostgreSQL"), SYBASE("Sybase"), H2("H2"), SQLITE("SQLite"), HANA("HDB"), MARIADB("MariaDB");

	private static final Map<String, DatabaseType> DATABASE_TYPES = Arrays.stream(DatabaseType.values())
		.collect(Collectors.toMap(DatabaseType::getProductName, Function.identity()));

	// A description is necessary due to the nature of database descriptions
	// in metadata.
	private final String productName;

	DatabaseType(String productName) {
		this.productName = productName;
	}

	public String getProductName() {
		return productName;
	}

	/**
	 * Static method to obtain a DatabaseType from the provided product name.
	 * @param productName {@link String} containing the product name. Must not be null.
	 * @return the {@link DatabaseType} for given product name.
	 * @throws IllegalArgumentException if none is found.
	 */
	public static DatabaseType fromProductName(String productName) {
		Assert.notNull(productName, "Product name must not be null");
		if (!DATABASE_TYPES.containsKey(productName)) {
			throw new IllegalArgumentException("DatabaseType not found for product name: [" + productName + "]");
		}
		return DATABASE_TYPES.get(productName);
	}

	/**
	 * Convenience method that pulls a database product name from the DataSource's
	 * metadata.
	 * @param dataSource {@link DataSource} to the database to be used.
	 * @return {@link DatabaseType} for the {@link DataSource} specified.
	 * @throws MetaDataAccessException if an error occurred during Metadata lookup.
	 */
	public static DatabaseType fromMetaData(DataSource dataSource) throws MetaDataAccessException {
		String databaseProductName = JdbcUtils.extractDatabaseMetaData(dataSource,
				DatabaseMetaData::getDatabaseProductName);
		if (StringUtils.hasText(databaseProductName) && databaseProductName.startsWith("DB2")) {
			String databaseProductVersion = JdbcUtils.extractDatabaseMetaData(dataSource,
					DatabaseMetaData::getDatabaseProductVersion);
			if (!StringUtils.hasText(databaseProductVersion)) {
				throw new MetaDataAccessException("Database product version not found for " + databaseProductName);
			}
			if (databaseProductVersion.startsWith("ARI")) {
				databaseProductName = "DB2VSE";
			}
			else if (databaseProductVersion.startsWith("DSN")) {
				databaseProductName = "DB2ZOS";
			}
			else if (databaseProductName.contains("AS") && (databaseProductVersion.startsWith("QSQ")
					|| databaseProductVersion.substring(databaseProductVersion.indexOf('V'))
						.matches("V\\dR\\d[mM]\\d"))) {
				databaseProductName = "DB2AS400";
			}
			else {
				databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
			}
		}
		else if (StringUtils.hasText(databaseProductName) && databaseProductName.startsWith("EnterpriseDB")) {
			databaseProductName = "PostgreSQL";
		}
		else {
			databaseProductName = JdbcUtils.commonDatabaseName(databaseProductName);
		}
		if (!StringUtils.hasText(databaseProductName)) {
			throw new MetaDataAccessException("Database product name not found for data source " + dataSource);
		}
		return fromProductName(databaseProductName);
	}

}

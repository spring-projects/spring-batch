/*
 * Copyright 2006-present the original author or authors.
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

	/**
	 * Apache Derby.
	 * @deprecated since 6.1.0 with no replacement. Scheduled for removal in 7.0.0.
	 */
	@Deprecated(since = "6.1.0", forRemoval = true)
	DERBY("Apache Derby", "schema-derby.sql", "schema-drop-derby.sql"),

	DB2("DB2", "schema-db2.sql", "schema-drop-db2.sql"), DB2VSE("DB2VSE", "schema-db2.sql", "schema-drop-db2.sql"),
	DB2ZOS("DB2ZOS", "schema-db2.sql", "schema-drop-db2.sql"),
	DB2AS400("DB2AS400", "schema-db2.sql", "schema-drop-db2.sql"),
	HSQL("HSQL Database Engine", "schema-hsqldb.sql", "schema-drop-hsqldb.sql"),
	SQLSERVER("Microsoft SQL Server", "schema-sqlserver.sql", "schema-drop-sqlserver.sql"),
	MYSQL("MySQL", "schema-mysql.sql", "schema-drop-mysql.sql"),
	ORACLE("Oracle", "schema-oracle.sql", "schema-drop-oracle.sql"),
	POSTGRES("PostgreSQL", "schema-postgresql.sql", "schema-drop-postgresql.sql"),
	SYBASE("Sybase", "schema-sybase.sql", "schema-drop-sybase.sql"), H2("H2", "schema-h2.sql", "schema-drop-h2.sql"),
	SQLITE("SQLite", "schema-sqlite.sql", "schema-drop-sqlite.sql"),
	HANA("HDB", "schema-hana.sql", "schema-drop-hana.sql"),
	MARIADB("MariaDB", "schema-mariadb.sql", "schema-drop-mariadb.sql");

	/**
	 * The package in which the schema scripts for supported databases are located.
	 */
	private static final String SCHEMA_LOCATION = "org/springframework/batch/core/";

	private static final Map<String, DatabaseType> DATABASE_TYPES = Arrays.stream(DatabaseType.values())
		.collect(Collectors.toMap(DatabaseType::getProductName, Function.identity()));

	// A description is necessary due to the nature of database descriptions
	// in metadata.
	private final String productName;

	private final String productSchema;

	private final String productSchemaDrop;

	DatabaseType(String productName, String productSchema, String productSchemaDrop) {
		this.productName = productName;
		this.productSchema = SCHEMA_LOCATION + productSchema;
		this.productSchemaDrop = SCHEMA_LOCATION + productSchemaDrop;
	}

	public String getProductName() {
		return productName;
	}

	/**
	 * Return the classpath location of the schema creation script for this database type.
	 * The returned location does not have a leading {@code /}, so that it can be used
	 * as-is with classpath-relative resource loading APIs (eg {@code ClassPathResource})
	 * as well as with AOT resource hint registration (eg
	 * {@code RuntimeHints.resources().registerPattern(...)}).
	 * @return the classpath location of the schema creation script
	 * @since 6.1
	 */
	public String getProductSchema() {
		return productSchema;
	}

	/**
	 * Return the classpath location of the schema drop script for this database type. The
	 * returned location does not have a leading {@code /}, so that it can be used as-is
	 * with classpath-relative resource loading APIs (eg {@code ClassPathResource}) as
	 * well as with AOT resource hint registration (eg
	 * {@code RuntimeHints.resources().registerPattern(...)}).
	 * @return the classpath location of the schema drop script
	 * @since 6.1
	 */
	public String getProductSchemaDrop() {
		return productSchemaDrop;
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

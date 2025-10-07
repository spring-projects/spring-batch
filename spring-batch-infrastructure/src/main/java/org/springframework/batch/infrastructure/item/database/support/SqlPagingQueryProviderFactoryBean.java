/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.support;

import static org.springframework.batch.infrastructure.support.DatabaseType.DB2;
import static org.springframework.batch.infrastructure.support.DatabaseType.DB2VSE;
import static org.springframework.batch.infrastructure.support.DatabaseType.DB2ZOS;
import static org.springframework.batch.infrastructure.support.DatabaseType.DB2AS400;
import static org.springframework.batch.infrastructure.support.DatabaseType.DERBY;
import static org.springframework.batch.infrastructure.support.DatabaseType.H2;
import static org.springframework.batch.infrastructure.support.DatabaseType.HANA;
import static org.springframework.batch.infrastructure.support.DatabaseType.HSQL;
import static org.springframework.batch.infrastructure.support.DatabaseType.MARIADB;
import static org.springframework.batch.infrastructure.support.DatabaseType.MYSQL;
import static org.springframework.batch.infrastructure.support.DatabaseType.ORACLE;
import static org.springframework.batch.infrastructure.support.DatabaseType.POSTGRES;
import static org.springframework.batch.infrastructure.support.DatabaseType.SQLITE;
import static org.springframework.batch.infrastructure.support.DatabaseType.SQLSERVER;
import static org.springframework.batch.infrastructure.support.DatabaseType.SYBASE;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.PagingQueryProvider;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for {@link PagingQueryProvider} interface. The database type will be
 * determined from the data source if not provided explicitly. Valid types are given by
 * the {@link DatabaseType} enum.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
public class SqlPagingQueryProviderFactoryBean implements FactoryBean<PagingQueryProvider> {

	private @Nullable DataSource dataSource;

	private @Nullable String databaseType;

	private @Nullable String fromClause;

	private @Nullable String whereClause;

	private @Nullable String selectClause;

	private @Nullable String groupClause;

	private @Nullable Map<String, Order> sortKeys;

	private final Map<DatabaseType, AbstractSqlPagingQueryProvider> providers = new HashMap<>();

	{
		providers.put(DB2, new Db2PagingQueryProvider());
		providers.put(DB2VSE, new Db2PagingQueryProvider());
		providers.put(DB2ZOS, new Db2PagingQueryProvider());
		providers.put(DB2AS400, new Db2PagingQueryProvider());
		providers.put(DERBY, new DerbyPagingQueryProvider());
		providers.put(HSQL, new HsqlPagingQueryProvider());
		providers.put(H2, new H2PagingQueryProvider());
		providers.put(HANA, new HanaPagingQueryProvider());
		providers.put(MYSQL, new MySqlPagingQueryProvider());
		providers.put(MARIADB, new MariaDBPagingQueryProvider());
		providers.put(ORACLE, new OraclePagingQueryProvider());
		providers.put(POSTGRES, new PostgresPagingQueryProvider());
		providers.put(SQLITE, new SqlitePagingQueryProvider());
		providers.put(SQLSERVER, new SqlServerPagingQueryProvider());
		providers.put(SYBASE, new SybasePagingQueryProvider());
	}

	/**
	 * @param groupClause SQL GROUP BY clause part of the SQL query string
	 */
	public void setGroupClause(String groupClause) {
		this.groupClause = groupClause;
	}

	/**
	 * @param databaseType the databaseType to set
	 */
	public void setDatabaseType(String databaseType) {
		this.databaseType = databaseType;
	}

	/**
	 * @param dataSource the dataSource to set
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @param fromClause the fromClause to set
	 */
	public void setFromClause(String fromClause) {
		this.fromClause = fromClause;
	}

	/**
	 * @param whereClause the whereClause to set
	 */
	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}

	/**
	 * @param selectClause the selectClause to set
	 */
	public void setSelectClause(String selectClause) {
		this.selectClause = selectClause;
	}

	/**
	 * @param sortKeys the sortKeys to set
	 */
	public void setSortKeys(Map<String, Order> sortKeys) {
		this.sortKeys = sortKeys;
	}

	public void setSortKey(String key) {
		Assert.doesNotContain(key, ",", "String setter is valid for a single ASC key only");
		this.sortKeys = Map.of(key, Order.ASCENDING);
	}

	/**
	 * Get a {@link PagingQueryProvider} instance using the provided properties and
	 * appropriate for the given database type.
	 *
	 * @see FactoryBean#getObject()
	 */
	@SuppressWarnings("DataFlowIssue")
	@Override
	public PagingQueryProvider getObject() throws Exception {

		DatabaseType type;
		try {
			type = databaseType != null ? DatabaseType.valueOf(databaseType.toUpperCase())
					: DatabaseType.fromMetaData(dataSource);
		}
		catch (MetaDataAccessException e) {
			throw new IllegalArgumentException(
					"Could not inspect meta data for database type.  You have to supply it explicitly.", e);
		}

		AbstractSqlPagingQueryProvider provider = providers.get(type);
		Assert.state(provider != null, "Should not happen: missing PagingQueryProvider for DatabaseType=" + type);

		provider.setFromClause(fromClause);
		provider.setWhereClause(whereClause);
		provider.setSortKeys(sortKeys);
		if (StringUtils.hasText(selectClause)) {
			provider.setSelectClause(selectClause);
		}
		if (StringUtils.hasText(groupClause)) {
			provider.setGroupClause(groupClause);
		}

		provider.init(dataSource);

		return provider;

	}

	/**
	 * Always returns {@link PagingQueryProvider}.
	 *
	 * @see FactoryBean#getObjectType()
	 */
	@Override
	public Class<PagingQueryProvider> getObjectType() {
		return PagingQueryProvider.class;
	}

	/**
	 * Always returns true.
	 * @see FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}

}

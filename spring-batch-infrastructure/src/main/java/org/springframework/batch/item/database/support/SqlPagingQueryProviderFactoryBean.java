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

import static org.springframework.batch.support.DatabaseType.DB2;
import static org.springframework.batch.support.DatabaseType.DB2ZOS;
import static org.springframework.batch.support.DatabaseType.DERBY;
import static org.springframework.batch.support.DatabaseType.HSQL;
import static org.springframework.batch.support.DatabaseType.H2;
import static org.springframework.batch.support.DatabaseType.MYSQL;
import static org.springframework.batch.support.DatabaseType.ORACLE;
import static org.springframework.batch.support.DatabaseType.POSTGRES;
import static org.springframework.batch.support.DatabaseType.SQLSERVER;
import static org.springframework.batch.support.DatabaseType.SYBASE;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.support.DatabaseType;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for {@link PagingQueryProvider} interface. The database type
 * will be determined from the data source if not provided explicitly. Valid
 * types are given by the {@link DatabaseType} enum.
 * 
 * @author Dave Syer
 */
public class SqlPagingQueryProviderFactoryBean implements FactoryBean {

	private DataSource dataSource;

	private String databaseType;

	private String fromClause;

	private String whereClause;

	private String selectClause;

	private String sortKey;

	private boolean ascending = true;

	private Map<DatabaseType, AbstractSqlPagingQueryProvider> providers = new HashMap<DatabaseType, AbstractSqlPagingQueryProvider>();


	{
		providers.put(DB2, new Db2PagingQueryProvider());
		providers.put(DB2ZOS, new Db2PagingQueryProvider());
		providers.put(DERBY,new DerbyPagingQueryProvider());
		providers.put(HSQL,new HsqlPagingQueryProvider());
		providers.put(H2,new H2PagingQueryProvider());
		providers.put(MYSQL,new MySqlPagingQueryProvider());
		providers.put(ORACLE,new OraclePagingQueryProvider());
		providers.put(POSTGRES,new PostgresPagingQueryProvider());
		providers.put(SQLSERVER,new SqlServerPagingQueryProvider());
		providers.put(SYBASE,new SybasePagingQueryProvider());
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
	 * @param sortKey the sortKey to set
	 */
	public void setSortKey(String sortKey) {
		this.sortKey = sortKey;
	}

	/**
	 * @param ascending
	 */
	public void setAscending(boolean ascending) {
		this.ascending = ascending;	
	}

	/**
	 * Get a {@link PagingQueryProvider} instance using the provided properties
	 * and appropriate for the given database type.
	 * 
	 * @see FactoryBean#getObject()
	 */
	public Object getObject() throws Exception {

		DatabaseType type;
		try {
			type = databaseType != null ? DatabaseType.valueOf(databaseType.toUpperCase()) : DatabaseType
					.fromMetaData(dataSource);
		}
		catch (MetaDataAccessException e) {
			throw new IllegalArgumentException(
					"Could not inspect meta data for database type.  You have to supply it explicitly.", e);
		}

		AbstractSqlPagingQueryProvider provider = providers.get(type);
		Assert.state(provider!=null, "Should not happen: missing PagingQueryProvider for DatabaseType="+type);

		provider.setFromClause(fromClause);
		provider.setWhereClause(whereClause);
		provider.setSortKey(sortKey);
		provider.setAscending(ascending);
		if (StringUtils.hasText(selectClause)) {
			provider.setSelectClause(selectClause);
		}

		provider.init(dataSource);

		return provider;

	}

	/**
	 * Always returns {@link PagingQueryProvider}.
	 * 
	 * @see FactoryBean#getObjectType()
	 */
	public Class<PagingQueryProvider> getObjectType() {
		return PagingQueryProvider.class;
	}

	/**
	 * Always returns true.
	 * @see FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

}

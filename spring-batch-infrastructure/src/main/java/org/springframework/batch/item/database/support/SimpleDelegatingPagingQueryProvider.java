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

import java.util.Arrays;

import org.springframework.batch.support.DatabaseType;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import javax.sql.DataSource;

/**
 * Generic Paging Query Provider using standard SQL:2003 windowing functions.  These features are supported by
 * DB2, Oracle, SQL Server 2005, Sybase and Apache Derby version 10.4.1.3
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class SimpleDelegatingPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	AbstractSqlPagingQueryProvider delegate;

	@Override
	public void init(DataSource dataSource) throws Exception {
		super.init(dataSource);
		
		DatabaseType type = DatabaseType.fromMetaData(dataSource);
		if (type == DERBY) {
			delegate = new DerbyPagingQueryProvider();
		}
		else if (type == DB2 || type == DB2ZOS) {
			delegate = new Db2PagingQueryProvider();
		}
		else if (type == HSQL) {
			delegate = new HsqlPagingQueryProvider();
		}
		else if (type == SQLSERVER) {
			delegate = new SqlServerPagingQueryProvider();
		}
		else if (type == MYSQL) {
			delegate = new MySqlPagingQueryProvider();
		}
		else if (type == ORACLE) {
			delegate = new OraclePagingQueryProvider();
		}
		else if (type == POSTGRES) {
			delegate = new PostgresPagingQueryProvider();
		}
		else if (type == SYBASE) {
			delegate = new SybasePagingQueryProvider();
		}
		else {
			throw new InvalidDataAccessResourceUsageException(type.name() +
					" is not a supported database.  The supported databases are " +
					Arrays.toString(DatabaseType.values()));
		}
		delegate.setSelectClause(this.getSelectClause());
		delegate.setFromClause(this.getFromClause());
		if (this.getWhereClause() != null) {
			delegate.setWhereClause(this.getWhereClause());
		}
		delegate.setSortKey(this.getSortKey());
		delegate.init(dataSource);
	}

	@Override
	public String generateFirstPageQuery(int pageSize) {
		return delegate.generateFirstPageQuery(pageSize);
	}

	@Override
	public String generateRemainingPagesQuery(int pageSize) {
		return delegate.generateRemainingPagesQuery(pageSize);
	}

	@Override
	public String generateJumpToItemQuery(int itemIndex, int pageSize) {
		return delegate.generateJumpToItemQuery(itemIndex, pageSize);
	}

}

package org.springframework.batch.item.database.support;

import static org.springframework.batch.support.DatabaseType.*;

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
		else if (type == DB2) {
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
					type.values().toString());
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

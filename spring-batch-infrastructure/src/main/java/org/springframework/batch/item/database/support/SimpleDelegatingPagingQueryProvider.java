package org.springframework.batch.item.database.support;

import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import javax.sql.DataSource;
import java.util.List;
import java.util.Arrays;

/**
 * Generic Paging Query Provider using standard SQL:2003 windowing functions.  These features are supported by
 * DB2, Oracle, SQL Server 2005, Sybase and Apache Derby version 10.4.1.3
 *
 * @author Thomas Risberg
 * @since 2.0
 */
public class SimpleDelegatingPagingQueryProvider extends AbstractSqlPagingQueryProvider {

	/* List of supported database products */
	public static final List<String> supportedDatabaseProducts = Arrays.asList(
			"Apache Derby",
			"DB2",
			"HSQL Database Engine",
			"Microsoft SQL Server",
			"MySQL",
			"Oracle",
			"PostgreSQL",
			"Sybase"
		);

	AbstractSqlPagingQueryProvider delegate;

	@Override
	public void init(DataSource dataSource) throws Exception {
		super.init(dataSource);
		String databaseProductName = JdbcUtils.commonDatabaseName(
				JdbcUtils.extractDatabaseMetaData(dataSource, "getDatabaseProductName").toString());
		if ("Apache Derby".equals(databaseProductName)) {
			delegate = new DerbyPagingQueryProvider();
		}
		else if ("DB2".equals(databaseProductName)) {
			delegate = new Db2PagingQueryProvider();
		}
		else if ("HSQL Database Engine".equals(databaseProductName)) {
			delegate = new HsqlPagingQueryProvider();
		}
		else if ("Microsoft SQL Server".equals(databaseProductName)) {
			delegate = new SqlServerPagingQueryProvider();
		}
		else if ("MySQL".equals(databaseProductName)) {
			delegate = new MySqlPagingQueryProvider();
		}
		else if ("Oracle".equals(databaseProductName)) {
			delegate = new OraclePagingQueryProvider();
		}
		else if ("PostgreSQL".equals(databaseProductName)) {
			delegate = new PostgresPagingQueryProvider();
		}
		else if ("Sybase".equals(databaseProductName)) {
			delegate = new SybasePagingQueryProvider();
		}
		else {
			throw new InvalidDataAccessResourceUsageException(databaseProductName +
					" is not a supported database.  The supported databases are " +
					supportedDatabaseProducts.toString());
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

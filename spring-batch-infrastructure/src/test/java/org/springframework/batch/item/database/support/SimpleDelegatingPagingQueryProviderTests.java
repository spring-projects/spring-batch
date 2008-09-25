package org.springframework.batch.item.database.support;

import static org.junit.Assert.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;

import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import org.easymock.EasyMock;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * @author Thomas Risberg
 */
public class SimpleDelegatingPagingQueryProviderTests {

	protected AbstractSqlPagingQueryProvider pagingQueryProvider;
	protected int pageSize;
	DataSource ds;
	Connection con;
	DatabaseMetaData dmd;

	@Before
	public void onSetUp() throws Exception {
		ds = createMock(DataSource.class);
		con = createMock(Connection.class);
		dmd = createMock(DatabaseMetaData.class);
		expect(con.getMetaData()).andReturn(dmd);
		expect(ds.getConnection()).andReturn(con);

		pagingQueryProvider = new SimpleDelegatingPagingQueryProvider();
		initializeQueryProvider(pagingQueryProvider);
	}

	@Test
	public void testApacheDerby() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new DerbyPagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		expect(dmd.getDatabaseProductName()).andReturn("Apache Derby");
		DatabaseMetaData dmd2 = createMock(DatabaseMetaData.class);
		expect(dmd2.getDatabaseProductVersion()).andReturn("10.4.1.3");
		expect(con.getMetaData()).andReturn(dmd2);
		expect(ds.getConnection()).andReturn(con);

		EasyMock.replay(dmd2);
		EasyMock.replay(con);
		EasyMock.replay(dmd);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(con);
		EasyMock.verify(dmd);
		EasyMock.verify(dmd2);
	}

	@Test
	public void testDb2() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new Db2PagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("DB2/Linux");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testDb2ZOS() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new Db2PagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("DB2");
		expect(con.getMetaData()).andReturn(dmd);
		expect(ds.getConnection()).andReturn(con);
		EasyMock.expect(dmd.getDatabaseProductVersion()).andReturn("DSN08015");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testHsql() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new HsqlPagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("HSQL Database Engine");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testSqlServer() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new SqlServerPagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("Microsoft SQL Server");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testMySql() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new MySqlPagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("MySQL");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testOracle() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new OraclePagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("Oracle");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testPostgres() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new PostgresPagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("PostgreSQL");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testSybase() throws Exception {
		AbstractSqlPagingQueryProvider queryProviderToBeUsed = new SybasePagingQueryProvider();
		initializeQueryProvider(queryProviderToBeUsed);

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("Sybase");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		pagingQueryProvider.init(ds);
		String sql = queryProviderToBeUsed.generateFirstPageQuery(pageSize);
		String s = pagingQueryProvider.generateFirstPageQuery(pageSize);
		Assert.assertEquals("", sql, s);

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	@Test
	public void testUnsupportedDatabase() throws Exception {

		EasyMock.expect(dmd.getDatabaseProductName()).andReturn("MyDB");
		EasyMock.replay(dmd);
		EasyMock.replay(con);
		EasyMock.replay(ds);

		try {
			pagingQueryProvider.init(ds);
			fail("Expected an InvalidDataAccessResourceUsageException since the MyDB database is not supported");
		} catch (IllegalArgumentException e) {
			// expected
		}

		EasyMock.verify(ds);
		EasyMock.verify(con);
		EasyMock.verify(dmd);
	}

	private void initializeQueryProvider(AbstractSqlPagingQueryProvider queryProvider) throws Exception {
		queryProvider.setSelectClause("id, name, age");
		queryProvider.setFromClause("foo");
		queryProvider.setWhereClause("bar = 1");
		queryProvider.setSortKey("id");
	}


}

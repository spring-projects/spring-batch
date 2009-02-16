package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JdbcPagingItemReaderCommonTests extends AbstractItemStreamItemReaderTests {

	@Autowired
	private DataSource dataSource;

	protected ItemReader<Foo> getItemReader() throws Exception {

		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<Foo>();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setSortKey("ID");
		reader.setQueryProvider(queryProvider);
		reader.setRowMapper(
				new ParameterizedRowMapper<Foo>() {
					public Foo mapRow(ResultSet rs, int i) throws SQLException {
						Foo foo = new Foo();
						foo.setId(rs.getInt(1));
						foo.setName(rs.getString(2));
						foo.setValue(rs.getInt(3));
						return foo;
					}
				}
		);
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		JdbcPagingItemReader<Foo> reader = (JdbcPagingItemReader<Foo>) tested;
		reader.close();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("where ID = -1");
		queryProvider.setSortKey("ID");
		reader.setQueryProvider(queryProvider);
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}

}

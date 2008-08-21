package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.batch.item.CommonItemStreamItemReaderTests;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JdbcPagingItemReaderCommonTests extends CommonItemStreamItemReaderTests {

	@Autowired
	private DataSource dataSource;

	protected ItemReader<Foo> getItemReader() throws Exception {

		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<Foo>();
		reader.setSelectClause("select ID, NAME, VALUE");
		reader.setFromClause("from T_FOOS");
		reader.setSortKey("ID");
		reader.setDataSource(dataSource);
		reader.setParameterizedRowMapper(
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
		reader.close(new ExecutionContext());
		reader.setSelectClause("select ID, NAME, VALUE");
		reader.setFromClause("from T_FOOS");
		reader.setWhereClause("where id = -1");
		reader.setDataSource(dataSource);
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}

}

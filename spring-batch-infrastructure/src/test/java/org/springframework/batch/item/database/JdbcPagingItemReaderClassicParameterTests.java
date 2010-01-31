package org.springframework.batch.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

import org.junit.runner.RunWith;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/JdbcPagingItemReaderParameterTests-context.xml")
public class JdbcPagingItemReaderClassicParameterTests extends AbstractPagingItemReaderParameterTests {

	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {

		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<Foo>();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("where VALUE >= ?");
		queryProvider.setSortKey("ID");
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 3));
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

}

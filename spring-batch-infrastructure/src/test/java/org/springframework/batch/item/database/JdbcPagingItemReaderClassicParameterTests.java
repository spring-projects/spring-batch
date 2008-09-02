package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.util.Collections;
import java.sql.ResultSet;
import java.sql.SQLException;

@SuppressWarnings("unchecked")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/JdbcPagingItemReaderParameterTests-context.xml")
public class JdbcPagingItemReaderClassicParameterTests extends AbstractPagingItemReaderParameterTests {

	protected ItemReader<Foo> getItemReader() throws Exception {

		JdbcPagingItemReader<Foo> reader = new JdbcPagingItemReader<Foo>();
		reader.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setWhereClause("where VALUE >= ?");
		queryProvider.setSortKey("ID");
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 3));
		reader.setQueryProvider(queryProvider);
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

}

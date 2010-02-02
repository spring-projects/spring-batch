package org.springframework.batch.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.HsqlPagingQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

/**
 * Tests for {@link JpaPagingItemReader} with sort key not equal to ID.
 *
 * @author Thomas Risberg
 */
public class JdbcPagingItemReaderOrderIntegrationTests extends AbstractGenericDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> createItemReader() throws Exception {

		JdbcPagingItemReader<Foo> inputSource = new JdbcPagingItemReader<Foo>();
		inputSource.setDataSource(dataSource);
		HsqlPagingQueryProvider queryProvider = new HsqlPagingQueryProvider();
		queryProvider.setSelectClause("select ID, NAME, VALUE");
		queryProvider.setFromClause("from T_FOOS");
		queryProvider.setSortKey("VALUE");
		inputSource.setQueryProvider(queryProvider);
		inputSource.setRowMapper(
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
		inputSource.setPageSize(3);
		inputSource.afterPropertiesSet();
		inputSource.setSaveState(true);

		return inputSource;
	}

}

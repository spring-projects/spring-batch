package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Tests for {@link JpaPagingItemReader}.
 *
 * @author Thomas Risberg
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
public class JdbcPagingItemReaderIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> createItemReader() throws Exception {

		JdbcPagingItemReader<Foo> inputSource = new JdbcPagingItemReader<Foo>();
		inputSource.setSelectClause("select ID, NAME, VALUE");
		inputSource.setFromClause("from T_FOOS");
		inputSource.setSortKey("ID");
		inputSource.setDataSource(dataSource);
		inputSource.setParameterizedRowMapper(
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

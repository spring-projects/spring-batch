package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;

/**
 * Tests for {@link JdbcCursorItemReader}
 * 
 * @author Robert Kasanicky
 */
public class JdbcCursorItemReaderIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader createItemReader() throws Exception {
		JdbcCursorItemReader result = new JdbcCursorItemReader();
		result.setDataSource(super.getJdbcTemplate().getDataSource());
		result.setSql("select ID, NAME, VALUE from T_FOOS");
		result.setIgnoreWarnings(true);
		result.setVerifyCursorPosition(true);
		
		result.setMapper(new FooRowMapper());
		result.setFetchSize(10);
		result.setMaxRows(100);
		result.setQueryTimeout(1000);
		result.setSaveState(true);

		return result;
	}

}

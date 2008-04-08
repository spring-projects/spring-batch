package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;

public class JdbcCursorItemReaderCommonTests extends CommonDatabaseItemStreamItemReaderTests {

	protected ItemReader getItemReader() throws Exception {
		
		JdbcCursorItemReader result = new JdbcCursorItemReader();
		result.setDataSource(getDataSource());
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

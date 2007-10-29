package org.springframework.batch.io.sql;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.cursor.SqlCursorInputSource;

/**
 * Tests for {@link SqlCursorInputSource}
 * 
 * @author Robert Kasanicky
 */
public class SqlCursorInputSourceIntegrationTests extends AbstractSqlInputSourceIntegrationTests{

	protected InputSource createInputSource() throws Exception {
		SqlCursorInputSource result = new SqlCursorInputSource();
		result.setDataSource(super.getJdbcTemplate().getDataSource());
		result.setSql("select ID, NAME, VALUE from T_FOOS");
		result.setIgnoreWarnings(true);
		result.setVerifyCursorPosition(true);
		
		result.setMapper(new FooRowMapper());
		result.setFetchSize(10);
		result.setMaxRows(100);
		result.setQueryTimeout(1000);

		return result;
	}

}

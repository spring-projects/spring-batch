package org.springframework.batch.io.cursor;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.cursor.JdbcCursorInputSource;
import org.springframework.batch.io.driving.FooRowMapper;
import org.springframework.batch.io.sql.AbstractJdbcInputSourceIntegrationTests;

/**
 * Tests for {@link JdbcCursorInputSource}
 * 
 * @author Robert Kasanicky
 */
public class JdbcCursorInputSourceIntegrationTests extends AbstractJdbcInputSourceIntegrationTests{

	protected InputSource createInputSource() throws Exception {
		JdbcCursorInputSource result = new JdbcCursorInputSource();
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

package org.springframework.batch.io.sql;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.sql.SingleKeySqlDrivingQueryInputSource;

public class SingleKeySqlDrivingQueryInputSourceIntegrationTests extends AbstractSqlInputSourceIntegrationTests {

	protected InputSource source;


	/**
	 * @return input source with all necessary dependencies set
	 */
	protected InputSource createInputSource() throws Exception {

		SingleKeySqlDrivingQueryInputSource inputSource = new SingleKeySqlDrivingQueryInputSource(getJdbcTemplate(),
				"SELECT ID from T_FOOS order by ID");
		inputSource.setRestartQuery("SELECT ID from T_FOOS where ID > ? order by ID");
		return new FooInputSource(inputSource, getJdbcTemplate());

	}
}

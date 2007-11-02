package org.springframework.batch.io.driving;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.driving.support.SingleColumnJdbcKeyGenerator;
import org.springframework.batch.io.sql.AbstractSqlInputSourceIntegrationTests;

public class SingleColumnJdbcDrivingQueryInputSourceIntegrationTests extends AbstractSqlInputSourceIntegrationTests {

	protected InputSource source;


	/**
	 * @return input source with all necessary dependencies set
	 */
	protected InputSource createInputSource() throws Exception {

		SingleColumnJdbcKeyGenerator keyStrategy = new SingleColumnJdbcKeyGenerator(getJdbcTemplate(),
				"SELECT ID from T_FOOS order by ID");
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		DrivingQueryInputSource inputSource = new DrivingQueryInputSource();
		inputSource.setKeyGenerator(keyStrategy);
		
		return new FooInputSource(inputSource, getJdbcTemplate());

	}
}

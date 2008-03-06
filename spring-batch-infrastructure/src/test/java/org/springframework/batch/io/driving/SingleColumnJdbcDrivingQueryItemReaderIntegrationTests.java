package org.springframework.batch.io.driving;

import org.springframework.batch.io.driving.support.SingleColumnJdbcKeyGenerator;
import org.springframework.batch.io.sql.AbstractJdbcItemReaderIntegrationTests;
import org.springframework.batch.item.ItemReader;

public class SingleColumnJdbcDrivingQueryItemReaderIntegrationTests extends AbstractJdbcItemReaderIntegrationTests {

	protected ItemReader source;


	/**
	 * @return input source with all necessary dependencies set
	 */
	protected ItemReader createItemReader() throws Exception {

		SingleColumnJdbcKeyGenerator keyStrategy = new SingleColumnJdbcKeyGenerator(getJdbcTemplate(),
				"SELECT ID from T_FOOS order by ID");
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		DrivingQueryItemReader inputSource = new DrivingQueryItemReader();
		inputSource.setKeyGenerator(keyStrategy);
		inputSource.setSaveState(true);
		return new FooItemReader(inputSource, getJdbcTemplate());

	}
}

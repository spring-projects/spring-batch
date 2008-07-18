package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.SingleColumnJdbcKeyCollector;
import org.springframework.batch.item.sample.Foo;

public class SingleColumnJdbcDrivingQueryItemReaderIntegrationTests extends AbstractJdbcItemReaderIntegrationTests {

	protected ItemReader<Long> source;


	/**
	 * @return input source with all necessary dependencies set
	 */
	protected ItemReader<Foo> createItemReader() throws Exception {

		SingleColumnJdbcKeyCollector<Long> keyStrategy = new SingleColumnJdbcKeyCollector<Long>(getJdbcTemplate(),
				"SELECT ID from T_FOOS order by ID");
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		DrivingQueryItemReader<Long> inputSource = new DrivingQueryItemReader<Long>();
		inputSource.setKeyCollector(keyStrategy);
		inputSource.setSaveState(true);
		return new FooItemReader(inputSource, getJdbcTemplate());

	}
}

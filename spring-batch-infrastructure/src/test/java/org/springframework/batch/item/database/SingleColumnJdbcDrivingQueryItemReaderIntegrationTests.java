package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.SingleColumnJdbcKeyCollector;
import org.springframework.batch.item.sample.Foo;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.junit.runner.RunWith;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
@SuppressWarnings("deprecation")
public class SingleColumnJdbcDrivingQueryItemReaderIntegrationTests extends AbstractJdbcItemReaderIntegrationTests {

	protected ItemReader<Long> source;

	/**
	 * @return input source with all necessary dependencies set
	 */
	protected ItemReader<Foo> createItemReader() throws Exception {

		SingleColumnJdbcKeyCollector<Long> keyStrategy =
				new SingleColumnJdbcKeyCollector<Long>(simpleJdbcTemplate.getJdbcOperations(),
						"SELECT ID from T_FOOS order by ID");
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		DrivingQueryItemReader<Long> inputSource = new DrivingQueryItemReader<Long>();
		inputSource.setKeyCollector(keyStrategy);
		inputSource.setSaveState(true);
		return new FooItemReader(inputSource, dataSource);

	}
}

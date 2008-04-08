package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.SingleColumnJdbcKeyCollector;
import org.springframework.jdbc.core.JdbcTemplate;

public class SingleColumnJdbcDrivingQueryItemReaderCommonTests extends CommonDatabaseItemStreamItemReaderTests {

	protected ItemReader getItemReader() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		SingleColumnJdbcKeyCollector keyStrategy = new SingleColumnJdbcKeyCollector(jdbcTemplate,
				"SELECT ID from T_FOOS order by ID");
		keyStrategy.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		DrivingQueryItemReader reader = new DrivingQueryItemReader();
		reader.setKeyCollector(keyStrategy);
		reader.setSaveState(true);
		return new FooItemReader(reader, jdbcTemplate);
	}

}

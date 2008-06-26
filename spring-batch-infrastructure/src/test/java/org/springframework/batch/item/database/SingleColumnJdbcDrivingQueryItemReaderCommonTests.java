package org.springframework.batch.item.database;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.SingleColumnJdbcKeyCollector;
import org.springframework.jdbc.core.JdbcTemplate;

public class SingleColumnJdbcDrivingQueryItemReaderCommonTests extends CommonDatabaseItemStreamItemReaderTests {

	protected ItemReader getItemReader() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		SingleColumnJdbcKeyCollector keyCollector = new SingleColumnJdbcKeyCollector(jdbcTemplate,
				"SELECT ID from T_FOOS order by ID");
		keyCollector.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		DrivingQueryItemReader reader = new DrivingQueryItemReader();
		reader.setKeyCollector(keyCollector);
		reader.setSaveState(true);
		return new FooItemReader(reader, jdbcTemplate);
	}

	protected void pointToEmptyInput(ItemReader tested) throws Exception {
		FooItemReader fooReader = (FooItemReader) tested;
		fooReader.close(new ExecutionContext());
		
		DrivingQueryItemReader reader = new DrivingQueryItemReader();
		reader.close(new ExecutionContext());
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		SingleColumnJdbcKeyCollector keyCollector = new SingleColumnJdbcKeyCollector(jdbcTemplate,
				"SELECT ID from T_FOOS where ID < 0");
		
		reader.setKeyCollector(keyCollector);
		reader.afterPropertiesSet();
		
		fooReader.setItemReader(reader);
		fooReader.open(new ExecutionContext());
		
	}

}

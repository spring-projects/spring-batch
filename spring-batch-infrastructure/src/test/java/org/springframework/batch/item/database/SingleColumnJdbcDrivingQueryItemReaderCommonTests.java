package org.springframework.batch.item.database;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.SingleColumnJdbcKeyCollector;
import org.springframework.batch.item.sample.Foo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;

@RunWith(JUnit4ClassRunner.class)
public class SingleColumnJdbcDrivingQueryItemReaderCommonTests extends CommonDatabaseItemStreamItemReaderTests {

	protected ItemReader<Foo> getItemReader() throws Exception {
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		SingleColumnJdbcKeyCollector<Long> keyCollector = new SingleColumnJdbcKeyCollector<Long>(jdbcTemplate,
				"SELECT ID from T_FOOS order by ID");
		keyCollector.setRestartSql("SELECT ID from T_FOOS where ID > ? order by ID");
		DrivingQueryItemReader<Long> reader = new DrivingQueryItemReader<Long>();
		reader.setKeyCollector(keyCollector);
		reader.setSaveState(true);
		return new FooItemReader(reader, getDataSource());
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		FooItemReader fooReader = (FooItemReader) tested;
		fooReader.close(new ExecutionContext());
		
		DrivingQueryItemReader<Long> reader = new DrivingQueryItemReader<Long>();
		reader.close(new ExecutionContext());
		
		JdbcTemplate jdbcTemplate = new JdbcTemplate(getDataSource());
		SingleColumnJdbcKeyCollector<Long> keyCollector = new SingleColumnJdbcKeyCollector<Long>(jdbcTemplate,
				"SELECT ID from T_FOOS where ID < 0");
		
		reader.setKeyCollector(keyCollector);
		reader.afterPropertiesSet();
		
		fooReader.setItemReader(reader);
		fooReader.open(new ExecutionContext());
		
	}

}

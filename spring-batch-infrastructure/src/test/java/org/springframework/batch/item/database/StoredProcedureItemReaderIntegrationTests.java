package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "stored-procedure-context.xml")
public class StoredProcedureItemReaderIntegrationTests 
		extends AbstractDataSourceItemReaderIntegrationTests {

	@Override
	protected ItemReader<Foo> createItemReader() throws Exception {
		StoredProcedureItemReader<Foo> reader = new StoredProcedureItemReader<Foo>();
		reader.setDataSource(dataSource);
		reader.setProcedureName("read_foos");
		reader.setRowMapper(new FooRowMapper());
		reader.setVerifyCursorPosition(false);
		return reader;
	}

}

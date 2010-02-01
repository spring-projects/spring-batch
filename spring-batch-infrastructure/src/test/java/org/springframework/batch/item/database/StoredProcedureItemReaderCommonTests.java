package org.springframework.batch.item.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hsqldb.Types;
import org.junit.Test;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.batch.item.sample.Foo;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlParameter;

@RunWith(JUnit4ClassRunner.class)
public class StoredProcedureItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	protected ItemReader<Foo> getItemReader() throws Exception {
		StoredProcedureItemReader<Foo> result = new StoredProcedureItemReader<Foo>();
		result.setDataSource(getDataSource());
		result.setProcedureName("read_foos");
		result.setRowMapper(new FooRowMapper());
		result.setVerifyCursorPosition(false);
		result.afterPropertiesSet();
		return result;
	}

	protected void initializeContext() throws Exception {
		ctx = new ClassPathXmlApplicationContext("org/springframework/batch/item/database/stored-procedure-context.xml");
	}

	@Test
	public void testRestartWithDriverSupportsAbsolute() throws Exception {
		testedAsStream().close();
		tested = getItemReader();
		((StoredProcedureItemReader<Foo>) tested).setDriverSupportsAbsolute(true);
		testedAsStream().open(executionContext);
		testedAsStream().close();
		testedAsStream().open(executionContext);
		testRestart();
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		StoredProcedureItemReader<Foo> reader = (StoredProcedureItemReader<Foo>) tested;
		reader.close();
		reader.setDataSource(getDataSource());
		reader.setProcedureName("read_some_foos");
		reader.setParameters(
				new SqlParameter[] {
					new SqlParameter("from_id", Types.NUMERIC),	
					new SqlParameter("to_id", Types.NUMERIC)	
				});
		reader.setPreparedStatementSetter(
				new PreparedStatementSetter() {
					public void setValues(PreparedStatement ps)
							throws SQLException {
						ps.setInt(1, 1000);
						ps.setInt(2, 1001);
					}
				});
		reader.setRowMapper(new FooRowMapper());
		reader.setVerifyCursorPosition(false);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());		
	}

	@Test(expected=ReaderNotOpenException.class)
	public void testReadBeforeOpen() throws Exception {
		testedAsStream().close();
		tested = getItemReader();
		tested.read();
	}
	
}

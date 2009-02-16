package org.springframework.batch.core.resource;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/core/repository/dao/data-source-context.xml")
public class JdbcCursorItemReaderPreparedStatementIntegrationTests {

	JdbcCursorItemReader<Foo> itemReader;

	private DataSource dataSource;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	@Before
	public void onSetUpInTransaction() throws Exception {
		
		itemReader = new JdbcCursorItemReader<Foo>();
		itemReader.setDataSource(dataSource);
		itemReader.setSql("select ID, NAME, VALUE from T_FOOS where ID > ? and ID < ?");
		itemReader.setIgnoreWarnings(true);
		itemReader.setVerifyCursorPosition(true);
		
		itemReader.setRowMapper(new FooRowMapper());
		itemReader.setFetchSize(10);
		itemReader.setMaxRows(100);
		itemReader.setQueryTimeout(1000);
		itemReader.setSaveState(true);
		StepExecutionPreparedStatementSetter pss = new StepExecutionPreparedStatementSetter();
		JobParameters jobParameters = new JobParametersBuilder().addLong("begin.id", 1L).addLong("end.id", 4L).toJobParameters();
		JobInstance jobInstance = new JobInstance(1L, jobParameters, "simpleJob");
		JobExecution jobExecution = new JobExecution(jobInstance, (long) 2);
		StepExecution stepExecution = new StepExecution("taskletStep", jobExecution, 3L);
		pss.beforeStep(stepExecution);
		
		List<String> parameterNames = new ArrayList<String>();
		parameterNames.add("begin.id");
		parameterNames.add("end.id");
		pss.setParameterKeys(parameterNames);
		
		itemReader.setPreparedStatementSetter(pss);
	}
	
	@Transactional @Test
	public void testRead() throws Exception{
		itemReader.open(new ExecutionContext());
		Foo foo = itemReader.read();
		assertEquals(2, foo.getId());
		foo = itemReader.read();
		assertEquals(3, foo.getId());
		assertNull(itemReader.read());
	}
	
}

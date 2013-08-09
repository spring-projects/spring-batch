package org.springframework.batch.core.step.builder;

import java.util.List;

import javax.sql.DataSource;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test for registering a listener class that implements different listeners interfaces
 * just once in java based configuration.
 * 
 * @author Tobias Flohre
 */
@ContextConfiguration(classes=RegisterMultiListenerTest.MultiListenerTestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RegisterMultiListenerTest {
	
	@Autowired
	private JobLauncher jobLauncher;
	
	@Autowired
	private Job job;
	
	@Autowired
	private CallChecker callChecker;
	
	@Test
	public void testMultiListener() throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException{
		jobLauncher.run(job, new JobParameters());
		assertTrue("beforeStep hasn't been called",callChecker.beforeStepCalled);
		assertTrue("beforeChunk hasn't been called",callChecker.beforeChunkCalled);
		assertTrue("beforeWrite hasn't been called",callChecker.beforeWriteCalled);
		assertTrue("skipInWrite hasn't been called",callChecker.skipInWriteCalled);
	}
	
	@Configuration
	@EnableBatchProcessing
	public static class MultiListenerTestConfiguration{
		
		@Autowired
		private JobBuilderFactory jobBuilders;
		
		@Autowired
		private StepBuilderFactory stepBuilders;
		
		@Bean
		public Job testJob(){
			return jobBuilders.get("testJob")
					.start(step())
					.build();
		}
		
		@Bean
		public Step step(){
			return stepBuilders.get("step")
					.listener(listener())
					.<String,String>chunk(1)
					.reader(reader())
					.writer(writer())
					.faultTolerant()
					.skipLimit(1)
					.skip(MySkippableException.class)
					.build();
		}

		
		@Bean
		public DataSource dataSource(){
			EmbeddedDatabaseBuilder embeddedDatabaseBuilder = new EmbeddedDatabaseBuilder();
			return embeddedDatabaseBuilder.addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql")
					.setType(EmbeddedDatabaseType.HSQL)
					.build();
		}
		
		@Bean
		public CallChecker callChecker(){
			return new CallChecker();
		}
		
		@Bean
		public MultiListener listener(){
			return new MultiListener(callChecker());
		}
		
		@Bean
		public ItemReader<String> reader(){
			return new ItemReader<String>(){

				@Override
				public String read() throws Exception,
						UnexpectedInputException, ParseException,
						NonTransientResourceException {
					return "item";
				}
				
			};
		}
		
		@Bean
		public ItemWriter<String> writer(){
			return new ItemWriter<String>(){

				@Override
				public void write(List<? extends String> items)
						throws Exception {
					throw new MySkippableException();
				}
				
			};
		}
		
	}
	
	private static class CallChecker {
		boolean beforeStepCalled = false;
		boolean beforeChunkCalled = false;
		boolean beforeWriteCalled = false;
		boolean skipInWriteCalled = false;
	}
	
	private static class MultiListener implements StepExecutionListener, ChunkListener, ItemWriteListener<String>, SkipListener<String,String>{
		
		private CallChecker callChecker;
		
		private MultiListener(CallChecker callChecker) {
			super();
			this.callChecker = callChecker;
		}

		@Override
		public void onSkipInRead(Throwable t) {
		}

		@Override
		public void onSkipInWrite(String item, Throwable t) {
			callChecker.skipInWriteCalled = true;
		}

		@Override
		public void onSkipInProcess(String item, Throwable t) {
		}

		@Override
		public void beforeWrite(List<? extends String> items) {
			callChecker.beforeWriteCalled = true;
		}

		@Override
		public void afterWrite(List<? extends String> items) {
		}

		@Override
		public void onWriteError(Exception exception,
				List<? extends String> items) {
		}

		@Override
		public void beforeChunk(ChunkContext context) {
			callChecker.beforeChunkCalled = true;
		}

		@Override
		public void afterChunk(ChunkContext context) {
		}

		@Override
		public void afterChunkError(ChunkContext context) {
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
			callChecker.beforeStepCalled = true;
		}

		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			return null;
		}
		
	}
	
	private static class MySkippableException extends RuntimeException{

		private static final long serialVersionUID = 1L;
		
	}

}

package org.springframework.batch.core.step.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Test for registering a listener class that implements different listeners interfaces
 * just once in java based configuration.
 *
 * @author Tobias Flohre
 */
public class RegisterMultiListenerTest {

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private Job job;

	@Autowired
	private CallChecker callChecker;

	@Autowired
	private EmbeddedDatabase dataSource;

	private GenericApplicationContext context;

	@After
	public void tearDown() {
		jobLauncher = null;
		job = null;
		callChecker = null;

		if(dataSource != null) {
			dataSource.shutdown();
		}

		if(context != null) {
			context.close();
		}
	}

	@Test
	public void testMultiListenerSimpleStep() throws Exception {
		bootstrap(MultiListenerFaultTolerantTestConfiguration.class);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertTrue("beforeStep hasn't been called",callChecker.beforeStepCalled);
		assertTrue("beforeChunk hasn't been called",callChecker.beforeChunkCalled);
		assertTrue("beforeWrite hasn't been called",callChecker.beforeWriteCalled);
		assertTrue("skipInWrite hasn't been called",callChecker.skipInWriteCalled);
	}

	@Test
	public void testMultiListenerFaultTolerantStep() throws Exception {
		bootstrap(MultiListenerTestConfiguration.class);

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertTrue("beforeStep hasn't been called",callChecker.beforeStepCalled);
		assertTrue("beforeChunk hasn't been called",callChecker.beforeChunkCalled);
		assertTrue("beforeWrite hasn't been called",callChecker.beforeWriteCalled);
	}

	private void bootstrap(Class<?> configurationClass) {
		context = new AnnotationConfigApplicationContext(configurationClass);
		context.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	public static abstract class MultiListenerTestConfigurationSupport {

		@Autowired
		protected JobBuilderFactory jobBuilders;

		@Autowired
		protected StepBuilderFactory stepBuilders;

		@Bean
		public Job testJob(){
			return jobBuilders.get("testJob")
					.start(step())
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

				private int count = 0;

				@Override
				public String read() throws Exception,
				UnexpectedInputException, ParseException,
				NonTransientResourceException {
					count++;

					if(count < 5) {
						System.err.println("returning item with count " + count);
						return "item" + count;
					} else {
						return null;
					}
				}

			};
		}

		@Bean
		public ItemWriter<String> writer(){
			return new ItemWriter<String>(){

				@Override
				public void write(List<? extends String> items)
						throws Exception {
					if(items.contains("item2")) {
						throw new MySkippableException();
					}
				}

			};
		}

		public abstract Step step();
	}

	@Configuration
	@EnableBatchProcessing
	public static class MultiListenerFaultTolerantTestConfiguration extends MultiListenerTestConfigurationSupport{

		@Override
		@Bean
		public Step step(){
			return stepBuilders.get("step")
					.listener(listener())
					.<String,String>chunk(2)
					.reader(reader())
					.writer(writer())
					.faultTolerant()
					.skipLimit(1)
					.listener((SkipListener<String, String>) listener())
					.skip(MySkippableException.class)
					.build();
		}
	}

	@Configuration
	@EnableBatchProcessing
	public static class MultiListenerTestConfiguration extends MultiListenerTestConfigurationSupport{

		@Override
		@Bean
		public Step step(){
			return stepBuilders.get("step")
					.listener(listener())
					.<String,String>chunk(2)
					.reader(reader())
					.writer(writer())
					.build();
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
			System.err.println("skipWrite was called");
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
			System.err.println("write error was called");
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

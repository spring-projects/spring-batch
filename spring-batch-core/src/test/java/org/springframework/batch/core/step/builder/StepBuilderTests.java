/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.step.builder;

import java.sql.Connection;

import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Dave Syer
 * 
 */
public class StepBuilderTests {

	@Test
	public void test() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getJobRepository();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution(
				"step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		TaskletStepBuilder builder = new StepBuilder("step").repository(jobRepository)
				.transactionManager(transactionManager).tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						return null;
					}
				});
		builder.build().execute(execution);
	}

	@Test
	public void testTransactionDefinitionFromContext() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getJobRepository();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution(
				"step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();
		transactionAttribute.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
		AbstractTaskletStepBuilder<TaskletStepBuilder> builder = new StepBuilder("step").repository(jobRepository)
				.transactionManager(transactionManager).tasklet(new Tasklet() {
					@Override
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel().intValue());
						return null;
					}
				}).transactionAttribute(transactionAttribute);
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	public void testTransactionDefinitionFromTaskletMethodAnnotation() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getJobRepository();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution(
				"step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		AbstractTaskletStepBuilder<TaskletStepBuilder> builder = new StepBuilder("step").repository(jobRepository)
				.transactionManager(transactionManager).tasklet(new Tasklet() {
					@Override
					@Transactional(isolation=Isolation.REPEATABLE_READ)
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel().intValue());
						return null;
					}
				});
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}
	
	@Test
	public void testTransactionDefinitionFromTaskletClassAnnotation() throws Exception {
		@Transactional(isolation=Isolation.REPEATABLE_READ)
		class AnnotatedTasklet implements Tasklet {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
				assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
				assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel().intValue());
				return null;
			}			
		}		
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getJobRepository();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution(
				"step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		AbstractTaskletStepBuilder<TaskletStepBuilder> builder = new StepBuilder("step").repository(jobRepository)
				.transactionManager(transactionManager).tasklet(new AnnotatedTasklet());
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	public void testTransactionDefinitionPrecedence() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getJobRepository();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution(
				"step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute();
		transactionAttribute.setIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ);
		AbstractTaskletStepBuilder<TaskletStepBuilder> builder = new StepBuilder("step").repository(jobRepository)
				.transactionManager(transactionManager).tasklet(new Tasklet() {
					@Override
					@Transactional(isolation=Isolation.SERIALIZABLE)
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
						assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TransactionSynchronizationManager.getCurrentTransactionIsolationLevel().intValue());
						return null;
					}
				}).transactionAttribute(transactionAttribute);
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

	@Test
	public void testTransactionDefinitionNotSupported() throws Exception {
		JobRepository jobRepository = new MapJobRepositoryFactoryBean().getJobRepository();
		StepExecution execution = jobRepository.createJobExecution("foo", new JobParameters()).createStepExecution(
				"step");
		jobRepository.add(execution);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		AbstractTaskletStepBuilder<TaskletStepBuilder> builder = new StepBuilder("step").repository(jobRepository)
				.transactionManager(transactionManager).tasklet(new Tasklet() {
					@Override
					@Transactional(propagation=Propagation.NOT_SUPPORTED)
					public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
							throws Exception {
						assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
						return null;
					}
				});
		builder.build().execute(execution);
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
	}

}

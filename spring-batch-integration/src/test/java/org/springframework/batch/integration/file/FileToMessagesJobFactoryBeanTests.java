/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.integration.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.JobRepositorySupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageConsumer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 * 
 */
public class FileToMessagesJobFactoryBeanTests {

	private static final String FILE_INPUT_PATH = ResourcePayloadAsJobParameterStrategy.FILE_INPUT_PATH;
	private FileToMessagesJobFactoryBean<FieldSet> factory = new FileToMessagesJobFactoryBean<FieldSet>();
	private DirectChannel channel = new DirectChannel();
	private List<FieldSet> receiver = new ArrayList<FieldSet>();
	private JobRepositorySupport jobRepository;

	@Before
	public void setUp() {
		jobRepository = new JobRepositorySupport();
		factory.setJobRepository(jobRepository);
		factory.setTransactionManager(new ResourcelessTransactionManager());
		FlatFileItemReader<FieldSet> itemReader = new FlatFileItemReader<FieldSet>();
		itemReader.setFieldSetMapper(new PassThroughFieldSetMapper());
		factory.setItemReader(itemReader);
		factory.setChannel(channel);
		channel.subscribe(new MessageConsumer() {
			public void onMessage(Message<?> message) {
				// TODO: Ask Mark: unsafe cast...
				receiver.add((FieldSet) message.getPayload());
			}
		});
	}
	
	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#setBeanName(java.lang.String)}.
	 * @throws Exception 
	 */
	@Test
	public void testSetBeanName() throws Exception {
		assertNotNull(((Job) factory.getObject()).getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#setItemReader(org.springframework.batch.item.ItemReader)}.
	 */
	@Test
	public void testSetItemReader() {
		Method method = ReflectionUtils.findMethod(FileToMessagesJobFactoryBean.class, "setItemReader",
				new Class<?>[] { ItemReader.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#setChannel(org.springframework.integration.channel.MessageChannel)}.
	 */
	@Test
	public void testSetChannel() {
		Method method = ReflectionUtils.findMethod(FileToMessagesJobFactoryBean.class, "setChannel",
				new Class<?>[] { MessageChannel.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#setJobRepository(org.springframework.batch.core.repository.JobRepository)}.
	 */
	@Test
	public void testSetJobRepository() {
		Method method = ReflectionUtils.findMethod(FileToMessagesJobFactoryBean.class, "setJobRepository",
				new Class<?>[] { JobRepository.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#setTransactionManager(org.springframework.transaction.PlatformTransactionManager)}.
	 */
	@Test
	public void testSetTransactionManager() {
		Method method = ReflectionUtils.findMethod(FileToMessagesJobFactoryBean.class, "setTransactionManager",
				new Class<?>[] { PlatformTransactionManager.class });
		assertNotNull(method);
		Annotation[] annotations = AnnotationUtils.getAnnotations(method);
		assertEquals(1, annotations.length);
		assertEquals(Required.class, annotations[0].annotationType());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#getObject()}.
	 * @throws Exception 
	 */
	@Test
	public void testGetObjectNotBroken() throws Exception {
		assertNotNull(factory.getObject());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#getObjectType()}.
	 */
	@Test
	public void testGetObjectType() {
		FileToMessagesJobFactoryBean<?> factory = new FileToMessagesJobFactoryBean<Object>();
		assertEquals(Job.class, factory.getObjectType());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.integration.file.FileToMessagesJobFactoryBean#isSingleton()}.
	 */
	@Test
	public void testIsSingleton() {
		FileToMessagesJobFactoryBean<?> factory = new FileToMessagesJobFactoryBean<Object>();
		assertEquals(true, factory.isSingleton());
	}
	
	@Test
	public void testVanillaJobExecution() throws Exception {

		Job job = (Job) factory.getObject();
		JobParameters jobParameters = new JobParametersBuilder().addString(FILE_INPUT_PATH, "classpath:/log4j.properties").toJobParameters();
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);

		job.execute(jobExecution);
		assertNotNull(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

		FieldSet payload;

		// first line from properties file
		payload = receiver.get(0);
		assertNotNull(payload);
		// second line from properties file
		payload = receiver.get(1);
		assertNotNull(payload);
	}

}

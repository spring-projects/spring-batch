/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Inject;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * <p>
 * Test cases for parsing various &lt;properties /&gt; elements defined by JSR-352.
 * </p>
 *
 * @author Chris Schaefer
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobPropertyTests {
	@Autowired
	private TestItemReader testItemReader;

	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private TestItemProcessor testItemProcessor;

	@Autowired
	private TestItemWriter testItemWriter;

	@Autowired
	private TestCheckpointAlgorithm testCheckpointAlgorithm;

	@Autowired
	private TestDecider testDecider;

	@Autowired
	private TestStepListener testStepListener;

	@Autowired
	private TestBatchlet testBatchlet;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testJobLevelPropertiesInItemReader() throws Exception {
		assertEquals("jobPropertyValue1", testItemReader.getJobPropertyName1());
		assertEquals("jobPropertyValue2", testItemReader.getJobPropertyName2());
	}

	@Test
	public void testStepContextProperties() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Test
	public void testItemReaderProperties() throws Exception {
		assertEquals("readerPropertyValue1", testItemReader.getReaderPropertyName1());
		assertEquals("readerPropertyValue2", testItemReader.getReaderPropertyName2());
		assertEquals("annotationNamedReaderPropertyValue", testItemReader.getAnnotationNamedProperty());
		assertNull(testItemReader.getNotDefinedProperty());
		assertNull(testItemReader.getNotDefinedAnnotationNamedProperty());
	}

	@Test
	public void testItemProcessorProperties() throws Exception {
		Assert.assertEquals("processorPropertyValue1", testItemProcessor.getProcessorPropertyName1());
		Assert.assertEquals("processorPropertyValue2", testItemProcessor.getProcessorPropertyName2());
		assertEquals("annotationNamedProcessorPropertyValue", testItemProcessor.getAnnotationNamedProperty());
		assertNull(testItemProcessor.getNotDefinedProperty());
		assertNull(testItemProcessor.getNotDefinedAnnotationNamedProperty());
	}

	@Test
	public void testItemWriterProperties() throws Exception {
		Assert.assertEquals("writerPropertyValue1", testItemWriter.getWriterPropertyName1());
		Assert.assertEquals("writerPropertyValue2", testItemWriter.getWriterPropertyName2());
		assertEquals("annotationNamedWriterPropertyValue", testItemWriter.getAnnotationNamedProperty());
		assertNull(testItemWriter.getNotDefinedProperty());
		assertNull(testItemWriter.getNotDefinedAnnotationNamedProperty());
	}

	@Test
	public void testCheckpointAlgorithmProperties() throws Exception {
		Assert.assertEquals("algorithmPropertyValue1", testCheckpointAlgorithm.getAlgorithmPropertyName1());
		Assert.assertEquals("algorithmPropertyValue2", testCheckpointAlgorithm.getAlgorithmPropertyName2());
		assertEquals("annotationNamedAlgorithmPropertyValue", testCheckpointAlgorithm.getAnnotationNamedProperty());
		assertNull(testCheckpointAlgorithm.getNotDefinedProperty());
		assertNull(testCheckpointAlgorithm.getNotDefinedAnnotationNamedProperty());
	}

	@Test
	public void testDeciderProperties() throws Exception {
		Assert.assertEquals("deciderPropertyValue1", testDecider.getDeciderPropertyName1());
		Assert.assertEquals("deciderPropertyValue2", testDecider.getDeciderPropertyName2());
		assertEquals("annotationNamedDeciderPropertyValue", testDecider.getAnnotationNamedProperty());
		assertNull(testDecider.getNotDefinedProperty());
		assertNull(testDecider.getNotDefinedAnnotationNamedProperty());
	}

	@Test
	public void testStepListenerProperties() throws Exception {
		Assert.assertEquals("stepListenerPropertyValue1", testStepListener.getStepListenerPropertyName1());
		Assert.assertEquals("stepListenerPropertyValue2", testStepListener.getStepListenerPropertyName2());
		assertEquals("annotationNamedStepListenerPropertyValue", testStepListener.getAnnotationNamedProperty());
		assertNull(testStepListener.getNotDefinedProperty());
		assertNull(testStepListener.getNotDefinedAnnotationNamedProperty());
	}

	@Test
	public void testBatchletProperties() throws Exception {
		Assert.assertEquals("batchletPropertyValue1", testBatchlet.getBatchletPropertyName1());
		Assert.assertEquals("batchletPropertyValue2", testBatchlet.getBatchletPropertyName2());
		assertEquals("annotationNamedBatchletPropertyValue", testBatchlet.getAnnotationNamedProperty());
		assertNull(testBatchlet.getNotDefinedProperty());
		assertNull(testBatchlet.getNotDefinedAnnotationNamedProperty());
	}

	@Test
	public void testFieldWithInjectAnnotationOnlyInjects() throws Exception {
		assertNotNull(testItemReader.getInjectAnnotatedOnlyField());
		assertEquals("Chris", testItemReader.getInjectAnnotatedOnlyField().getName());
	}

	@Test
	public void testFieldWithBatchPropertyAnnotationOnlyNoInjection() throws Exception {
		assertNull(testItemReader.getBatchAnnotatedOnlyField());
	}

	public static final class TestItemReader implements ItemReader {
		private int cnt;

		@Inject @BatchProperty String readerPropertyName1;
		@Inject @BatchProperty String readerPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedReaderPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;
		@Inject @BatchProperty String jobPropertyName1;
		@Inject @BatchProperty String jobPropertyName2;
		@Inject InjectTestObj injectAnnotatedOnlyField;
		@BatchProperty String batchAnnotatedOnlyField;
		@Inject javax.batch.runtime.context.StepContext stepContext;

		@Override
		public void open(Serializable serializable) throws Exception {
			org.springframework.util.Assert.notNull(stepContext);
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step2PropertyName1"));
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step2PropertyName2"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step1PropertyName1").equals("step1PropertyValue1"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step1PropertyName2").equals("step1PropertyValue2"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName1").equals("jobPropertyValue1"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName2").equals("jobPropertyValue2"));
		}

		@Override
		public void close() throws Exception {
		}

		@Override
		public Object readItem() throws Exception {
			if (cnt == 0) {
				cnt++;
				return "blah";
			}

			return null;
		}

		@Override
		public Serializable checkpointInfo() throws Exception {
			return null;
		}

		String getReaderPropertyName1() {
			return readerPropertyName1;
		}

		String getReaderPropertyName2() {
			return readerPropertyName2;
		}

		String getAnnotationNamedProperty() {
			return annotationNamedProperty;
		}

		String getNotDefinedProperty() {
			return notDefinedProperty;
		}

		String getNotDefinedAnnotationNamedProperty() {
			return notDefinedAnnotationNamedProperty;
		}

		String getJobPropertyName1() {
			return jobPropertyName1;
		}

		String getJobPropertyName2() {
			return jobPropertyName2;
		}

		InjectTestObj getInjectAnnotatedOnlyField() {
			return injectAnnotatedOnlyField;
		}

		String getBatchAnnotatedOnlyField() {
			return batchAnnotatedOnlyField;
		}
	}

	public static final class TestItemProcessor implements ItemProcessor {
		@Inject @BatchProperty String processorPropertyName1;
		@Inject @BatchProperty String processorPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedProcessorPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public Object processItem(Object o) throws Exception {
			return o;
		}

		String getProcessorPropertyName1() {
			return processorPropertyName1;
		}

		String getProcessorPropertyName2() {
			return processorPropertyName2;
		}

		String getAnnotationNamedProperty() {
			return annotationNamedProperty;
		}

		String getNotDefinedProperty() {
			return notDefinedProperty;
		}

		String getNotDefinedAnnotationNamedProperty() {
			return notDefinedAnnotationNamedProperty;
		}
	}

	public static final class TestItemWriter implements ItemWriter {
		@Inject @BatchProperty String writerPropertyName1;
		@Inject @BatchProperty String writerPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedWriterPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public void open(Serializable serializable) throws Exception {
		}

		@Override
		public void close() throws Exception {
		}

		@Override
		public void writeItems(List<Object> objects) throws Exception {
			System.out.println(objects);
		}

		@Override
		public Serializable checkpointInfo() throws Exception {
			return null;
		}

		String getWriterPropertyName1() {
			return writerPropertyName1;
		}

		String getWriterPropertyName2() {
			return writerPropertyName2;
		}

		String getAnnotationNamedProperty() {
			return annotationNamedProperty;
		}

		String getNotDefinedProperty() {
			return notDefinedProperty;
		}

		String getNotDefinedAnnotationNamedProperty() {
			return notDefinedAnnotationNamedProperty;
		}
	}

	public static final class TestCheckpointAlgorithm implements CompletionPolicy {
		@Inject @BatchProperty String algorithmPropertyName1;
		@Inject @BatchProperty String algorithmPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedAlgorithmPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public boolean isComplete(RepeatContext context, RepeatStatus result) {
			return true;
		}

		@Override
		public boolean isComplete(RepeatContext context) {
			return true;
		}

		@Override
		public RepeatContext start(RepeatContext parent) {
			return parent;
		}

		@Override
		public void update(RepeatContext context) {
		}

		String getAlgorithmPropertyName1() {
			return algorithmPropertyName1;
		}

		String getAlgorithmPropertyName2() {
			return algorithmPropertyName2;
		}

		String getAnnotationNamedProperty() {
			return annotationNamedProperty;
		}

		String getNotDefinedProperty() {
			return notDefinedProperty;
		}

		String getNotDefinedAnnotationNamedProperty() {
			return notDefinedAnnotationNamedProperty;
		}
	}

	public static class TestDecider implements JobExecutionDecider {
		@Inject @BatchProperty String deciderPropertyName1;
		@Inject @BatchProperty String deciderPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedDeciderPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public FlowExecutionStatus decide(JobExecution jobExecution,
				StepExecution stepExecution) {
			return new FlowExecutionStatus("step2");
		}

		String getDeciderPropertyName1() {
			return deciderPropertyName1;
		}

		String getDeciderPropertyName2() {
			return deciderPropertyName2;
		}

		String getAnnotationNamedProperty() {
			return annotationNamedProperty;
		}

		String getNotDefinedProperty() {
			return notDefinedProperty;
		}

		String getNotDefinedAnnotationNamedProperty() {
			return notDefinedAnnotationNamedProperty;
		}
	}

	public static class TestStepListener implements javax.batch.api.chunk.listener.ItemReadListener,
	javax.batch.api.chunk.listener.ItemProcessListener, javax.batch.api.chunk.listener.ItemWriteListener {
		@Inject @BatchProperty String stepListenerPropertyName1;
		@Inject @BatchProperty String stepListenerPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedStepListenerPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public void beforeProcess(Object o) throws Exception {
		}

		@Override
		public void afterProcess(Object o, Object o2) throws Exception {
		}

		@Override
		public void onProcessError(Object o, Exception e) throws Exception {
		}

		@Override
		public void beforeRead() throws Exception {
		}

		@Override
		public void afterRead(Object o) throws Exception {
		}

		@Override
		public void onReadError(Exception e) throws Exception {
		}

		@Override
		public void beforeWrite(List<Object> objects) throws Exception {
		}

		@Override
		public void afterWrite(List<Object> objects) throws Exception {
		}

		@Override
		public void onWriteError(List<Object> objects, Exception e) throws Exception {
		}

		String getStepListenerPropertyName1() {
			return stepListenerPropertyName1;
		}

		String getStepListenerPropertyName2() {
			return stepListenerPropertyName2;
		}

		String getAnnotationNamedProperty() {
			return annotationNamedProperty;
		}

		String getNotDefinedProperty() {
			return notDefinedProperty;
		}

		String getNotDefinedAnnotationNamedProperty() {
			return notDefinedAnnotationNamedProperty;
		}
	}

	public static class TestBatchlet implements Batchlet {
		@Inject @BatchProperty String batchletPropertyName1;
		@Inject @BatchProperty String batchletPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedBatchletPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;
		@Inject javax.batch.runtime.context.StepContext stepContext;

		@Override
		public String process() throws Exception {
			org.springframework.util.Assert.notNull(stepContext);
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step1PropertyName1"));
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step1PropertyName2"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step2PropertyName1").equals("step2PropertyValue1"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step2PropertyName2").equals("step2PropertyValue2"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName1").equals("jobPropertyValue1"));
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName2").equals("jobPropertyValue2"));

			return null;
		}

		@Override
		public void stop() throws Exception {
		}

		String getBatchletPropertyName1() {
			return batchletPropertyName1;
		}

		String getBatchletPropertyName2() {
			return batchletPropertyName2;
		}

		String getAnnotationNamedProperty() {
			return annotationNamedProperty;
		}

		String getNotDefinedProperty() {
			return notDefinedProperty;
		}

		String getNotDefinedAnnotationNamedProperty() {
			return notDefinedAnnotationNamedProperty;
		}
	}

	public static class InjectTestObj {
		private String name;

		public InjectTestObj(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}

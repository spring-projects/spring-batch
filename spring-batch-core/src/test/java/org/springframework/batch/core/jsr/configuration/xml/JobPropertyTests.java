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

import java.io.Serializable;
import java.util.List;

import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.api.Decider;
import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.listener.StepListener;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Configuration test for parsing various &lt;properties /&gt; elements defined by JSR-352.
 * </p>
 *
 * @author Chris Schaefer
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobPropertyTests {
	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

	@Test
	public void testJobPropertyConfiguration() throws Exception {
		JobExecution jobExecution = jobLauncher.run(job, new JobParameters());
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	public static final class TestItemReader implements ItemReader {
		private int cnt;

		@Inject @BatchProperty String readerPropertyName1;
		@Inject @BatchProperty String readerPropertyName2;
		@Inject @BatchProperty String readerPropertyName3;
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
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName1") == null);
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName2") == null);
			org.springframework.util.Assert.isTrue("jobPropertyValue1".equals(jobPropertyName1));
			org.springframework.util.Assert.isTrue("jobPropertyValue2".equals(jobPropertyName2));
			org.springframework.util.Assert.isTrue("readerPropertyValue1".equals(readerPropertyName1));
			org.springframework.util.Assert.isTrue("readerPropertyValue2".equals(readerPropertyName2));
			org.springframework.util.Assert.isTrue("annotationNamedReaderPropertyValue".equals(annotationNamedProperty));
			org.springframework.util.Assert.isNull(notDefinedProperty);
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty);
			org.springframework.util.Assert.isNull(batchAnnotatedOnlyField);
			org.springframework.util.Assert.notNull(injectAnnotatedOnlyField);
			org.springframework.util.Assert.isTrue("Chris".equals(injectAnnotatedOnlyField.getName()));
			org.springframework.util.Assert.isNull(readerPropertyName3);
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
	}

	public static final class TestItemProcessor implements ItemProcessor {
		@Inject @BatchProperty String processorPropertyName1;
		@Inject @BatchProperty String processorPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedProcessorPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public Object processItem(Object o) throws Exception {
			org.springframework.util.Assert.isTrue("processorPropertyValue1".equals(processorPropertyName1));
			org.springframework.util.Assert.isTrue("processorPropertyValue2".equals(processorPropertyName2));
			org.springframework.util.Assert.isTrue("annotationNamedProcessorPropertyValue".equals(annotationNamedProperty));
			org.springframework.util.Assert.isNull(notDefinedProperty);
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty);

			return o;
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
			org.springframework.util.Assert.isTrue("writerPropertyValue1".equals(writerPropertyName1));
			org.springframework.util.Assert.isTrue("writerPropertyValue2".equals(writerPropertyName2));
			org.springframework.util.Assert.isTrue("annotationNamedWriterPropertyValue".equals(annotationNamedProperty));
			org.springframework.util.Assert.isNull(notDefinedProperty);
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty);
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
	}

	public static final class TestCheckpointAlgorithm implements CheckpointAlgorithm {
		@Inject @BatchProperty String algorithmPropertyName1;
		@Inject @BatchProperty String algorithmPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedAlgorithmPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public int checkpointTimeout() throws Exception {
			return 0;
		}

		@Override
		public void beginCheckpoint() throws Exception {
			org.springframework.util.Assert.isTrue("algorithmPropertyValue1".equals(algorithmPropertyName1));
			org.springframework.util.Assert.isTrue("algorithmPropertyValue2".equals(algorithmPropertyName2));
			org.springframework.util.Assert.isTrue("annotationNamedAlgorithmPropertyValue".equals(annotationNamedProperty));
			org.springframework.util.Assert.isNull(notDefinedProperty);
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty);
		}

		@Override
		public boolean isReadyToCheckpoint() throws Exception {
			return true;
		}

		@Override
		public void endCheckpoint() throws Exception {
		}
	}

	public static class TestDecider implements Decider {
		@Inject @BatchProperty String deciderPropertyName1;
		@Inject @BatchProperty String deciderPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedDeciderPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public String decide(javax.batch.runtime.StepExecution[] executions) throws Exception {
			org.springframework.util.Assert.isTrue("deciderPropertyValue1".equals(deciderPropertyName1));
			org.springframework.util.Assert.isTrue("deciderPropertyValue2".equals(deciderPropertyName2));
			org.springframework.util.Assert.isTrue("annotationNamedDeciderPropertyValue".equals(annotationNamedProperty));
			org.springframework.util.Assert.isNull(notDefinedProperty);
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty);

			return "step2";
		}
	}

	public static class TestStepListener implements StepListener {
		@Inject @BatchProperty String stepListenerPropertyName1;
		@Inject @BatchProperty String stepListenerPropertyName2;
		@Inject @BatchProperty(name = "annotationNamedStepListenerPropertyName") String annotationNamedProperty;
		@Inject @BatchProperty String notDefinedProperty;
		@Inject @BatchProperty(name = "notDefinedAnnotationNamedProperty") String notDefinedAnnotationNamedProperty;

		@Override
		public void beforeStep() throws Exception {
			org.springframework.util.Assert.isTrue("stepListenerPropertyValue1".equals(stepListenerPropertyName1));
			org.springframework.util.Assert.isTrue("stepListenerPropertyValue2".equals(stepListenerPropertyName2));
			org.springframework.util.Assert.isTrue("annotationNamedStepListenerPropertyValue".equals(annotationNamedProperty));
			org.springframework.util.Assert.isNull(notDefinedProperty);
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty);
		}

		@Override
		public void afterStep() throws Exception {
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
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName1") == null);
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName2") == null);

			org.springframework.util.Assert.isTrue("batchletPropertyValue1".equals(batchletPropertyName1));
			org.springframework.util.Assert.isTrue("batchletPropertyValue2".equals(batchletPropertyName2));
			org.springframework.util.Assert.isTrue("annotationNamedBatchletPropertyValue".equals(annotationNamedProperty));
			org.springframework.util.Assert.isNull(notDefinedProperty);
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty);

			return null;
		}

		@Override
		public void stop() throws Exception {
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

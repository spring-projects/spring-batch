/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Properties;
import javax.batch.api.BatchProperty;
import javax.batch.api.Batchlet;
import javax.batch.api.Decider;
import javax.batch.api.chunk.CheckpointAlgorithm;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.listener.StepListener;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

import org.junit.Test;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.jsr.AbstractJsrTestCase;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;

import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Configuration test for parsing various &lt;properties /&gt; elements defined by JSR-352.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class JobPropertyTests extends AbstractJsrTestCase {
	@Test
	public void testJobPropertyConfiguration() throws Exception {
		Properties jobParameters = new Properties();
		jobParameters.setProperty("allow.start.if.complete", "true");
		jobParameters.setProperty("deciderName", "stepDecider");
		jobParameters.setProperty("deciderNumber", "1");

		JobExecution jobExecution = runJob("jsrJobPropertyTestsContext", jobParameters, 5000L);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getBatchStatus());
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
		@Inject JobContext injectAnnotatedOnlyField;
		@BatchProperty String batchAnnotatedOnlyField;
		@Inject javax.batch.runtime.context.StepContext stepContext;

		@Override
		public void open(Serializable serializable) throws Exception {
			org.springframework.util.Assert.notNull(stepContext, "stepContext is not null");
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step2PropertyName1"), "step2PropertyName1 is not null");
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step2PropertyName2"), "step2PropertyName2 is not null");
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step1PropertyName1").equals("step1PropertyValue1"), "The value of step2PropertyName1 does not equal step2PropertyName1");
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step1PropertyName2").equals("step1PropertyValue2"), "The value of step2PropertyName2 does not equal step2PropertyName2");
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("jobPropertyName1"), "jobPropertyName1 is not null");
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("jobPropertyName2") == null, "jobPropertyName2 is not null");
			org.springframework.util.Assert.isTrue("readerPropertyValue1".equals(readerPropertyName1), "The value of readerPropertyValue1 does not equal readerPropertyValue1");
			org.springframework.util.Assert.isTrue("readerPropertyValue2".equals(readerPropertyName2), "The value of readerPropertyValue2 does not equal readerPropertyValue2");
			org.springframework.util.Assert.isTrue("annotationNamedReaderPropertyValue".equals(annotationNamedProperty), "The value of annotationNamedReaderPropertyValue does not equal annotationNamedReaderPropertyValue");
			org.springframework.util.Assert.isNull(notDefinedProperty, "notDefinedProperty is not null");
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty, "notDefinedAnnotationNamedProperty is not null");
			org.springframework.util.Assert.isNull(batchAnnotatedOnlyField, "batchAnnotatedOnlyField is not null");
			org.springframework.util.Assert.notNull(injectAnnotatedOnlyField, "injectAnnotatedOnlyField is not null");
			org.springframework.util.Assert.isTrue("job1".equals(injectAnnotatedOnlyField.getJobName()), "injectAnnotatedOnlyField does not equal job1");
			org.springframework.util.Assert.isNull(readerPropertyName3, "readerPropertyName3 is not null");

			Properties jobProperties = injectAnnotatedOnlyField.getProperties();
			org.springframework.util.Assert.isTrue(jobProperties.size() == 5, "jobProperties has the wrong number of values.  Expected 5, got " + jobProperties.size());
			org.springframework.util.Assert.isTrue(jobProperties.get("jobPropertyName1").equals("jobPropertyValue1"), "The value of jobPropertyName1 does not equal jobPropertyName1");
			org.springframework.util.Assert.isTrue(jobProperties.get("jobPropertyName2").equals("jobPropertyValue2"), "The value of jobPropertyName2 does not equal jobPropertyName2");
			org.springframework.util.Assert.isTrue(jobProperties.get("step2name").equals("step2"), "The value of step2name does note equal step2");
			org.springframework.util.Assert.isTrue(jobProperties.get("filestem").equals("postings"), "The value of filestem does not equal postings");
			org.springframework.util.Assert.isTrue(jobProperties.get("x").equals("xVal"), "The value of x does not equal xVal");
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
			org.springframework.util.Assert.isTrue("processorPropertyValue1".equals(processorPropertyName1), "The value of processorPropertyValue1 does not equal processorPropertyValue1");
			org.springframework.util.Assert.isTrue("processorPropertyValue2".equals(processorPropertyName2), "The value of processorPropertyValue2 does not equal processorPropertyValue2");
			org.springframework.util.Assert.isTrue("annotationNamedProcessorPropertyValue".equals(annotationNamedProperty), "The value of annotationNamedProcessorPropertyValue does not equal annotationNamedProcessorPropertyValue");
			org.springframework.util.Assert.isNull(notDefinedProperty, "The notDefinedProperty is not null");
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty, "The notDefinedNamedProperty is not null");

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
			org.springframework.util.Assert.isTrue("writerPropertyValue1".equals(writerPropertyName1), "The value of writerPropertyValue1 does not equal writerPropertyValue1");
			org.springframework.util.Assert.isTrue("writerPropertyValue2".equals(writerPropertyName2), "The value of writerPropertyValue2 does not equal writerPropertyValue2");
			org.springframework.util.Assert.isTrue("annotationNamedWriterPropertyValue".equals(annotationNamedProperty), "The value of annotationNamedWriterPropertyValue does not equal annotationNamedWriterPropertyValue");
			org.springframework.util.Assert.isNull(notDefinedProperty, "notDefinedProperty is not null");
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty, "notDefinedAnnotationNamedProperty is not null");
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
			org.springframework.util.Assert.isTrue("algorithmPropertyValue1".equals(algorithmPropertyName1), "The value of algorithmPropertyValue1 does not equal algorithmPropertyValue1");
			org.springframework.util.Assert.isTrue("algorithmPropertyValue2".equals(algorithmPropertyName2), "The value of algorithmPropertyValue2 does not equal algorithmPropertyValue2");
			org.springframework.util.Assert.isTrue("annotationNamedAlgorithmPropertyValue".equals(annotationNamedProperty), "The annotationNamedAlgorithmPropertyValue does not equal annotationNamedAlgorithmPropertyValue");
			org.springframework.util.Assert.isNull(notDefinedProperty, "notDefinedProperty is not null");
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty, "notDefinedAnnotationNamedProperty is not null");
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
			org.springframework.util.Assert.isTrue("deciderPropertyValue1".equals(deciderPropertyName1), "The value of deciderPropertyValue1 does not equal deciderPropertyValue1");
			org.springframework.util.Assert.isTrue("deciderPropertyValue2".equals(deciderPropertyName2), "The value of deciderPropertyValue2 does not equal deciderPropertyValue2");
			org.springframework.util.Assert.isTrue("annotationNamedDeciderPropertyValue".equals(annotationNamedProperty), "The value of annotationNamedDeciderPropertyValue does not equal annotationNamedDeciderPropertyValue");
			org.springframework.util.Assert.isNull(notDefinedProperty, "notDefinedProperty is not null");
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty, "notDefinedAnnotationNamedProperty is not null");

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
			org.springframework.util.Assert.isTrue("stepListenerPropertyValue1".equals(stepListenerPropertyName1), "The value of stepListenerPropertyValue1 does not equal stepListenerPropertyValue1");
			org.springframework.util.Assert.isTrue("stepListenerPropertyValue2".equals(stepListenerPropertyName2), "The value of stepListenerPropertyValue2 does not equal stepListenerPropertyValue2");
			org.springframework.util.Assert.isTrue("annotationNamedStepListenerPropertyValue".equals(annotationNamedProperty), "The value of annotationNamedStepListenerPropertyValue does note equal annotationNamedStepListenerPropertyValue");
			org.springframework.util.Assert.isNull(notDefinedProperty, "notDefinedProperty is not null");
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty, "notDefinedAnnotationNamedProperty is not null");
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
		@Inject @BatchProperty(name = "infile.name") String infile;
		@Inject @BatchProperty(name = "y") String y;
		@Inject @BatchProperty(name = "x") String x;

		@Override
		public String process() throws Exception {
			org.springframework.util.Assert.notNull(stepContext, "StepContext is not null");
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step1PropertyName1"), "The value of step1PropertyName1 is not null");
			org.springframework.util.Assert.isNull(stepContext.getProperties().get("step1PropertyName2"), "The value of step1PropertyName2 is not null");
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step2PropertyName1").equals("step2PropertyValue1"), "The value of step2PropertyName1 does not equal step2PropertyName1");
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("step2PropertyName2").equals("step2PropertyValue2"), "The value of step2PropertyName2 does not equal step2PropertyName2");
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName1") == null, "jobPropertyName1 is not null");
			org.springframework.util.Assert.isTrue(stepContext.getProperties().get("jobPropertyName2") == null, "jobPropertyName2 is not null");

			org.springframework.util.Assert.isTrue("batchletPropertyValue1".equals(batchletPropertyName1), "batchletPropertyValue1 does not equal batchletPropertyValue1");
			org.springframework.util.Assert.isTrue("batchletPropertyValue2".equals(batchletPropertyName2), "batchletPropertyValue2 does not equal batchletPropertyValue2");
			org.springframework.util.Assert.isTrue("annotationNamedBatchletPropertyValue".equals(annotationNamedProperty), "annotationNamedBatchletPropertyValue does not equal annotationNamedBatchletPropertyValue");
			org.springframework.util.Assert.isTrue("postings.txt".equals(infile), "infile does not equal postings.txt");
			org.springframework.util.Assert.isTrue("xVal".equals(y), "y does not equal xVal");
			org.springframework.util.Assert.isNull(notDefinedProperty, "notDefinedProperty is not null");
			org.springframework.util.Assert.isNull(notDefinedAnnotationNamedProperty, "notDefinedAnnotationNamedProperty is not null");
			org.springframework.util.Assert.isNull(x, "x is not null");

			return null;
		}

		@Override
		public void stop() throws Exception {
		}
	}

	public static class TestTasklet implements Tasklet {
		@Inject
		@BatchProperty
		private String p1;

		@Nullable
		@Override
		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
			org.springframework.util.Assert.isTrue("p1val".equals(p1), "Expected p1val, got " + p1);

			return RepeatStatus.FINISHED;
		}
	}
}

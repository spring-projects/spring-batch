/*
 * Copyright 2013 the original author or authors.
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
import java.util.Collection;
import java.util.List;
import javax.batch.api.chunk.ItemProcessor;
import javax.batch.api.chunk.ItemReader;
import javax.batch.api.chunk.ItemWriter;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.operations.BatchRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.retry.RetryException;
import org.springframework.util.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Test cases around JSR-352 retry listeners.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class RetryListenerTests {
	private static final Log LOG = LogFactory.getLog(RetryListenerTests.class);

	@Test
	@SuppressWarnings("resource")
	public void testReadRetryExhausted() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/batch/core/jsr/configuration/xml/RetryReadListenerExhausted.xml");

		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		JobExecution jobExecution = jobLauncher.run(context.getBean(Job.class), new JobParameters());

		List<Throwable> failureExceptions = jobExecution.getAllFailureExceptions();
		assertTrue("Expected 1 failure exceptions", failureExceptions.size() == 1);
		assertTrue("Failure exception must be of type RetryException", (failureExceptions.get(0) instanceof RetryException));
		assertTrue("Exception cause must be of type IllegalArgumentException", (failureExceptions.get(0).getCause() instanceof IllegalArgumentException));

		assertEquals(ExitStatus.FAILED, jobExecution.getExitStatus());
	}

	@Test
	@SuppressWarnings("resource")
	public void testReadRetryOnce() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/batch/core/jsr/configuration/xml/RetryReadListenerRetryOnce.xml");

		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		JobExecution jobExecution = jobLauncher.run(context.getBean(Job.class), new JobParameters());

		Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
		assertEquals(1, stepExecutions.size());

		StepExecution stepExecution = stepExecutions.iterator().next();
		assertEquals(1, stepExecution.getCommitCount());
		assertEquals(2, stepExecution.getReadCount());

		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Test
	@SuppressWarnings("resource")
	public void testReadRetryExceptionInListener() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("org/springframework/batch/core/jsr/configuration/xml/RetryReadListenerListenerException.xml");

		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		JobExecution jobExecution = jobLauncher.run(context.getBean(Job.class), new JobParameters());

		List<Throwable> failureExceptions = jobExecution.getAllFailureExceptions();
		assertTrue("Failure exceptions must equal one", failureExceptions.size() == 1);
		assertTrue("Failure exception must be of type RetryException", (failureExceptions.get(0) instanceof RetryException));
		assertTrue("Exception cause must be of type BatchRuntimeException", (failureExceptions.get(0).getCause() instanceof BatchRuntimeException));

		assertEquals(ExitStatus.FAILED, jobExecution.getExitStatus());
	}

	public static class ExceptionThrowingRetryReadListener implements RetryReadListener {
		@Override
		public void onRetryReadException(Exception ex) throws Exception {
			Assert.isInstanceOf(IllegalArgumentException.class, ex);
			throw new IllegalStateException();
		}
	}

	public static class TestRetryReadListener implements RetryReadListener {
		@Override
		public void onRetryReadException(Exception ex) throws Exception {
			Assert.isInstanceOf(IllegalArgumentException.class, ex);
		}
	}

	public static class TestRetryProcessListener implements RetryProcessListener {
		@Override
		public void onRetryProcessException(Object item, Exception ex) throws Exception {
			Assert.isInstanceOf(String.class, item);

			String currentItem = (String) item;

			Assert.isTrue("three".equals(currentItem), "currentItem was expected to be three but was not " + currentItem);
			Assert.isInstanceOf(IllegalArgumentException.class, ex);
		}
	}

	public static class TestRetryWriteListener implements RetryWriteListener {
		@Override
		public void onRetryWriteException(List<Object> items, Exception ex) throws Exception {
			Assert.isTrue(items.size() == 2, "Must be two items to write");
			Assert.isTrue(items.contains("three"), "Items must contain the string 'three'");
			Assert.isTrue(items.contains("one"), "Items must contain the string 'one'");
			Assert.isInstanceOf(IllegalArgumentException.class, ex);
		}
	}

	public static class AlwaysFailItemReader implements ItemReader {
		@Override
		public void open(Serializable checkpoint) throws Exception {
		}

		@Override
		public void close() throws Exception {
		}

		@Override
		public Object readItem() throws Exception {
			throw new IllegalArgumentException();
		}

		@Override
		public Serializable checkpointInfo() throws Exception {
			return null;
		}
	}

	public static class FailOnceItemReader implements ItemReader {
		private int cnt;

		@Override
		public void open(Serializable checkpoint) throws Exception {
		}

		@Override
		public void close() throws Exception {
		}

		@Override
		public Object readItem() throws Exception {
			if(cnt == 0) {
				cnt++;
				return "one";
			} else if (cnt == 1) {
				cnt++;
				throw new IllegalArgumentException();
			} else if (cnt == 2) {
				cnt++;
				return "three";
			}

			return null;
		}

		@Override
		public Serializable checkpointInfo() throws Exception {
			return null;
		}
	}

	public static class FailOnceItemProcessor implements ItemProcessor {
		private int cnt;

		@Override
		public Object processItem(Object item) throws Exception {
			if(cnt == 0) {
				cnt++;
				return "one";
			} else if (cnt == 1) {
				cnt++;
				throw new IllegalArgumentException();
			} else if (cnt == 2) {
				cnt++;
				return "three";
			}

			return null;
		}
	}

	public static class FailOnceItemWriter implements ItemWriter {
		private int cnt;

		@Override
		public void open(Serializable checkpoint) throws Exception {
		}

		@Override
		public void close() throws Exception {
		}

		@Override
		public void writeItems(List<Object> items) throws Exception {
			for(@SuppressWarnings("unused") Object item : items) {
				if(cnt == 0) {
					cnt++;
					LOG.info("one");
				} else if (cnt == 1) {
					cnt++;
					throw new IllegalArgumentException();
				} else if (cnt == 2) {
					cnt++;
					LOG.info("three");
				}
			}
		}

		@Override
		public Serializable checkpointInfo() throws Exception {
			return null;
		}
	}
}

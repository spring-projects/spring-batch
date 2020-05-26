/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.core.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class MulticasterBatchListenerTests {

	private MulticasterBatchListener<Integer, String> multicast = new MulticasterBatchListener<>();

	private int count = 0;

	private boolean error = false;

	@Before
	public void setUp() {
		multicast.register(new CountingStepListenerSupport());
	}

	@Test
	public void testSetListeners() {
		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = new StepExecution("s1", jobExecution);
		multicast.setListeners(Arrays.asList(new StepListenerSupport<Integer, String>() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				count++;
				return super.afterStep(stepExecution);
			}
		}));
		multicast.afterStep(stepExecution);
		// setListeners is cumulative (should be OK if used for DI)
		assertEquals(2, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#register(org.springframework.batch.core.StepListener)}
	 * .
	 */
	@Test
	public void testRegister() {
		JobExecution jobExecution = new JobExecution(1L);
		StepExecution stepExecution = new StepExecution("s1", jobExecution);
		multicast.register(new StepListenerSupport<Integer, String>() {
			@Nullable
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				count++;
				return super.afterStep(stepExecution);
			}
		});
		multicast.afterStep(stepExecution);
		assertEquals(2, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterStep(org.springframework.batch.core.StepExecution)}
	 * .
	 */
	@Test
	public void testAfterStepFails() {
		error = true;
		try {
			multicast.afterStep(null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeStep(org.springframework.batch.core.StepExecution)}
	 * .
	 */
	@Test
	public void testBeforeStep() {
		multicast.beforeStep(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeStep(org.springframework.batch.core.StepExecution)}
	 * .
	 */
	@Test
	public void testBeforeStepFails() {
		error = true;
		try {
			multicast.beforeStep(null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterChunk(ChunkContext context)}
	 * .
	 */
	@Test
	public void testAfterChunk() {
		multicast.afterChunk(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterChunk(ChunkContext context)}
	 * .
	 */
	@Test
	public void testAfterChunkFails() {
		error = true;
		try {
			multicast.afterChunk(null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeChunk(ChunkContext context)}
	 * .
	 */
	@Test
	public void testBeforeChunk() {
		multicast.beforeChunk(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeChunk(ChunkContext context)}
	 * .
	 */
	@Test
	public void testBeforeChunkFails() {
		error = true;
		try {
			multicast.beforeChunk(null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterRead(java.lang.Object)}
	 * .
	 */
	@Test
	public void testAfterRead() {
		multicast.afterRead(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterRead(java.lang.Object)}
	 * .
	 */
	@Test
	public void testAfterReadFails() {
		error = true;
		try {
			multicast.afterRead(null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeRead()}
	 * .
	 */
	@Test
	public void testBeforeRead() {
		multicast.beforeRead();
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeRead()}
	 * .
	 */
	@Test
	public void testBeforeReadFails() {
		error = true;
		try {
			multicast.beforeRead();
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onReadError(java.lang.Exception)}
	 * .
	 */
	@Test
	public void testOnReadError() {
		multicast.onReadError(new RuntimeException("foo"));
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onReadError(java.lang.Exception)}
	 * .
	 */
	@Test
	public void testOnReadErrorFails() {
		error = true;
		try {
			multicast.onReadError(new RuntimeException("foo"));
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterWrite(java.util.List)}
	 * .
	 */
	@Test
	public void testAfterWrite() {
		multicast.afterWrite(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterWrite(java.util.List)}
	 * .
	 */
	@Test
	public void testAfterWriteFails() {
		error = true;
		try {
			multicast.afterWrite(null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeWrite(List)}
	 * .
	 */
	@Test
	public void testBeforeWrite() {
		multicast.beforeWrite(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeWrite(List)}
	 * .
	 */
	@Test
	public void testBeforeWriteFails() {
		error = true;
		try {
			multicast.beforeWrite(null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onWriteError(Exception, java.util.List)}
	 * .
	 */
	@Test
	public void testOnWriteError() {
		multicast.onWriteError(new RuntimeException("foo"), null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onWriteError(Exception, java.util.List)}
	 * .
	 */
	@Test
	public void testOnWriteErrorFails() {
		error = true;
		try {
			multicast.onWriteError(new RuntimeException("foo"), null);
			fail("Expected StepListenerFailedException");
		}
		catch (StepListenerFailedException e) {
			// expected
			String message = e.getCause().getMessage();
			assertEquals("Wrong message: " + message, "listener error", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInRead(java.lang.Throwable)}
	 * .
	 */
	@Test
	public void testOnSkipInRead() {
		multicast.register(new SkipListenerSupport<Object,Object>() {
			@Override
			public void onSkipInRead(Throwable t) {
				count++;
				super.onSkipInRead(t);
			}
		});
		multicast.onSkipInRead(new RuntimeException("foo"));
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInRead(java.lang.Throwable)}
	 * .
	 */
	@Test
	public void testOnSkipInReadFails() {
		multicast.register(new SkipListenerSupport<Object,Object>() {
			@Override
			public void onSkipInRead(Throwable t) {
				count++;
				throw new RuntimeException("foo");
			}
		});
		try {
			multicast.onSkipInRead(new RuntimeException("bar"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected
			String message = e.getMessage();
			assertEquals("Wrong message: " + message, "foo", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)}
	 * .
	 */
	@Test
	public void testOnSkipInWrite() {
		multicast.register(new SkipListenerSupport<Object,Object>() {
			@Override
			public void onSkipInWrite(Object item, Throwable t) {
				count++;
				super.onSkipInWrite(item, t);
			}
		});
		multicast.onSkipInWrite(null, new RuntimeException("foo"));
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)}
	 * .
	 */
	@Test
	public void testOnSkipInWriteFails() {
		multicast.register(new SkipListenerSupport<Object,Object>() {
			@Override
			public void onSkipInWrite(Object item, Throwable t) {
				count++;
				throw new RuntimeException("foo");
			}
		});
		try {
			multicast.onSkipInWrite(null, new RuntimeException("bar"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected
			String message = e.getMessage();
			assertEquals("Wrong message: " + message, "foo", message);
		}
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)}
	 * .
	 */
	@Test
	public void testOnSkipInProcess() {
		multicast.register(new SkipListenerSupport<Object,Object>() {
			@Override
			public void onSkipInProcess(Object item, Throwable t) {
				count++;
				super.onSkipInWrite(item, t);
			}
		});
		multicast.onSkipInProcess(null, new RuntimeException("foo"));
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)}
	 * .
	 */
	@Test
	public void testOnSkipInProcessFails() {
		multicast.register(new SkipListenerSupport<Object,Object>() {
			@Override
			public void onSkipInProcess(Object item, Throwable t) {
				count++;
				throw new RuntimeException("foo");
			}
		});
		try {
			multicast.onSkipInProcess(null, new RuntimeException("bar"));
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected
			String message = e.getMessage();
			assertEquals("Wrong message: " + message, "foo", message);
		}
		assertEquals(1, count);
	}

	@Test
	public void testBeforeReadFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.beforeRead();
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	@Test
	public void testAfterReadFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.afterRead(null);
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	@Test
	public void testBeforeProcessFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.beforeProcess(null);
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	@Test
	public void testAfterProcessFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.afterProcess(null, null);
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	@Test
	public void testBeforeWriteFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.beforeWrite(null);
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	@Test
	public void testAfterWriteFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.afterWrite(null);
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	@Test
	public void testBeforeChunkFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.beforeChunk(null);
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	@Test
	public void testAfterChunkFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		try {
			multicast.afterChunk(null);
			fail("Expected StepListenerFailedException");
		} catch (StepListenerFailedException e) {
			// expected
			Throwable cause = e.getCause();
			String message = cause.getMessage();
			assertTrue(cause instanceof IllegalStateException);
			assertEquals("Wrong message: " + message, "listener error", message);
		}
	}

	private final class AnnotationBasedStepListener {

		private IllegalStateException exception = new IllegalStateException("listener error");

		@BeforeRead
		public void beforeRead() {
			throw exception;
		}

		@AfterRead
		public void afterRead() {
			throw exception;
		}

		@BeforeProcess
		public void beforeProcess() {
			throw exception;
		}

		@AfterProcess
		public void afterProcess() {
			throw exception;
		}

		@BeforeWrite
		public void beforeWrite() {
			throw exception;
		}

		@AfterWrite
		public void afterWrite() {
			throw exception;
		}

		@BeforeChunk
		public void beforeChunk() {
			throw exception;
		}

		@AfterChunk
		public void afterChunk() {
			throw exception;
		}

	}

	/**
	 * @author Dave Syer
	 *
	 */
	private final class CountingStepListenerSupport extends StepListenerSupport<Integer, String> {
		@Override
		public void onReadError(Exception ex) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.onReadError(ex);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#afterChunk
		 * ()
		 */
		@Override
		public void afterChunk(ChunkContext context) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.afterChunk(context);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#afterRead
		 * (java.lang.Object)
		 */
		@Override
		public void afterRead(Integer item) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.afterRead(item);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#afterStep
		 * (org.springframework.batch.core.StepExecution)
		 */
		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			return super.afterStep(stepExecution);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#beforeChunk
		 * ()
		 */
		@Override
		public void beforeChunk(ChunkContext context) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeChunk(context);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#beforeRead
		 * ()
		 */
		@Override
		public void beforeRead() {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeRead();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#beforeStep
		 * (org.springframework.batch.core.StepExecution)
		 */
		@Override
		public void beforeStep(StepExecution stepExecution) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeStep(stepExecution);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#afterWrite
		 * (java.util.List)
		 */
		@Override
		public void afterWrite(List<? extends String> items) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.afterWrite(items);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#beforeWrite
		 * (java.util.List)
		 */
		@Override
		public void beforeWrite(List<? extends String> items) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeWrite(items);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.springframework.batch.core.listener.StepListenerSupport#onWriteError
		 * (java.lang.Exception, java.util.List)
		 */
		@Override
		public void onWriteError(Exception exception, List<? extends String> items) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.onWriteError(exception, items);
		}

	}

}

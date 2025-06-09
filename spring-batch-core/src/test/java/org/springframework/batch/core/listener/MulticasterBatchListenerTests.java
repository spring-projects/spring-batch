/*
 * Copyright 2006-2025 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.Chunk;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class MulticasterBatchListenerTests {

	private final MulticasterBatchListener<Integer, String> multicast = new MulticasterBatchListener<>();

	private int count = 0;

	private boolean error = false;

	@BeforeEach
	void setUp() {
		multicast.register(new CountingStepListenerSupport());
	}

	@Test
	void testSetListeners() {
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
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#register(StepListener)}
	 * .
	 */
	@Test
	void testRegister() {
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
	void testAfterStepFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterStep(null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeStep(org.springframework.batch.core.StepExecution)}
	 * .
	 */
	@Test
	void testBeforeStep() {
		multicast.beforeStep(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeStep(org.springframework.batch.core.StepExecution)}
	 * .
	 */
	@Test
	void testBeforeStepFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.beforeStep(null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterChunk(ChunkContext context)}
	 * .
	 */
	@Test
	void testAfterChunk() {
		multicast.afterChunk(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterChunk(ChunkContext context)}
	 * .
	 */
	@Test
	void testAfterChunkFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterChunk(null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeChunk(ChunkContext context)}
	 * .
	 */
	@Test
	void testBeforeChunk() {
		multicast.beforeChunk(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeChunk(ChunkContext context)}
	 * .
	 */
	@Test
	void testBeforeChunkFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.beforeChunk(null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterRead(java.lang.Object)}
	 * .
	 */
	@Test
	void testAfterRead() {
		multicast.afterRead(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterRead(java.lang.Object)}
	 * .
	 */
	@Test
	void testAfterReadFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterRead(null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeRead()}
	 * .
	 */
	@Test
	void testBeforeRead() {
		multicast.beforeRead();
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeRead()}
	 * .
	 */
	@Test
	void testBeforeReadFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, multicast::beforeRead);
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onReadError(java.lang.Exception)}
	 * .
	 */
	@Test
	void testOnReadError() {
		multicast.onReadError(new RuntimeException("foo"));
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onReadError(java.lang.Exception)}
	 * .
	 */
	@Test
	void testOnReadErrorFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class,
				() -> multicast.onReadError(new RuntimeException("foo")));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterWrite(Chunk)}
	 * .
	 */
	@Test
	void testAfterWrite() {
		multicast.afterWrite(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#afterWrite(Chunk)}
	 * .
	 */
	@Test
	void testAfterWriteFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterWrite(null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeWrite(Chunk)}
	 * .
	 */
	@Test
	void testBeforeWrite() {
		multicast.beforeWrite(null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#beforeWrite(Chunk)}
	 * .
	 */
	@Test
	void testBeforeWriteFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.beforeWrite(null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onWriteError(Exception, Chunk)}
	 * .
	 */
	@Test
	void testOnWriteError() {
		multicast.onWriteError(new RuntimeException("foo"), null);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onWriteError(Exception, Chunk)}
	 * .
	 */
	@Test
	void testOnWriteErrorFails() {
		error = true;
		Exception exception = assertThrows(StepListenerFailedException.class,
				() -> multicast.onWriteError(new RuntimeException("foo"), null));
		String message = exception.getCause().getMessage();
		assertEquals("listener error", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInRead(java.lang.Throwable)}
	 * .
	 */
	@Test
	void testOnSkipInRead() {
		multicast.register(new SkipListener<>() {
			@Override
			public void onSkipInRead(Throwable t) {
				count++;
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
	void testOnSkipInReadFails() {
		multicast.register(new SkipListener<>() {
			@Override
			public void onSkipInRead(Throwable t) {
				count++;
				throw new RuntimeException("foo");
			}
		});
		Exception exception = assertThrows(RuntimeException.class,
				() -> multicast.onSkipInRead(new RuntimeException("bar")));
		String message = exception.getMessage();
		assertEquals("foo", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)}
	 * .
	 */
	@Test
	void testOnSkipInWrite() {
		multicast.register(new SkipListener<>() {
			@Override
			public void onSkipInWrite(Object item, Throwable t) {
				count++;
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
	void testOnSkipInWriteFails() {
		multicast.register(new SkipListener<>() {
			@Override
			public void onSkipInWrite(Object item, Throwable t) {
				count++;
				throw new RuntimeException("foo");
			}
		});
		Exception exception = assertThrows(RuntimeException.class,
				() -> multicast.onSkipInWrite(null, new RuntimeException("bar")));
		String message = exception.getMessage();
		assertEquals("foo", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.listener.MulticasterBatchListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)}
	 * .
	 */
	@Test
	void testOnSkipInProcess() {
		multicast.register(new SkipListener<>() {
			@Override
			public void onSkipInProcess(Object item, Throwable t) {
				count++;
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
	void testOnSkipInProcessFails() {
		multicast.register(new SkipListener<>() {
			@Override
			public void onSkipInProcess(Object item, Throwable t) {
				count++;
				throw new RuntimeException("foo");
			}
		});
		Exception exception = assertThrows(RuntimeException.class,
				() -> multicast.onSkipInProcess(null, new RuntimeException("bar")));
		String message = exception.getMessage();
		assertEquals("foo", message, "Wrong message: " + message);
		assertEquals(1, count);
	}

	@Test
	void testBeforeReadFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, multicast::beforeRead);
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	@Test
	void testAfterReadFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterRead(null));
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	@Test
	void testBeforeProcessFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.beforeProcess(null));
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	@Test
	void testAfterProcessFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterProcess(null, null));
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	@Test
	void testBeforeWriteFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.beforeWrite(null));
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	@Test
	void testAfterWriteFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterWrite(null));
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	@Test
	void testBeforeChunkFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.beforeChunk(null));
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	@Test
	void testAfterChunkFails_withAnnotatedListener() {
		StepListener listener = StepListenerFactoryBean.getListener(new AnnotationBasedStepListener());
		multicast.register(listener);

		Exception exception = assertThrows(StepListenerFailedException.class, () -> multicast.afterChunk(null));
		Throwable cause = exception.getCause();
		String message = cause.getMessage();
		assertInstanceOf(IllegalStateException.class, cause);
		assertEquals("listener error", message, "Wrong message: " + message);
	}

	private static final class AnnotationBasedStepListener {

		private final IllegalStateException exception = new IllegalStateException("listener error");

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

		@Override
		public void afterChunk(ChunkContext context) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.afterChunk(context);
		}

		@Override
		public void afterRead(Integer item) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.afterRead(item);
		}

		@Nullable
		@Override
		public ExitStatus afterStep(StepExecution stepExecution) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			return super.afterStep(stepExecution);
		}

		@Override
		public void beforeChunk(ChunkContext context) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeChunk(context);
		}

		@Override
		public void beforeRead() {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeRead();
		}

		@Override
		public void beforeStep(StepExecution stepExecution) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeStep(stepExecution);
		}

		@Override
		public void afterWrite(Chunk<? extends String> items) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.afterWrite(items);
		}

		@Override
		public void beforeWrite(Chunk<? extends String> items) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.beforeWrite(items);
		}

		@Override
		public void onWriteError(Exception exception, Chunk<? extends String> items) {
			count++;
			if (error) {
				throw new RuntimeException("listener error");
			}
			super.onWriteError(exception, items);
		}

	}

}

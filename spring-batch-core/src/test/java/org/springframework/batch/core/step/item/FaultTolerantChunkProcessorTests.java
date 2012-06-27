package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.dao.DataIntegrityViolationException;

public class FaultTolerantChunkProcessorTests {

	private BatchRetryTemplate batchRetryTemplate;

	private List<String> list = new ArrayList<String>();

	private List<String> after = new ArrayList<String>();

	private List<String> writeError = new ArrayList<String>();

	private FaultTolerantChunkProcessor<String, String> processor;

	private StepContribution contribution = new StepExecution("foo",
			new JobExecution(0L)).createStepContribution();

	@Before
	public void setUp() {
		batchRetryTemplate = new BatchRetryTemplate();
		processor = new FaultTolerantChunkProcessor<String, String>(
				new PassThroughItemProcessor<String>(),
				new ItemWriter<String>() {
					public void write(List<? extends String> items)
							throws Exception {
						if (items.contains("fail")) {
							throw new RuntimeException("Planned failure!");
						}
						list.addAll(items);
					}
				}, batchRetryTemplate);
		batchRetryTemplate.setRetryPolicy(new NeverRetryPolicy());
	}

	@Test
	public void testWrite() throws Exception {
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("1", "2"));
		processor.process(contribution, inputs);
		assertEquals(2, list.size());
	}

	@Test
	public void testTransform() throws Exception {
		processor.setItemProcessor(new ItemProcessor<String, String>() {
			public String process(String item) throws Exception {
				return item.equals("1") ? null : item;
			}
		});
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("1", "2"));
		processor.process(contribution, inputs);
		assertEquals(1, list.size());
		assertEquals(1, contribution.getFilterCount());
	}

	@Test
	public void testFilterCountOnSkip() throws Exception {
		processor.setProcessSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processor.setItemProcessor(new ItemProcessor<String, String>() {
			public String process(String item) throws Exception {
				if (item.equals("1")) {
					throw new RuntimeException("Skippable");
				}
				if (item.equals("3")) {
					return null;
				}
				return item;
			}
		});
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("3", "1", "2"));
		try {
			processor.process(contribution, inputs);
			fail("Expected Exception");
		} catch (Exception e) {
			assertEquals("Skippable", e.getMessage());
		}
		processor.process(contribution, inputs);
		assertEquals(1, list.size());
		assertEquals(1, contribution.getSkipCount());
		assertEquals(1, contribution.getFilterCount());
	}

	/**
	 * An Error can be retried or skipped but by default it is just propagated
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteSkipOnError() throws Exception {
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processor.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				if (items.contains("fail")) {
					assertFalse("Expected Error!", true);
				}
			}
		});
		Chunk<String> inputs = new Chunk<String>(
				Arrays.asList("3", "fail", "2"));
		try {
			processor.process(contribution, inputs);
			fail("Expected Error");
		} catch (Error e) {
			assertEquals("Expected Error!", e.getMessage());
		}
		processor.process(contribution, inputs);
	}

	@Test
	public void testWriteSkipOnException() throws Exception {
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processor.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				if (items.contains("fail")) {
					throw new RuntimeException("Expected Exception!");
				}
			}
		});
		Chunk<String> inputs = new Chunk<String>(
				Arrays.asList("3", "fail", "2"));
		try {
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		processor.process(contribution, inputs);
		try {
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		assertEquals(1, contribution.getSkipCount());
		assertEquals(1, contribution.getWriteCount());
		assertEquals(0, contribution.getFilterCount());
	}

	@Test
	public void testWriteSkipOnExceptionWithTrivialChunk() throws Exception {
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processor.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				if (items.contains("fail")) {
					throw new RuntimeException("Expected Exception!");
				}
			}
		});
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("fail"));
		try {
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		// BATCH-1518: ideally we would not want this to be necessary, but it
		// still is...
		try {
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		processor.process(contribution, inputs);
		assertEquals(1, contribution.getSkipCount());
		assertEquals(0, contribution.getWriteCount());
		assertEquals(0, contribution.getFilterCount());
	}

	@Test
	public void testTransformWithExceptionAndNoRollback() throws Exception {
		processor.setItemProcessor(new ItemProcessor<String, String>() {
			public String process(String item) throws Exception {
				if (item.equals("1"))
					throw new DataIntegrityViolationException("Planned");
				return item;
			}
		});
		processor.setProcessSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processor
				.setRollbackClassifier(new BinaryExceptionClassifier(
						Collections
								.<Class<? extends Throwable>> singleton(DataIntegrityViolationException.class),
						false));
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("1", "2"));
		processor.process(contribution, inputs);
		assertEquals(1, list.size());
	}

	@Test
	public void testAfterWrite() throws Exception {
		Chunk<String> chunk = new Chunk<String>(Arrays.asList("foo", "fail",
				"bar"));
		processor.setListeners(Arrays
				.asList(new ItemListenerSupport<String, String>() {
					@Override
					public void afterWrite(List<? extends String> item) {
						after.addAll(item);
					}
				}));
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processAndExpectPlannedRuntimeException(chunk);
		processor.process(contribution, chunk);
		assertEquals(2, chunk.getItems().size());
		processAndExpectPlannedRuntimeException(chunk);
		assertEquals(1, chunk.getItems().size());
		processor.process(contribution, chunk);
		assertEquals(0, chunk.getItems().size());
		// foo is written once because it the failure is detected before it is
		// committed the first time
		assertEquals("[foo, bar]", list.toString());
		// the after listener is called once per successful item, which is
		// important
		assertEquals("[foo, bar]", after.toString());
	}

	@Test
	public void testAfterWriteAllPassedInRecovery() throws Exception {
		Chunk<String> chunk = new Chunk<String>(Arrays.asList("foo", "bar"));
		processor = new FaultTolerantChunkProcessor<String, String>(
				new PassThroughItemProcessor<String>(),
				new ItemWriter<String>() {
					public void write(List<? extends String> items)
							throws Exception {
						// Fail if there is more than one item
						if (items.size() > 1) {
							throw new RuntimeException("Planned failure!");
						}
						list.addAll(items);
					}
				}, batchRetryTemplate);
		processor.setListeners(Arrays
				.asList(new ItemListenerSupport<String, String>() {
					@Override
					public void afterWrite(List<? extends String> item) {
						after.addAll(item);
					}
				}));
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());

		processAndExpectPlannedRuntimeException(chunk);
		processor.process(contribution, chunk);
		processor.process(contribution, chunk);

		assertEquals("[foo, bar]", list.toString());
		assertEquals("[foo, bar]", after.toString());
	}

	@Test
	public void testOnErrorInWrite() throws Exception {
		Chunk<String> chunk = new Chunk<String>(Arrays.asList("foo", "fail"));
		processor.setListeners(Arrays
				.asList(new ItemListenerSupport<String, String>() {
					@Override
					public void onWriteError(Exception e,
							List<? extends String> item) {
						writeError.addAll(item);
					}
				}));
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());

		processAndExpectPlannedRuntimeException(chunk);// Process foo, fail
		processor.process(contribution, chunk);
		;// Process foo
		processAndExpectPlannedRuntimeException(chunk);// Process fail

		assertEquals("[foo, fail, fail]", writeError.toString());
	}

	@Test
	public void testOnErrorInWriteAllItemsFail() throws Exception {
		Chunk<String> chunk = new Chunk<String>(Arrays.asList("foo", "bar"));
		processor = new FaultTolerantChunkProcessor<String, String>(
				new PassThroughItemProcessor<String>(),
				new ItemWriter<String>() {
					public void write(List<? extends String> items)
							throws Exception {
						// Always fail in writer
						throw new RuntimeException("Planned failure!");
					}
				}, batchRetryTemplate);
		processor.setListeners(Arrays
				.asList(new ItemListenerSupport<String, String>() {
					@Override
					public void onWriteError(Exception e,
							List<? extends String> item) {
						writeError.addAll(item);
					}
				}));
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());

		processAndExpectPlannedRuntimeException(chunk);// Process foo, bar
		processAndExpectPlannedRuntimeException(chunk);// Process foo
		processAndExpectPlannedRuntimeException(chunk);// Process bar

		assertEquals("[foo, bar, foo, bar]", writeError.toString());
	}

	@Test
	public void testWriteRetryOnException() throws Exception {
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(2);
		batchRetryTemplate.setRetryPolicy(retryPolicy);
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processor.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				if (items.contains("fail")) {
					throw new IllegalArgumentException("Expected Exception!");
				}
			}
		});
		Chunk<String> inputs = new Chunk<String>(
				Arrays.asList("3", "fail", "2"));
		try {
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		try {
			// first retry
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		// retry exhausted, now scanning
		processor.process(contribution, inputs);
		try {
			// skip on this attempt
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		// finish chunk
		processor.process(contribution, inputs);
		assertEquals(1, contribution.getSkipCount());
		assertEquals(2, contribution.getWriteCount());
		assertEquals(0, contribution.getFilterCount());
	}

	@Test
	public void testWriteRetryOnTwoExceptions() throws Exception {
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(2);
		batchRetryTemplate.setRetryPolicy(retryPolicy);
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());
		processor.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				if (items.contains("fail")) {
					throw new IllegalArgumentException("Expected Exception!");
				}
			}
		});
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("3", "fail",
				"fail", "4"));
		try {
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		try {
			// first retry
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		// retry exhausted, now scanning
		processor.process(contribution, inputs);
		try {
			// skip on this attempt
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		try {
			// 2nd exception detected
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RuntimeException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		// still scanning
		processor.process(contribution, inputs);
		assertEquals(2, contribution.getSkipCount());
		assertEquals(2, contribution.getWriteCount());
		assertEquals(0, contribution.getFilterCount());
	}

	@Test
	// BATCH-1804
	public void testWriteRetryOnNonSkippableException() throws Exception {
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
		retryPolicy.setMaxAttempts(2);
		batchRetryTemplate.setRetryPolicy(retryPolicy);
		processor.setWriteSkipPolicy(new LimitCheckingItemSkipPolicy(1,
				Collections.<Class<? extends Throwable>, Boolean> singletonMap(
						IllegalArgumentException.class, true)));
		processor.setItemWriter(new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				if (items.contains("fail")) {
					throw new IllegalArgumentException("Expected Exception!");
				}
				if (items.contains("2")) {
					throw new RuntimeException(
							"Expected Non-Skippable Exception!");
				}
			}
		});
		Chunk<String> inputs = new Chunk<String>(
				Arrays.asList("3", "fail", "2"));
		try {
			processor.process(contribution, inputs);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		try {
			// first retry
			processor.process(contribution, inputs);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		// retry exhausted, now scanning
		processor.process(contribution, inputs);
		try {
			// skip on this attempt
			processor.process(contribution, inputs);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			assertEquals("Expected Exception!", e.getMessage());
		}
		try {
			// should retry
			processor.process(contribution, inputs);
			fail("Expected RuntimeException");
		} catch (RetryException e) {
			throw e;
		} catch (RuntimeException e) {
			assertEquals("Expected Non-Skippable Exception!", e.getMessage());
		}
		assertEquals(1, contribution.getSkipCount());
		assertEquals(1, contribution.getWriteCount());
		assertEquals(0, contribution.getFilterCount());
	}

	protected void processAndExpectPlannedRuntimeException(Chunk<String> chunk)
			throws Exception {
		try {
			processor.process(contribution, chunk);
			fail();
		} catch (RuntimeException e) {
			assertEquals("Planned failure!", e.getMessage());
		}
	}
}

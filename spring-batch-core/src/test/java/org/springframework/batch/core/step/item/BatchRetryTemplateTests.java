package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryState;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.batch.retry.support.DefaultRetryState;

public class BatchRetryTemplateTests {

	private static class RecoverableException extends Exception {

		public RecoverableException(String message) {
			super(message);
		}

	}

	private int count = 0;

	private List<String> outputs = new ArrayList<String>();

	@Test
	public void testSuccessfulAttempt() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();

		String result = template.execute(new RetryCallback<String>() {
			public String doWithRetry(RetryContext context) throws Exception {
				assertTrue("Wrong context type: " + context.getClass().getSimpleName(), context.getClass()
						.getSimpleName().contains("Batch"));
				return "2";
			}
		}, Arrays.<RetryState> asList(new DefaultRetryState("1")));

		assertEquals("2", result);

	}

	@Test
	public void testUnSuccessfulAttemptAndRetry() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();

		RetryCallback<String[]> retryCallback = new RetryCallback<String[]>() {
			public String[] doWithRetry(RetryContext context) throws Exception {
				assertEquals(count, context.getRetryCount());
				if (count++ == 0) {
					throw new RecoverableException("Recoverable");
				}
				return new String[] { "a", "b" };
			}
		};

		List<RetryState> states = Arrays.<RetryState> asList(new DefaultRetryState("1"), new DefaultRetryState("2"));
		try {
			template.execute(retryCallback, states);
			fail("Expected RecoverableException");
		}
		catch (RecoverableException e) {
			assertEquals("Recoverable", e.getMessage());
		}
		String[] result = template.execute(retryCallback, states);

		assertEquals("[a, b]", Arrays.toString(result));

	}

	@Test(expected = ExhaustedRetryException.class)
	public void testExhaustedRetry() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();
		template.setRetryPolicy(new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));

		RetryCallback<String[]> retryCallback = new RetryCallback<String[]>() {
			public String[] doWithRetry(RetryContext context) throws Exception {
				if (count++ < 2) {
					throw new RecoverableException("Recoverable");
				}
				return outputs.toArray(new String[0]);
			}
		};

		outputs = Arrays.asList("a", "b");
		try {
			template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
			fail("Expected RecoverableException");
		}
		catch (RecoverableException e) {
			assertEquals("Recoverable", e.getMessage());
		}
		outputs = Arrays.asList("a", "c");
		template.execute(retryCallback, BatchRetryTemplate.createState(outputs));

	}

	@Test
	public void testExhaustedRetryAfterShuffle() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();
		template.setRetryPolicy(new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));

		RetryCallback<String[]> retryCallback = new RetryCallback<String[]>() {
			public String[] doWithRetry(RetryContext context) throws Exception {
				if (count++ < 1) {
					throw new RecoverableException("Recoverable");
				}
				return outputs.toArray(new String[0]);
			}
		};

		outputs = Arrays.asList("a", "b");
		try {
			template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
			fail("Expected RecoverableException");
		}
		catch (RecoverableException e) {
			assertEquals("Recoverable", e.getMessage());
		}

		outputs = Arrays.asList("b", "c");
		try {
			template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
			fail("Expected ExhaustedRetryException");
		}
		catch (ExhaustedRetryException e) {
		}

		// "c" is not tarred with same brush as "b" because it was never
		// processed on account of the exhausted retry
		outputs = Arrays.asList("d", "c");
		String[] result = template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
		assertEquals("[d, c]", Arrays.toString(result));

		// "a" is still marked as a failure from the first chunk
		outputs = Arrays.asList("a", "e");
		try {
			template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
			fail("Expected ExhaustedRetryException");
		}
		catch (ExhaustedRetryException e) {
		}

		outputs = Arrays.asList("e", "f");
		result = template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
		assertEquals("[e, f]", Arrays.toString(result));

	}

	@Test
	public void testExhaustedRetryWithRecovery() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();
		template.setRetryPolicy(new SimpleRetryPolicy(1, Collections
				.<Class<? extends Throwable>, Boolean> singletonMap(Exception.class, true)));

		RetryCallback<String[]> retryCallback = new RetryCallback<String[]>() {
			public String[] doWithRetry(RetryContext context) throws Exception {
				if (count++ < 2) {
					throw new RecoverableException("Recoverable");
				}
				return outputs.toArray(new String[0]);
			}
		};

		RecoveryCallback<String[]> recoveryCallback = new RecoveryCallback<String[]>() {
			public String[] recover(RetryContext context) throws Exception {
				List<String> recovered = new ArrayList<String>();
				for (String item : outputs) {
					recovered.add("r:" + item);
				}
				return recovered.toArray(new String[0]);
			}
		};

		outputs = Arrays.asList("a", "b");
		try {
			template.execute(retryCallback, recoveryCallback, BatchRetryTemplate.createState(outputs));
			fail("Expected RecoverableException");
		}
		catch (RecoverableException e) {
			assertEquals("Recoverable", e.getMessage());
		}

		outputs = Arrays.asList("b", "c");
		String[] result = template.execute(retryCallback, recoveryCallback, BatchRetryTemplate.createState(outputs));
		assertEquals("[r:b, r:c]", Arrays.toString(result));

	}

}

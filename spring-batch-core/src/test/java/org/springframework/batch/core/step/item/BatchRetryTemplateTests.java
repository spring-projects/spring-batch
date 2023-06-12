/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.junit.jupiter.api.Test;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryState;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.DefaultRetryState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchRetryTemplateTests {

	private static class RecoverableException extends Exception {

		public RecoverableException(String message) {
			super(message);
		}

	}

	private int count = 0;

	private List<String> outputs = new ArrayList<>();

	@Test
	void testSuccessfulAttempt() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();

		String result = template.execute((RetryCallback<String, Exception>) context -> {
			assertTrue(context.getClass().getSimpleName().contains("Batch"),
					"Wrong context type: " + context.getClass().getSimpleName());
			return "2";
		}, Arrays.<RetryState>asList(new DefaultRetryState("1")));

		assertEquals("2", result);

	}

	@Test
	void testUnSuccessfulAttemptAndRetry() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();

		RetryCallback<String[], Exception> retryCallback = context -> {
			assertEquals(count, context.getRetryCount());
			if (count++ == 0) {
				throw new RecoverableException("Recoverable");
			}
			return new String[] { "a", "b" };
		};

		List<RetryState> states = Arrays.<RetryState>asList(new DefaultRetryState("1"), new DefaultRetryState("2"));
		Exception exception = assertThrows(RecoverableException.class, () -> template.execute(retryCallback, states));
		assertEquals("Recoverable", exception.getMessage());
		String[] result = template.execute(retryCallback, states);

		assertEquals("[a, b]", Arrays.toString(result));

	}

	@Test
	void testExhaustedRetry() {

		BatchRetryTemplate template = new BatchRetryTemplate();
		template.setRetryPolicy(new SimpleRetryPolicy(1,
				Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true)));

		RetryCallback<String[], Exception> retryCallback = context -> {
			if (count++ < 2) {
				throw new RecoverableException("Recoverable");
			}
			return outputs.toArray(new String[0]);
		};

		outputs = List.of("a", "b");
		Exception exception = assertThrows(RecoverableException.class,
				() -> template.execute(retryCallback, BatchRetryTemplate.createState(outputs)));
		assertEquals("Recoverable", exception.getMessage());
		outputs = List.of("a", "c");
		assertThrows(ExhaustedRetryException.class,
				() -> template.execute(retryCallback, BatchRetryTemplate.createState(outputs)));
	}

	@Test
	void testExhaustedRetryAfterShuffle() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();
		template.setRetryPolicy(new SimpleRetryPolicy(1,
				Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true)));

		RetryCallback<String[], Exception> retryCallback = context -> {
			if (count++ < 1) {
				throw new RecoverableException("Recoverable");
			}
			return outputs.toArray(new String[0]);
		};

		outputs = Arrays.asList("a", "b");
		Exception exception = assertThrows(RecoverableException.class,
				() -> template.execute(retryCallback, BatchRetryTemplate.createState(outputs)));
		assertEquals("Recoverable", exception.getMessage());

		outputs = Arrays.asList("b", "c");
		assertThrows(ExhaustedRetryException.class,
				() -> template.execute(retryCallback, BatchRetryTemplate.createState(outputs)));

		// "c" is not tarred with same brush as "b" because it was never
		// processed on account of the exhausted retry
		outputs = Arrays.asList("d", "c");
		String[] result = template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
		assertEquals("[d, c]", Arrays.toString(result));

		// "a" is still marked as a failure from the first chunk
		outputs = Arrays.asList("a", "e");
		assertThrows(ExhaustedRetryException.class,
				() -> template.execute(retryCallback, BatchRetryTemplate.createState(outputs)));

		outputs = Arrays.asList("e", "f");
		result = template.execute(retryCallback, BatchRetryTemplate.createState(outputs));
		assertEquals("[e, f]", Arrays.toString(result));

	}

	@Test
	void testExhaustedRetryWithRecovery() throws Exception {

		BatchRetryTemplate template = new BatchRetryTemplate();
		template.setRetryPolicy(new SimpleRetryPolicy(1,
				Collections.<Class<? extends Throwable>, Boolean>singletonMap(Exception.class, true)));

		RetryCallback<String[], Exception> retryCallback = context -> {
			if (count++ < 2) {
				throw new RecoverableException("Recoverable");
			}
			return outputs.toArray(new String[0]);
		};

		RecoveryCallback<String[]> recoveryCallback = context -> {
			List<String> recovered = new ArrayList<>();
			for (String item : outputs) {
				recovered.add("r:" + item);
			}
			return recovered.toArray(new String[0]);
		};

		outputs = Arrays.asList("a", "b");
		Exception exception = assertThrows(RecoverableException.class,
				() -> template.execute(retryCallback, recoveryCallback, BatchRetryTemplate.createState(outputs)));
		assertEquals("Recoverable", exception.getMessage());

		outputs = Arrays.asList("b", "c");
		String[] result = template.execute(retryCallback, recoveryCallback, BatchRetryTemplate.createState(outputs));
		assertEquals("[r:b, r:c]", Arrays.toString(result));

	}

}

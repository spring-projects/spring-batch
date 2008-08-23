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
package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Dave Syer
 * 
 */
@RunWith(Parameterized.class)
public class ChunkTests {

	private enum CallType {
		RUN, RETRY;
	}

	private Log logger = LogFactory.getLog(getClass());

	private final Chunk<String> chunk;

	private final int retryLimit;

	private int retryAttempts = 0;

	private static final int BACKSTOP_LIMIT = 1000;

	private int count = 0;

	private Object lastCallType;

	public ChunkTests(String[] args, int limit) {
		chunk = new Chunk<String>();
		for (String string : args) {
			chunk.add(string);
		}
		this.retryLimit = limit;
	}

	@Test
	public void testSimpleScenario() throws Exception {
		logger.debug("Starting simple scenario");
		List<String> items = new ArrayList<String>(chunk.getItems());
		items.removeAll(Collections.singleton("fail"));
		boolean error = true;
		while (error && count++ < BACKSTOP_LIMIT) {
			try {
				if (!chunk.canRetry()) {
					// success
					logger.debug("Run items: " + chunk.getItems());
					lastCallType = CallType.RUN;
					runChunk(chunk);
				}
				else {
					logger.debug(String.format("Retry (attempts=%d) items: %s", retryAttempts, chunk.getItems()));
					lastCallType = CallType.RETRY;
					try {
						retryChunk(chunk);
					}
					catch (Exception e) {
						chunk.rethrow(e);
					}

				}
				chunk.rethrow();
				error = false;
			}
			catch (Exception e) {
				error = true;
			}
		}
		logger.debug("Items: " + chunk.getItems());
		assertTrue("Backstop reached.  Probably an infinite loop...", count < BACKSTOP_LIMIT);
		assertEquals(CallType.RETRY, lastCallType);
		assertFalse(chunk.getItems().contains("fail"));
		assertEquals(items, chunk.getItems());
	}

	/**
	 * @param chunk
	 * @throws Exception
	 */
	private void retryChunk(Chunk<String> chunk) throws Exception {
		try {
			doWrite(chunk);
			retryAttempts = 0;
		}
		catch (Exception e) {
			if (++retryAttempts > retryLimit) {
				// recovery
				retryAttempts = 0;
				if (chunk.canSkip()) {
					chunk.getSkippedItem();
				} else {
					throw e;
				}
			}
			else {
				// stateful retry always rethrow
				throw e;
			}
		}
	}

	/**
	 * @param chunk
	 * @throws Exception
	 */
	private void runChunk(Chunk<String> chunk) throws Exception {
		try {
			doWrite(chunk);
		}
		catch (Exception e) {
			chunk.rethrow(e);
		}
	}

	/**
	 * @param chunk
	 * @throws Exception
	 */
	private void doWrite(Chunk<String> chunk) throws Exception {
		List<String> items = chunk.getItems();
		if (items.contains("fail")) {
			throw new Exception();
		}
	}

	@Parameters
	public static List<Object[]> data() {
		List<Object[]> params = new ArrayList<Object[]>();
		params.add(new Object[] { new String[] { "foo" }, 0 });
		params.add(new Object[] { new String[] { "foo", "bar" }, 0 });
		params.add(new Object[] { new String[] { "foo", "bar", "spam" }, 0 });
		params.add(new Object[] { new String[] { "foo", "bar", "spam", "maps", "rab", "oof" }, 0 });
		params.add(new Object[] { new String[] { "fail" }, 0 });
		params.add(new Object[] { new String[] { "foo", "fail" }, 0 });
		params.add(new Object[] { new String[] { "fail", "bar" }, 0 });
		params.add(new Object[] { new String[] { "foo", "fail", "spam" }, 0 });
		params.add(new Object[] { new String[] { "fail", "bar", "spam" }, 0 });
		params.add(new Object[] { new String[] { "foo", "fail", "spam", "maps", "rab", "oof" }, 0 });
		params.add(new Object[] { new String[] { "foo", "fail", "spam", "fail", "rab", "oof" }, 0 });
		params.add(new Object[] { new String[] { "fail", "bar", "spam", "fail", "rab", "oof" }, 0 });
		params.add(new Object[] { new String[] { "foo", "fail", "fail", "fail", "rab", "oof" }, 0 });
		params.add(new Object[] { new String[] { "fail" }, 1 });
		params.add(new Object[] { new String[] { "foo", "fail", "fail", "fail", "rab", "oof" }, 1 });
		params.add(new Object[] { new String[] { "foo", "fail", "fail", "fail", "rab", "oof" }, 4 });
		return params;
	}
}

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
public class AlmostStatefulRetryChunkTests {

	private Log logger = LogFactory.getLog(getClass());

	private final Chunk<String> chunk;

	private final int retryLimit;

	private int retryAttempts = 0;

	private static final int BACKSTOP_LIMIT = 1000;

	private int count = 0;

	public AlmostStatefulRetryChunkTests(String[] args, int limit) {
		chunk = new Chunk<String>();
		for (String string : args) {
			chunk.add(string);
		}
		this.retryLimit = limit;
	}

	@Test
	public void testRetry() throws Exception {
		logger.debug("Starting simple scenario");
		List<String> items = new ArrayList<String>(chunk.getItems());
		int before = items.size();
		items.removeAll(Collections.singleton("fail"));
		boolean error = true;
		while (error && count++ < BACKSTOP_LIMIT) {
			try {
				statefulRetry(chunk);
				error = false;
			}
			catch (Exception e) {
				error = true;
			}
		}
		logger.debug("Chunk: " + chunk);
		assertTrue("Backstop reached.  Probably an infinite loop...", count < BACKSTOP_LIMIT);
		assertFalse(chunk.getItems().contains("fail"));
		assertEquals(items, chunk.getItems());
		assertEquals(before-chunk.getItems().size(), chunk.getSkips().size());
	}

	/**
	 * @param chunk
	 * @throws Exception
	 */
	private void statefulRetry(Chunk<String> chunk) throws Exception {
		if (retryAttempts <= retryLimit) {
			try {
				// N.B. a classic stateful retry goes straight to recovery here
				logger.debug(String.format("Retry (attempts=%d) chunk: %s", retryAttempts, chunk));
				doWrite(chunk.getItems());
				retryAttempts = 0;
			}
			catch (Exception e) {
				retryAttempts++;
				// stateful retry always rethrow
				throw e;
			}
		}
		else {
			try {
				logger.debug(String.format("Recover (attempts=%d) chunk: %s", retryAttempts, chunk));
				recover(chunk);
			}
			finally {
				retryAttempts = 0;
			}
		}
		// recovery
		return;

	}

	/**
	 * @param chunk
	 * @throws Exception
	 */
	private void recover(Chunk<String> chunk) throws Exception {
		for (Chunk<String>.ChunkIterator iterator = chunk.iterator(); iterator.hasNext();) {
			String string = iterator.next();
			try {
				doWrite(Collections.singletonList(string));
			} catch (Exception e) {
				iterator.remove(e);
				throw e;
			}
		}
	}

	/**
	 * @param chunk
	 * @throws Exception
	 */
	private void doWrite(List<String> items) throws Exception {
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

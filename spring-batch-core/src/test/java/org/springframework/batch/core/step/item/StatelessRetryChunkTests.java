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
public class StatelessRetryChunkTests {

	private Log logger = LogFactory.getLog(getClass());

	private final Chunk<String> chunk;

	private static final int BACKSTOP_LIMIT = 1000;

	private int count = 0;
	
	public StatelessRetryChunkTests(String[] args) {
		chunk = new Chunk<String>();
		for (String string : args) {
			chunk.add(string);
		}
	}

	@Test
	public void testRetry() throws Exception {

		logger.debug("Starting simple scenario");
		List<String> items = new ArrayList<String>(chunk.getItems());
		int before = items.size();
		
		items.removeAll(Collections.singleton("fail"));

		int errors = 0;
		
		boolean error = true;
		while (error && count++ < BACKSTOP_LIMIT) {
			try {
				// success
				logger.debug("Run items: " + chunk.getItems());
				retryChunk(chunk);
				error = false;
			}
			catch (SpecialException e) {
				error = true;
			}
			catch (Exception e) {
				errors++;
				error = true;
			}
		}

		logger.debug("Items: " + chunk.getItems());
		
		assertTrue("Backstop reached.  Probably an infinite loop...", count < BACKSTOP_LIMIT);
		assertFalse(chunk.getItems().contains("fail"));
		assertEquals(items, chunk.getItems());

		int after = chunk.getItems().size();
		logger.debug(String.format("Error count: %d, size before: %d, size after: %d", errors, before, after));

	}

	/**
	 * @param chunk
	 * @throws Exception
	 */
	private void retryChunk(Chunk<String> chunk) throws Exception {
		boolean complete = chunk.isComplete();
		try {
			doWrite(chunk);
		}
		catch (Exception e) {
			if (chunk.canSkip()) {
				chunk.getSkippedItem();
			}
			else {
				if (complete) {
					chunk.rethrow(e);
				} else {
					chunk.rethrow(new SpecialException());
				}
			}
		}
		chunk.rethrow();
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
		params.add(new Object[] { new String[] { "foo" } });
		params.add(new Object[] { new String[] { "foo", "bar" } });
		params.add(new Object[] { new String[] { "foo", "bar", "spam" } });
		params.add(new Object[] { new String[] { "foo", "bar", "spam", "maps", "rab", "oof" } });
		params.add(new Object[] { new String[] { "fail" } });
		params.add(new Object[] { new String[] { "foo", "fail" } });
		params.add(new Object[] { new String[] { "fail", "bar" } });
		params.add(new Object[] { new String[] { "foo", "fail", "spam" } });
		params.add(new Object[] { new String[] { "fail", "bar", "spam" } });
		params.add(new Object[] { new String[] { "foo", "fail", "spam", "maps", "rab", "oof" } });
		params.add(new Object[] { new String[] { "foo", "fail", "spam", "fail", "rab", "oof" } });
		params.add(new Object[] { new String[] { "fail", "bar", "spam", "fail", "rab", "oof" } });
		params.add(new Object[] { new String[] { "foo", "fail", "fail", "fail", "rab", "oof" } });
		params.add(new Object[] { new String[] { "fail" } });
		params.add(new Object[] { new String[] { "foo", "fail", "fail", "fail", "rab", "oof" } });
		params.add(new Object[] { new String[] { "foo", "fail", "fail", "fail", "rab", "oof" } });
		return params;
	}

	/**
	 * @author Dave Syer
	 *
	 */
	public class SpecialException extends Exception {

	}

}

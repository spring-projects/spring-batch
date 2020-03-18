/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.batch.sample.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

/**
 * Tests for {@link RetrySampleItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class RetrySampleItemWriterTests {

	private RetrySampleItemWriter<Object> processor = new RetrySampleItemWriter<>();

	/*
	 * Processing throws exception on 2nd and 3rd call.
	 */
	@Test
	public void testProcess() throws Exception {
		Object item = null;
		processor.write(Collections.singletonList(item));

		try {
			processor.write(Arrays.asList(item, item, item));
			fail();
		}
		catch (RuntimeException e) {
			// expected
		}

		processor.write(Collections.singletonList(item));

		assertEquals(5, processor.getCounter());
	}
}

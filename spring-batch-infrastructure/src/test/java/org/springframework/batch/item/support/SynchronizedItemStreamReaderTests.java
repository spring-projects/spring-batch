/*
 * Copyright 2015-2018 the original author or authors.
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
package org.springframework.batch.item.support;

import org.junit.Test;

/**
 *
 * @author Matthew Ouyang
 * @author Dimitrios Liapis
 *
 */
public class SynchronizedItemStreamReaderTests extends AbstractSynchronizedItemStreamReaderTests {

	@Test(expected = AssertionError.class)
	public void givenMultipleThreads_whenAllCallItemStreamReader_thenNotThreadSafe() throws Exception {
		TestItemReader testItemReader = new TestItemReader();
		multiThreadedInvocation(testItemReader);
	}

	@Test
	public void givenMultipleThreads_whenAllCallSynchronizedItemStreamReader_thenThreadSafe() throws Exception {
		TestItemReader testItemReader = new TestItemReader();
		SynchronizedItemStreamReader<Integer> synchronizedItemStreamReader = new SynchronizedItemStreamReader<>();
		synchronizedItemStreamReader.setDelegate(testItemReader);
		multiThreadedInvocation(synchronizedItemStreamReader);
	}

}

/*
 * Copyright 2020-2021 the original author or authors.
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
import org.springframework.beans.factory.InitializingBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Dimitrios Liapis
 *
 */
public class SynchronizedItemStreamWriterTests extends AbstractSynchronizedItemStreamWriterTests {


	@Override
	protected SynchronizedItemStreamWriter<Object> createNewSynchronizedItemStreamWriter() {
		SynchronizedItemStreamWriter<Object> synchronizedItemStreamWriter = new SynchronizedItemStreamWriter<>();
		synchronizedItemStreamWriter.setDelegate(delegate);
		return synchronizedItemStreamWriter;
	}

	@Test
	public void testDelegateIsNotNullWhenPropertiesSet() {
		final Exception expectedException = assertThrows(IllegalArgumentException.class,
				() -> ((InitializingBean) new SynchronizedItemStreamWriter<>()).afterPropertiesSet());
		assertEquals("A delegate item writer is required", expectedException.getMessage());
	}
}

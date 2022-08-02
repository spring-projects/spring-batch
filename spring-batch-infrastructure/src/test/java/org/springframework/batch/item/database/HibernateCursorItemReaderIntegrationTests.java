/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.item.database;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 *
 * @author Robert Kasanicky
 */
class HibernateCursorItemReaderIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {

	/**
	 * Exception scenario.
	 *
	 * {@link HibernateCursorItemReader#setUseStatelessSession(boolean)} can be called
	 * only in uninitialized state.
	 */
	@Test
	void testSetUseStatelessSession() {
		HibernateCursorItemReader<Foo> inputSource = (HibernateCursorItemReader<Foo>) reader;

		// initialize and call setter => error
		inputSource.open(new ExecutionContext());
		assertThrows(IllegalStateException.class, () -> inputSource.setUseStatelessSession(false));
	}

}

/*
 * Copyright 2026 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.infrastructure.item.database;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Seonghun Lee
 */
class JpaPagingItemReaderTests {

	@Test
	void closeShouldNotFailWhenReaderWasNeverOpened() throws Exception {
		EntityManagerFactory entityManagerFactory = mock(EntityManagerFactory.class);
		JpaPagingItemReader<Object> reader = new JpaPagingItemReader<>(entityManagerFactory);
		reader.setQueryString("select f from Foo f");
		reader.afterPropertiesSet();

		assertDoesNotThrow(reader::close);
	}

}

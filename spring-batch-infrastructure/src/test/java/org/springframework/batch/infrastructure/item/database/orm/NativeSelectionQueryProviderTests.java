/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.orm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.SelectionQuery;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.sample.Foo;

/**
 * Test for {@link NativeSelectionQueryProvider}s.
 *
 * @author Philippe Marschall
 */
class NativeSelectionQueryProviderTests {

	@Test
	void testJpaNamedQueryProviderNamedQueryIsProvided() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new NativeSelectionQueryProvider<>("", Foo.class));
		assertEquals("sqlQuery must not be empty.", exception.getMessage());
	}

	@Test
	void testJpaNamedQueryProviderEntityClassIsProvided() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new NativeSelectionQueryProvider<>("select * from T_FOOS", null));
		assertEquals("resultType must not be null.", exception.getMessage());
	}

	@Test
	void testNamedQueryCreation() {
		// given
		String nativeQuery = "select * from T_FOOS";
		@SuppressWarnings("unchecked")
		NativeQuery<Foo> query = mock(NativeQuery.class);
		var statelessSession = mock(StatelessSession.class);
		when(statelessSession.createNativeQuery(nativeQuery, Foo.class)).thenReturn(query);
		var namedSelectionQueryProvider = new NativeSelectionQueryProvider<>(nativeQuery, Foo.class);

		// when
		SelectionQuery<Foo> result = namedSelectionQueryProvider.createSelectionQuery(statelessSession);

		// then
		assertNotNull(result);
		verify(statelessSession).createNativeQuery(nativeQuery, Foo.class);
	}

}

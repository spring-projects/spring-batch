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
import org.hibernate.query.SelectionQuery;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.sample.Foo;

/**
 * Test for {@link NamedSelectionQueryProvider}s.
 *
 * @author Philippe Marschall
 */
class NamedSelectionQueryProviderTests {

	@Test
	void testJpaNamedQueryProviderNamedQueryIsProvided() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new NamedSelectionQueryProvider<>("", Foo.class));
		assertEquals("query name is required.", exception.getMessage());
	}

	@Test
	void testJpaNamedQueryProviderEntityClassIsProvided() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new NamedSelectionQueryProvider<>("allFoos", null));
		assertEquals("resultType must not be null.", exception.getMessage());
	}

	@Test
	void testNamedQueryCreation() {
		// given
		String namedQuery = "allFoos";
		@SuppressWarnings("unchecked")
		SelectionQuery<Foo> query = mock(SelectionQuery.class);
		var statelessSession = mock(StatelessSession.class);
		when(statelessSession.createNamedSelectionQuery(namedQuery, Foo.class)).thenReturn(query);
		var namedSelectionQueryProvider = new NamedSelectionQueryProvider<>(namedQuery, Foo.class);

		// when
		SelectionQuery<Foo> result = namedSelectionQueryProvider.createSelectionQuery(statelessSession);

		// then
		assertNotNull(result);
		verify(statelessSession).createNamedSelectionQuery(namedQuery, Foo.class);
	}

}

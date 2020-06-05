/*
 * Copyright 2020 the original author or authors.
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
package org.springframework.batch.item.database.orm;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.batch.item.sample.Foo;

/**
 * Test for {@link JpaNamedQueryProvider}s.
 *
 * @author Parikshit Dutta
 */
public class JpaNamedQueryProviderTests {

	@Test
	public void testJpaNamedQueryProviderNamedQueryIsProvided() {
		JpaNamedQueryProvider<Foo> jpaNamedQueryProvider = new JpaNamedQueryProvider<>();
		jpaNamedQueryProvider.setEntityClass(Foo.class);

		try {
			jpaNamedQueryProvider.afterPropertiesSet();
		}
		catch (Exception exception) {
			assertEquals("Named query cannot be empty", exception.getMessage());
		}
	}

	@Test
	public void testJpaNamedQueryProviderEntityClassIsProvided() {
		JpaNamedQueryProvider<Foo> jpaNamedQueryProvider = new JpaNamedQueryProvider<>();
		jpaNamedQueryProvider.setNamedQuery("allFoos");

		try {
			jpaNamedQueryProvider.afterPropertiesSet();
		}
		catch (Exception exception) {
			assertEquals("Entity class cannot be NULL", exception.getMessage());
		}
	}
}

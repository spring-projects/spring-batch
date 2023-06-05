/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.batch.item.database.builder;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.batch.item.sample.Foo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 */
@MockitoSettings(strictness = Strictness.LENIENT)
class HibernateItemWriterBuilderTests {

	@Mock
	private SessionFactory sessionFactory;

	@Mock
	private Session session;

	@BeforeEach
	void setUp() {
		when(this.sessionFactory.getCurrentSession()).thenReturn(this.session);
	}

	@Test
	void testConfiguration() {
		HibernateItemWriter<Foo> itemWriter = new HibernateItemWriterBuilder<Foo>().sessionFactory(this.sessionFactory)
			.build();

		itemWriter.afterPropertiesSet();

		Chunk<Foo> foos = getFoos();

		itemWriter.write(foos);

		verify(this.session).saveOrUpdate(foos.getItems().get(0));
		verify(this.session).saveOrUpdate(foos.getItems().get(1));
		verify(this.session).saveOrUpdate(foos.getItems().get(2));
	}

	@Test
	void testConfigurationClearSession() {
		HibernateItemWriter<Foo> itemWriter = new HibernateItemWriterBuilder<Foo>().sessionFactory(this.sessionFactory)
			.clearSession(false)
			.build();

		itemWriter.afterPropertiesSet();

		Chunk<Foo> foos = getFoos();

		itemWriter.write(foos);

		verify(this.session).saveOrUpdate(foos.getItems().get(0));
		verify(this.session).saveOrUpdate(foos.getItems().get(1));
		verify(this.session).saveOrUpdate(foos.getItems().get(2));
		verify(this.session, never()).clear();
	}

	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalStateException.class,
				() -> new HibernateItemWriterBuilder<Foo>().build());
		assertEquals("SessionFactory must be provided", exception.getMessage());
	}

	private Chunk<Foo> getFoos() {
		Chunk<Foo> foos = new Chunk<>();

		for (int i = 1; i < 4; i++) {
			Foo foo = new Foo();
			foo.setName("foo" + i);
			foo.setValue(i);
			foos.add(foo);
		}

		return foos;
	}

}

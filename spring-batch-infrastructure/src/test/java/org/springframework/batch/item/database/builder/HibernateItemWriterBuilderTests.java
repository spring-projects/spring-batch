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

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.batch.item.sample.Foo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Michael Minella
 */
@ExtendWith(MockitoExtension.class)
public class HibernateItemWriterBuilderTests {

	@Mock
	private SessionFactory sessionFactory;

	@Mock
	private Session session;

	@BeforeEach
	public void setUp() {
		lenient().when(this.sessionFactory.getCurrentSession()).thenReturn(this.session);
	}

	@Test
	public void testConfiguration() {
		HibernateItemWriter<Foo> itemWriter = new HibernateItemWriterBuilder<Foo>().sessionFactory(this.sessionFactory)
				.build();

		itemWriter.afterPropertiesSet();

		List<Foo> foos = getFoos();

		itemWriter.write(foos);

		verify(this.session).saveOrUpdate(foos.get(0));
		verify(this.session).saveOrUpdate(foos.get(1));
		verify(this.session).saveOrUpdate(foos.get(2));
	}

	@Test
	public void testConfigurationClearSession() {
		HibernateItemWriter<Foo> itemWriter = new HibernateItemWriterBuilder<Foo>().sessionFactory(this.sessionFactory)
				.clearSession(false).build();

		itemWriter.afterPropertiesSet();

		List<Foo> foos = getFoos();

		itemWriter.write(foos);

		verify(this.session).saveOrUpdate(foos.get(0));
		verify(this.session).saveOrUpdate(foos.get(1));
		verify(this.session).saveOrUpdate(foos.get(2));
		verify(this.session, never()).clear();
	}

	@Test
	public void testValidation() {
		try {
			new HibernateItemWriterBuilder<Foo>().build();
			fail("sessionFactory is required");
		}
		catch (IllegalStateException ise) {
			assertEquals("SessionFactory must be provided", ise.getMessage(), "Incorrect message");
		}
	}

	private List<Foo> getFoos() {
		List<Foo> foos = new ArrayList<>(3);

		for (int i = 1; i < 4; i++) {
			Foo foo = new Foo();
			foo.setName("foo" + i);
			foo.setValue(i);
			foos.add(foo);
		}

		return foos;
	}

}

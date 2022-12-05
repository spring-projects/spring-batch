/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.batch.item.data;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.batch.item.Chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
@MockitoSettings(strictness = Strictness.LENIENT)
class Neo4jItemWriterTests {

	private Neo4jItemWriter<String> writer;

	@Mock
	private SessionFactory sessionFactory;

	@Mock
	private Session session;

	@Test
	void testAfterPropertiesSet() throws Exception {

		writer = new Neo4jItemWriter<>();

		Exception exception = assertThrows(IllegalStateException.class, writer::afterPropertiesSet);
		assertEquals("A SessionFactory is required", exception.getMessage());

		writer.setSessionFactory(this.sessionFactory);

		writer.afterPropertiesSet();

		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);

		writer.afterPropertiesSet();
	}

	@Test
	void testWriteNoItemsWithSession() throws Exception {
		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(new Chunk<>());

		verifyNoInteractions(this.session);
	}

	@Test
	void testWriteItemsWithSession() throws Exception {
		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		Chunk<String> items = new Chunk<>();
		items.add("foo");
		items.add("bar");

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(items);

		verify(this.session).save("foo");
		verify(this.session).save("bar");
	}

	@Test
	void testDeleteItemsWithSession() throws Exception {
		writer = new Neo4jItemWriter<>();

		writer.setSessionFactory(this.sessionFactory);
		writer.afterPropertiesSet();

		Chunk<String> items = new Chunk<>();
		items.add("foo");
		items.add("bar");

		writer.setDelete(true);

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(items);

		verify(this.session).delete("foo");
		verify(this.session).delete("bar");
	}

}

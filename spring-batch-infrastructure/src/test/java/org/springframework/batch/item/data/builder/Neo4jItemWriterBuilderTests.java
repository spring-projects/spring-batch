/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.batch.item.data.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.data.Neo4jItemWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 */
@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
class Neo4jItemWriterBuilderTests {

	@Mock
	private SessionFactory sessionFactory;

	@Mock
	private Session session;

	@Test
	void testBasicWriter() throws Exception {
		Neo4jItemWriter<String> writer = new Neo4jItemWriterBuilder<String>().sessionFactory(this.sessionFactory)
			.build();
		Chunk<String> items = new Chunk<>();
		items.add("foo");
		items.add("bar");

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(items);

		verify(this.session).save("foo");
		verify(this.session).save("bar");
		verify(this.session, never()).delete("foo");
		verify(this.session, never()).delete("bar");
	}

	@Test
	void testBasicDelete() throws Exception {
		Neo4jItemWriter<String> writer = new Neo4jItemWriterBuilder<String>().delete(true)
			.sessionFactory(this.sessionFactory)
			.build();
		Chunk<String> items = new Chunk<>();
		items.add("foo");
		items.add("bar");

		when(this.sessionFactory.openSession()).thenReturn(this.session);
		writer.write(items);

		verify(this.session).delete("foo");
		verify(this.session).delete("bar");
		verify(this.session, never()).save("foo");
		verify(this.session, never()).save("bar");
	}

	@Test
	void testNoSessionFactory() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new Neo4jItemWriterBuilder<String>().build());
		assertEquals("sessionFactory is required.", exception.getMessage());
	}

}

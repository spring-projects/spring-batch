/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.infrastructure.item.database.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.SharedStatelessSessionBuilder;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.database.StatelessSessionItemWriter;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.orm.jpa.hibernate.SessionHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Philippe Marschall
 */
@ExtendWith(MockitoExtension.class)
class StatelessSessionItemWriterBuilderTests {

	@Mock
	private SessionFactory sessionFactory;

	@Mock
	private Session session;

	@Mock
	private StatelessSession statelessSession;

	@BeforeEach
	void setUp() {
		TransactionSynchronizationManager.bindResource(this.sessionFactory, new SessionHolder(this.session));
	}

	@AfterEach
	void tearDown() {
		TransactionSynchronizationManager.unbindResource(this.sessionFactory);
	}

	@Test
	void testInsertMultiple() throws Exception {
		StatelessSessionItemWriter<String> itemWriter = new StatelessSessionItemWriterBuilder<String>()
			.sessionFactory(this.sessionFactory)
			.build();

		Chunk<String> chunk = Chunk.of("foo", "bar");

		// statelessWithOptions().connection().open()
		SharedStatelessSessionBuilder statelessSessionBuilder = mock(SharedStatelessSessionBuilder.class);
		when(statelessSessionBuilder.connection()).thenReturn(statelessSessionBuilder);
		when(session.statelessWithOptions()).thenReturn(statelessSessionBuilder);
		when(statelessSessionBuilder.open()).thenReturn(this.statelessSession);

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.execute(status -> {
			itemWriter.write(chunk);
			return null;
		});

		verify(this.statelessSession).insertMultiple(chunk.getItems());
	}

	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalStateException.class,
				() -> new StatelessSessionItemWriterBuilder<String>().build());
		assertEquals("SessionFactory must be provided", exception.getMessage());
	}

}

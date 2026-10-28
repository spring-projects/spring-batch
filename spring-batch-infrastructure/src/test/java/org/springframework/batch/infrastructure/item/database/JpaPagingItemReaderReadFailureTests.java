/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link JpaPagingItemReader} reading again after a failed page query, as a
 * retry of {@link JpaPagingItemReader#read()} does.
 *
 * @author WooSeung Kim
 */
@SpringJUnitConfig(locations = "classpath:data-source-context.xml")
class JpaPagingItemReaderReadFailureTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void readAfterFailedFirstPageQueryReadsFirstPageAgain() throws Exception {
		AtomicBoolean fail = new AtomicBoolean(true);
		JpaPagingItemReader<Foo> reader = createItemReader(fail, true);

		assertThrows(PersistenceException.class, reader::read);
		assertEquals(List.of(1, 2, 3, 4, 5), readAllIds(reader));
		reader.close();
	}

	@Test
	void readAfterFailedFirstPageQueryWithoutTransactionReadsFirstPageAgain() throws Exception {
		AtomicBoolean fail = new AtomicBoolean(true);
		JpaPagingItemReader<Foo> reader = createItemReader(fail, false);

		assertThrows(PersistenceException.class, reader::read);
		assertEquals(List.of(1, 2, 3, 4, 5), readAllIds(reader));
		reader.close();
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void readAfterFailedSecondPageQueryReadsSecondPageAgain(boolean transacted) throws Exception {
		AtomicBoolean fail = new AtomicBoolean(false);
		JpaPagingItemReader<Foo> reader = createItemReader(fail, transacted);

		assertEquals(1, reader.read().getId());
		assertEquals(2, reader.read().getId());
		fail.set(true);
		assertThrows(PersistenceException.class, reader::read);
		assertEquals(List.of(3, 4, 5), readAllIds(reader));
		reader.close();
	}

	@Test
	void failedRollbackDoesNotHideTheFailureOfThePageQuery() throws Exception {
		AtomicBoolean fail = new AtomicBoolean(true);
		JpaPagingItemReader<Foo> reader = createItemReader(createFailingDataSource(fail, true), true);

		PersistenceException exception = assertThrows(PersistenceException.class, reader::read);

		assertEquals(1, exception.getSuppressed().length);
		assertTrue(rootCause(exception).getMessage().contains("Connection reset"));
	}

	private JpaPagingItemReader<Foo> createItemReader(AtomicBoolean fail, boolean transacted) throws Exception {
		return createItemReader(createFailingDataSource(fail, false), transacted);
	}

	private JpaPagingItemReader<Foo> createItemReader(DataSource dataSource, boolean transacted) throws Exception {
		JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<>(createEntityManagerFactory(dataSource));
		reader.setQueryString("select f from Foo f order by f.id");
		reader.setPageSize(2);
		reader.setTransacted(transacted);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
		return reader;
	}

	/**
	 * Create a data source whose connections fail once to prepare a statement, as soon as
	 * {@code fail} is set. If {@code failRollback} is set, they also fail to roll back.
	 */
	private DataSource createFailingDataSource(AtomicBoolean fail, boolean failRollback) {
		return new DelegatingDataSource(this.dataSource) {
			@Override
			public Connection getConnection() throws SQLException {
				Connection connection = super.getConnection();
				return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] { Connection.class }, (proxy, method, args) -> {
							if (method.getName().equals("prepareStatement") && fail.getAndSet(false)) {
								throw new SQLTransientConnectionException("Connection reset");
							}
							if (failRollback && method.getName().equals("rollback")) {
								throw new SQLTransientConnectionException("Rollback failed");
							}
							try {
								return method.invoke(connection, args);
							}
							catch (InvocationTargetException e) {
								throw e.getTargetException();
							}
						});
			}
		};
	}

	private EntityManagerFactory createEntityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factoryBean.setPersistenceUnitName("foo");
		factoryBean.afterPropertiesSet();
		return factoryBean.getObject();
	}

	private Throwable rootCause(Throwable throwable) {
		while (throwable.getCause() != null) {
			throwable = throwable.getCause();
		}
		return throwable;
	}

	private List<Integer> readAllIds(JpaPagingItemReader<Foo> reader) throws Exception {
		List<Integer> ids = new ArrayList<>();
		Foo foo;
		while ((foo = reader.read()) != null) {
			ids.add(foo.getId());
		}
		return ids;
	}

}

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

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
		DataSource dataSource = new DelegatingDataSource(this.dataSource) {
			@Override
			public Connection getConnection() throws SQLException {
				Connection connection = super.getConnection();
				return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] { Connection.class }, (proxy, method, args) -> {
							if (method.getName().equals("prepareStatement") && fail.getAndSet(false)) {
								throw new SQLTransientConnectionException("Connection reset");
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
		JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<>(createEntityManagerFactory(dataSource));
		reader.setQueryString("select f from Foo f order by f.id");
		reader.setPageSize(2);
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());

		assertThrows(PersistenceException.class, reader::read);
		assertEquals(List.of(1, 2, 3, 4, 5), readAllIds(reader));
		reader.close();
	}

	private EntityManagerFactory createEntityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factoryBean.setPersistenceUnitName("foo");
		factoryBean.afterPropertiesSet();
		return factoryBean.getObject();
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

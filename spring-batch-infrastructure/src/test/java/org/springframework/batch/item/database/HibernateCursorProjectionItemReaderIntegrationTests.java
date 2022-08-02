/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.item.database;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 *
 * @author Robert Kasanicky
 */
@SpringJUnitConfig(locations = "data-source-context.xml")
class HibernateCursorProjectionItemReaderIntegrationTests {

	@Autowired
	private DataSource dataSource;

	private void initializeItemReader(HibernateCursorItemReader<?> reader, String hsqlQuery) throws Exception {

		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = factoryBean.getObject();

		reader.setQueryString(hsqlQuery);
		reader.setSessionFactory(sessionFactory);
		reader.afterPropertiesSet();
		reader.setSaveState(true);
		reader.open(new ExecutionContext());

	}

	@Test
	void testMultipleItemsInProjection() throws Exception {
		HibernateCursorItemReader<Object[]> reader = new HibernateCursorItemReader<>();
		initializeItemReader(reader, "select f.value, f.name from Foo f");
		Object[] foo1 = reader.read();
		assertEquals(1, foo1[0]);
	}

	@Test
	void testSingleItemInProjection() throws Exception {
		HibernateCursorItemReader<Object> reader = new HibernateCursorItemReader<>();
		initializeItemReader(reader, "select f.value from Foo f");
		Object foo1 = reader.read();
		assertEquals(1, foo1);
	}

	@Test
	void testSingleItemInProjectionWithArrayType() throws Exception {
		HibernateCursorItemReader<Object[]> reader = new HibernateCursorItemReader<>();
		initializeItemReader(reader, "select f.value from Foo f");
		assertThrows(ClassCastException.class, () -> {
			Object[] foo1 = reader.read();
		});
	}

}

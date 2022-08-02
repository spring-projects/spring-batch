/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.item.database.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.hibernate.query.Query;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.database.orm.HibernateNativeQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Anatoly Polinsky
 * @author Dave Syer
 */
@SpringJUnitConfig(locations = "../data-source-context.xml")
class HibernateNativeQueryProviderIntegrationTests {

	@Autowired
	private DataSource dataSource;

	private final HibernateNativeQueryProvider<Foo> hibernateQueryProvider;

	private SessionFactory sessionFactory;

	HibernateNativeQueryProviderIntegrationTests() {
		hibernateQueryProvider = new HibernateNativeQueryProvider<>();
		hibernateQueryProvider.setEntityClass(Foo.class);
	}

	@BeforeEach
	void setUp() throws Exception {

		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("../Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		sessionFactory = factoryBean.getObject();

	}

	@Test
	@Transactional
	void shouldRetrieveAndMapAllFoos() throws Exception {

		String nativeQuery = "select * from T_FOOS";

		hibernateQueryProvider.setSqlQuery(nativeQuery);
		hibernateQueryProvider.afterPropertiesSet();
		hibernateQueryProvider.setSession(sessionFactory.openSession());

		Query<Foo> query = hibernateQueryProvider.createQuery();

		List<Foo> expectedFoos = new ArrayList<>();

		expectedFoos.add(new Foo(1, "bar1", 1));
		expectedFoos.add(new Foo(2, "bar2", 2));
		expectedFoos.add(new Foo(3, "bar3", 3));
		expectedFoos.add(new Foo(4, "bar4", 4));
		expectedFoos.add(new Foo(5, "bar5", 5));

		List<Foo> actualFoos = query.list();

		assertEquals(actualFoos, expectedFoos);

	}

}

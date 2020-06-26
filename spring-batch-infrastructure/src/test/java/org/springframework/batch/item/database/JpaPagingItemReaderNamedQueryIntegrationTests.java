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
package org.springframework.batch.item.database;

import javax.persistence.EntityManagerFactory;

import org.junit.runner.RunWith;

import org.springframework.batch.item.database.orm.JpaNamedQueryProvider;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration Test for {@link JpaPagingItemReader} and {@link JpaNamedQueryProvider}.
 *
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"JpaPagingItemReaderParameterTests-context.xml"})
public class JpaPagingItemReaderNamedQueryIntegrationTests extends AbstractPagingItemReaderParameterTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;
	
	@Override
	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {

		String namedQuery = "foosStartingFrom2";

		JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<>();

		//creating a named query provider as it would be created in configuration
		JpaNamedQueryProvider<Foo> jpaNamedQueryProvider = new JpaNamedQueryProvider<>();
		jpaNamedQueryProvider.setNamedQuery(namedQuery);
		jpaNamedQueryProvider.setEntityClass(Foo.class);
		jpaNamedQueryProvider.afterPropertiesSet();

		reader.setEntityManagerFactory(entityManagerFactory);
		reader.setQueryProvider(jpaNamedQueryProvider);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}
}

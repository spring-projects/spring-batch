/*
 * Copyright 2026 the original author or authors.
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

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.sample.Foo;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import jakarta.persistence.EntityManagerFactory;

/**
 * @author Philippe Marschall
 */
class StatelessSessionCursorItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	private EntityManagerFactory entityManagerFactory;

	@AfterEach
	protected void tearDown() throws Exception {
		this.entityManagerFactory.close();
		super.tearDown();
	}

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
		factoryBean.setDataSource(getDataSource());
		factoryBean.setPersistenceUnitName("foo");
		factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factoryBean.afterPropertiesSet();
		this.entityManagerFactory = factoryBean.getObject();
		var sessionFactory = this.entityManagerFactory.unwrap(SessionFactory.class);

		String jpqlQuery = "from Foo order by id";
		StatelessSessionCursorItemReader<Foo> itemReader = new StatelessSessionCursorItemReader<>(sessionFactory,
				Foo.class);
		itemReader.setQueryString(jpqlQuery);
		itemReader.afterPropertiesSet();
		itemReader.setSaveState(true);
		return itemReader;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		StatelessSessionCursorItemReader<Foo> reader = (StatelessSessionCursorItemReader<Foo>) tested;
		reader.close();
		reader.setQueryString("from Foo foo where foo.id = -1");
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}

}

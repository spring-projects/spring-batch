/*
 * Copyright 2008-2012 the original author or authors.
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

import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Tests for {@link org.springframework.batch.item.database.JpaPagingItemReader}.
 *
 * @author Thomas Risberg
 */
public class JpaPagingItemReaderIntegrationTests extends AbstractGenericDataSourceItemReaderIntegrationTests {

    @Override
	protected ItemReader<Foo> createItemReader() throws Exception {
		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factoryBean.setPersistenceUnitName("bar");
		factoryBean.afterPropertiesSet();

		EntityManagerFactory entityManagerFactory = factoryBean.getObject();

		String jpqlQuery = "select f from Foo f where name like :name";

		JpaPagingItemReader<Foo> inputSource = new JpaPagingItemReader<>();
		inputSource.setQueryString(jpqlQuery);
		inputSource.setParameterValues(Collections.singletonMap("name", "bar%"));
		inputSource.setEntityManagerFactory(entityManagerFactory);
		inputSource.setPageSize(3);
		inputSource.afterPropertiesSet();
		inputSource.setSaveState(true);

		return inputSource;
	}

}

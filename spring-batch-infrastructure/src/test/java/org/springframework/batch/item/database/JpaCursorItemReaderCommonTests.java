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

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * @author Mahmoud Ben Hassine
 */
public class JpaCursorItemReaderCommonTests extends
		AbstractDatabaseItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		LocalContainerEntityManagerFactoryBean factoryBean =
				new LocalContainerEntityManagerFactoryBean();
		factoryBean.setDataSource(getDataSource());
		factoryBean.setPersistenceUnitName("bar");
		factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factoryBean.afterPropertiesSet();

		String jpqlQuery = "from Foo";
		JpaCursorItemReader<Foo> itemReader = new JpaCursorItemReader<>();
		itemReader.setQueryString(jpqlQuery);
		itemReader.setEntityManagerFactory(factoryBean.getObject());
		itemReader.afterPropertiesSet();
		itemReader.setSaveState(true);
		return itemReader;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		JpaCursorItemReader<Foo> reader = (JpaCursorItemReader<Foo>) tested;
		reader.close();
		reader.setQueryString("from Foo foo where foo.id = -1");
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}
}

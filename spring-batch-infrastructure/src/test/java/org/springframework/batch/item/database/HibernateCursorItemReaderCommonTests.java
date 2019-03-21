/*
 * Copyright 2008-2013 the original author or authors.
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

import org.hibernate.SessionFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

public class HibernateCursorItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {

		SessionFactory sessionFactory = createSessionFactory();

		String hsqlQuery = "from Foo";

		HibernateCursorItemReader<Foo> reader = new HibernateCursorItemReader<>();
		reader.setQueryString(hsqlQuery);
		reader.setSessionFactory(sessionFactory);
		reader.setUseStatelessSession(true);
		reader.setFetchSize(10);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

	private SessionFactory createSessionFactory() throws Exception {
		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(getDataSource());
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		return factoryBean.getObject();

	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		HibernateCursorItemReader<Foo> reader = (HibernateCursorItemReader<Foo>) tested;
		reader.close();
		reader.setQueryString("from Foo foo where foo.id = -1");
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}

}

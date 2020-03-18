/*
 * Copyright 2010-2012 the original author or authors.
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
import org.hibernate.StatelessSession;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public abstract class AbstractHibernateCursorItemReaderIntegrationTests extends
AbstractGenericDataSourceItemReaderIntegrationTests {

	@Override
	protected ItemReader<Foo> createItemReader() throws Exception {

		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMappingLocations(new ClassPathResource("Foo.hbm.xml", getClass()));
		customizeSessionFactory(factoryBean);
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = factoryBean.getObject();

		HibernateCursorItemReader<Foo> hibernateReader = new HibernateCursorItemReader<>();
		setQuery(hibernateReader);
		hibernateReader.setSessionFactory(sessionFactory);
		hibernateReader.setUseStatelessSession(isUseStatelessSession());
		hibernateReader.afterPropertiesSet();
		hibernateReader.setSaveState(true);

		return hibernateReader;

	}

	protected void customizeSessionFactory(LocalSessionFactoryBean factoryBean) {
	}

	protected void setQuery(HibernateCursorItemReader<Foo> reader) throws Exception {
		reader.setQueryString("from Foo");
	}

	protected boolean isUseStatelessSession() {
		return true;
	}

}

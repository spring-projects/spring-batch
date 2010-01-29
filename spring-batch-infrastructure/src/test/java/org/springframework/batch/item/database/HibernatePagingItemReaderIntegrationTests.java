package org.springframework.batch.item.database;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class HibernatePagingItemReaderIntegrationTests extends
		AbstractGenericDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> createItemReader() throws Exception {

		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
		customizeSessionFactory(factoryBean);
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = (SessionFactory) factoryBean.getObject();

		HibernatePagingItemReader<Foo> hibernateReader = new HibernatePagingItemReader<Foo>();
		setQuery(hibernateReader);
		hibernateReader.setPageSize(2);
		hibernateReader.setSessionFactory(sessionFactory);
		hibernateReader.setUseStatelessSession(isUseStatelessSession());
		hibernateReader.afterPropertiesSet();
		hibernateReader.setSaveState(true);

		return hibernateReader;

	}

	protected void customizeSessionFactory(LocalSessionFactoryBean factoryBean) {
	}

	protected void setQuery(HibernatePagingItemReader<?> reader) throws Exception {
		reader.setQueryString("from Foo");
	}

	protected boolean isUseStatelessSession() {
		return true;
	}

}

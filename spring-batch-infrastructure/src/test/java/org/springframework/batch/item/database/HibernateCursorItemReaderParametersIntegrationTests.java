package org.springframework.batch.item.database;

import java.util.Collections;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
public class HibernateCursorItemReaderParametersIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> createItemReader() throws Exception {
		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = (SessionFactory) factoryBean.getObject();


		HibernateCursorItemReader<Foo> hibernateReader = new HibernateCursorItemReader<Foo>();
		setQuery(hibernateReader);
		hibernateReader.setSessionFactory(sessionFactory);
		hibernateReader.setUseStatelessSession(isUseStatelessSession());
		hibernateReader.afterPropertiesSet();
		hibernateReader.setSaveState(true);

		return hibernateReader;
	}
	
	protected void setQuery(HibernateCursorItemReader<?> reader) {
		reader.setQueryString("from Foo where name like :name");
		reader.setParameterValues(Collections.singletonMap("name", (Object)"bar%"));
	}

	protected boolean isUseStatelessSession() {
		return true;
	}

}

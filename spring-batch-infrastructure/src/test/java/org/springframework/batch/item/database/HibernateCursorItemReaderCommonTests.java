package org.springframework.batch.item.database;

import org.hibernate.SessionFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

public class HibernateCursorItemReaderCommonTests extends CommonDatabaseItemStreamItemReaderTests {

	protected ItemReader getItemReader() throws Exception {
		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(getDataSource());
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = (SessionFactory) factoryBean.getObject();

		String hsqlQuery = "from Foo";

		HibernateCursorItemReader reader = new HibernateCursorItemReader();
		reader.setQueryString(hsqlQuery);
		reader.setSessionFactory(sessionFactory);
		reader.setUseStatelessSession(true);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

}

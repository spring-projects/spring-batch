package org.springframework.batch.io.cursor;

import org.hibernate.SessionFactory;
import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.cursor.HibernateCursorInputSource;
import org.springframework.batch.io.support.AbstractDataSourceInputSourceIntegrationTests;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * Tests for {@link HibernateCursorInputSource}
 * 
 * @author Robert Kasanicky
 */
public class HibernateInputSourceIntegrationTests extends AbstractDataSourceInputSourceIntegrationTests {

	protected InputSource createInputSource() throws Exception {
		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(super.getJdbcTemplate().getDataSource());
		factoryBean.setMappingLocations(new Resource[]{new ClassPathResource("Foo.hbm.xml", getClass())});
		factoryBean.afterPropertiesSet();
		
		SessionFactory sessionFactory = (SessionFactory) factoryBean.getObject();
		
		String hsqlQuery = "from Foo";
		
		HibernateCursorInputSource inputSource = new HibernateCursorInputSource();
		inputSource.setQueryString(hsqlQuery);
		inputSource.setSessionFactory(sessionFactory);
		inputSource.afterPropertiesSet();
		
		return inputSource;
	}

}

package org.springframework.batch.io.cursor;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.io.support.AbstractDataSourceItemReaderIntegrationTests;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 * 
 * @author Robert Kasanicky
 */
public class HibernateCursorItemReaderIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader createItemReader() throws Exception {
		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(super.getJdbcTemplate().getDataSource());
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = (SessionFactory) factoryBean.getObject();

		String hsqlQuery = "from Foo";

		HibernateCursorItemReader inputSource = new HibernateCursorItemReader();
		inputSource.setQueryString(hsqlQuery);
		inputSource.setSessionFactory(sessionFactory);
		inputSource.setUseStatelessSession(isUseStatelessSession());
		inputSource.afterPropertiesSet();

		return inputSource;
	}

	protected boolean isUseStatelessSession() {
		return true;
	}

	/**
	 * Exception scenario.
	 * 
	 * {@link HibernateCursorItemReader#setUseStatelessSession(boolean)} can be
	 * called only in uninitialized state.
	 */
	public void testSetUseStatelessSession() {
		HibernateCursorItemReader inputSource = ((HibernateCursorItemReader) source);

		// initialize and call setter => error
		inputSource.open();
		try {
			inputSource.setUseStatelessSession(false);
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

}

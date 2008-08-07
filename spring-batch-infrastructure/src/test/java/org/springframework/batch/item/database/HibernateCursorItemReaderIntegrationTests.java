package org.springframework.batch.item.database;

import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
public class HibernateCursorItemReaderIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> createItemReader() throws Exception {
		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource("Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = (SessionFactory) factoryBean.getObject();

		String hsqlQuery = "from Foo";

		HibernateCursorItemReader<Foo> inputSource = new HibernateCursorItemReader<Foo>();
		inputSource.setQueryString(hsqlQuery);
		inputSource.setSessionFactory(sessionFactory);
		inputSource.setUseStatelessSession(isUseStatelessSession());
		inputSource.afterPropertiesSet();
		inputSource.setSaveState(true);

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
	@Test
	public void testSetUseStatelessSession() {
		HibernateCursorItemReader<Foo> inputSource = (HibernateCursorItemReader<Foo>)reader;

		// initialize and call setter => error
		inputSource.open(new ExecutionContext());
		try {
			inputSource.setUseStatelessSession(false);
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

}

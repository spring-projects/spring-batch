package org.springframework.batch.item.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import javax.sql.DataSource;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests for {@link HibernateCursorItemReader} using {@link StatelessSession}.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
public class HibernateCursorProjectionItemReaderIntegrationTests {

	@Autowired
	private DataSource dataSource;

	private void initializeItemReader(HibernateCursorItemReader<?> reader,
			String hsqlQuery) throws Exception {

		LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setMappingLocations(new Resource[] { new ClassPathResource(
				"Foo.hbm.xml", getClass()) });
		factoryBean.afterPropertiesSet();

		SessionFactory sessionFactory = (SessionFactory) factoryBean
				.getObject();

		reader.setQueryString(hsqlQuery);
		reader.setSessionFactory(sessionFactory);
		reader.afterPropertiesSet();
		reader.setSaveState(true);
		reader.open(new ExecutionContext());

	}
	
	@Test
	public void testMultipleItemsInProjection() throws Exception {
		HibernateCursorItemReader<Object[]> reader = new HibernateCursorItemReader<Object[]>();
		initializeItemReader(reader, "select f.value, f.name from Foo f");
		Object[] foo1 = reader.read();
		assertEquals(1, foo1[0]);
	}

	@Test
	public void testSingleItemInProjection() throws Exception {
		HibernateCursorItemReader<Object> reader = new HibernateCursorItemReader<Object>();
		initializeItemReader(reader, "select f.value from Foo f");
		Object foo1 = reader.read();
		assertEquals(1, foo1);
	}

	@Test
	public void testSingleItemInProjectionWithArrayType() throws Exception {
		HibernateCursorItemReader<Object[]> reader = new HibernateCursorItemReader<Object[]>();
		initializeItemReader(reader, "select f.value from Foo f");
		try {
			Object[] foo1 = reader.read();
			assertNotNull(foo1);
			fail("Expected ClassCastException");
		} catch(ClassCastException e) {
			// expected
		}
	}

}

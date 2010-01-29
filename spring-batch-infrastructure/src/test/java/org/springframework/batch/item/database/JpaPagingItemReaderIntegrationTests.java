package org.springframework.batch.item.database;

import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Tests for {@link org.springframework.batch.item.database.JpaPagingItemReader}.
 *
 * @author Thomas Risberg
 */
public class JpaPagingItemReaderIntegrationTests extends AbstractGenericDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> createItemReader() throws Exception {
		LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
		factoryBean.setDataSource(dataSource);
		factoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
		factoryBean.setPersistenceUnitName("bar");
		factoryBean.afterPropertiesSet();

		EntityManagerFactory entityManagerFactory = factoryBean.getObject();

		String jpqlQuery = "select f from Foo f where name like :name";

		JpaPagingItemReader<Foo> inputSource = new JpaPagingItemReader<Foo>();
		inputSource.setQueryString(jpqlQuery);
		inputSource.setParameterValues(Collections.singletonMap("name", (Object)"bar%"));
		inputSource.setEntityManagerFactory(entityManagerFactory);
		inputSource.setPageSize(3);
		inputSource.afterPropertiesSet();
		inputSource.setSaveState(true);

		return inputSource;
	}

}

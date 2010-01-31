package org.springframework.batch.item.database;

import java.util.Collections;

import javax.persistence.EntityManagerFactory;

import org.junit.runner.RunWith;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JpaPagingItemReaderParameterTests extends AbstractPagingItemReaderParameterTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {

		String jpqlQuery = "select f from Foo f where f.value >= :limit";

		JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<Foo>();
		reader.setQueryString(jpqlQuery);
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 3));
		reader.setEntityManagerFactory(entityManagerFactory);
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

}

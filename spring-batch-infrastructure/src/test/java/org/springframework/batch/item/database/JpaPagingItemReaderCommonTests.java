package org.springframework.batch.item.database;

import javax.persistence.EntityManagerFactory;

import org.junit.runner.RunWith;
import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JpaPagingItemReaderCommonTests extends AbstractItemStreamItemReaderTests {

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	protected ItemReader<Foo> getItemReader() throws Exception {

		String jpqlQuery = "select f from Foo f";

		JpaPagingItemReader<Foo> reader = new JpaPagingItemReader<Foo>();
		reader.setQueryString(jpqlQuery);
		reader.setEntityManagerFactory(entityManagerFactory);
		reader.setPageSize(3);
		reader.afterPropertiesSet();
		reader.setSaveState(true);

		return reader;
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		JpaPagingItemReader<Foo> reader = (JpaPagingItemReader<Foo>) tested;
		reader.close();
		reader.setQueryString("select f from Foo f where f.id = -1");
		reader.afterPropertiesSet();
		reader.open(new ExecutionContext());
	}

}

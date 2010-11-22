package org.springframework.batch.item.database;

import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;

import com.ibatis.sqlmap.client.SqlMapClient;

@RunWith(JUnit4ClassRunner.class)
public class IbatisPagingItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {
	
	protected ItemReader<Foo> getItemReader() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(getDataSource());
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader<Foo> reader = new IbatisPagingItemReader<Foo>();
		reader.setQueryId("getPagedFoos");
		reader.setPageSize(2);
		reader.setSqlMapClient(sqlMapClient);
		reader.setSaveState(true);

		reader.afterPropertiesSet();

		return reader;
	}

	private SqlMapClient createSqlMapClient() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(getDataSource());
		factory.afterPropertiesSet();
		return (SqlMapClient) factory.getObject();
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		IbatisPagingItemReader<Foo> reader = (IbatisPagingItemReader<Foo>) tested;
		reader.close();

		reader.setQueryId("getNoFoos");
		reader.afterPropertiesSet();

		reader.open(new ExecutionContext());
	}

}

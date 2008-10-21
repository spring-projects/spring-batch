package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.core.io.ClassPathResource;
import com.ibatis.sqlmap.client.SqlMapClient;

@SuppressWarnings("unchecked")
@RunWith(JUnit4ClassRunner.class)
public class IbatisPagingItemReaderCommonTests extends CommonDatabaseItemStreamItemReaderTests {

	protected ItemReader<Foo> getItemReader() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(getDataSource());
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader reader = new IbatisPagingItemReader();
		reader.setQueryId("getPagedFoos");
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
		IbatisPagingItemReader reader = (IbatisPagingItemReader) tested;
		reader.close(new ExecutionContext());

		reader.setQueryId("getNoFoos");
		reader.afterPropertiesSet();

		reader.open(new ExecutionContext());
	}

}

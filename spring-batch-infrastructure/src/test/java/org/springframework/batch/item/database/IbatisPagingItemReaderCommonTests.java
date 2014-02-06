package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;

@RunWith(JUnit4.class)
public class IbatisPagingItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader<Foo> reader = new IbatisPagingItemReader<Foo>();
		reader.setQueryId("getPagedFoos");
		reader.setPageSize(2);
		reader.setSqlMapClient(sqlMapClient);
		reader.setDataSource(getDataSource());
		reader.setSaveState(true);

		reader.afterPropertiesSet();

		return reader;
	}

	private SqlMapClient createSqlMapClient() throws Exception {
		return SqlMapClientBuilder.buildSqlMapClient(new ClassPathResource("ibatis-config.xml", getClass()).getInputStream());
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		IbatisPagingItemReader<Foo> reader = (IbatisPagingItemReader<Foo>) tested;
		reader.close();

		reader.setQueryId("getNoFoos");
		reader.afterPropertiesSet();

		reader.open(new ExecutionContext());
	}

}

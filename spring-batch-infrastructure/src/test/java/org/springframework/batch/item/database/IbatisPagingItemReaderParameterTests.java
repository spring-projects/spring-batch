package org.springframework.batch.item.database;

import java.util.Collections;

import org.junit.runner.RunWith;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.ibatis.sqlmap.client.SqlMapClientBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/data-source-context.xml")
public class IbatisPagingItemReaderParameterTests extends AbstractPagingItemReaderParameterTests {

	@Override
	@SuppressWarnings("deprecation")
	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader<Foo> reader = new IbatisPagingItemReader<Foo>();
		reader.setQueryId("getPagedFoos3AndUp");
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 3));
		reader.setSqlMapClient(sqlMapClient);
		reader.setDataSource(dataSource);
		reader.setSaveState(true);

		reader.afterPropertiesSet();

		return reader;
	}

	private SqlMapClient createSqlMapClient() throws Exception {
		SqlMapClient client = SqlMapClientBuilder.buildSqlMapClient(new ClassPathResource("ibatis-config.xml", getClass()).getInputStream());
		return client;
	}
}

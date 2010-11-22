package org.springframework.batch.item.database;

import java.util.Collections;

import org.junit.runner.RunWith;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ibatis.sqlmap.client.SqlMapClient;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/data-source-context.xml")
public class IbatisPagingItemReaderParameterTests extends AbstractPagingItemReaderParameterTests {

	protected AbstractPagingItemReader<Foo> getItemReader() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(dataSource);
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader<Foo> reader = new IbatisPagingItemReader<Foo>();
		reader.setQueryId("getPagedFoos3AndUp");
		reader.setParameterValues(Collections.<String, Object>singletonMap("limit", 3));
		reader.setSqlMapClient(sqlMapClient);
		reader.setSaveState(true);

		reader.afterPropertiesSet();

		return reader;
	}

	private SqlMapClient createSqlMapClient() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(dataSource);
		factory.afterPropertiesSet();
		return (SqlMapClient) factory.getObject();
	}

}

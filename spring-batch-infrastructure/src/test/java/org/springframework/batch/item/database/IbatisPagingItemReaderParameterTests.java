package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.ItemReader;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import com.ibatis.sqlmap.client.SqlMapClient;

import java.util.Collections;

@SuppressWarnings("unchecked")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/org/springframework/batch/item/database/data-source-context.xml")
public class IbatisPagingItemReaderParameterTests extends AbstractPagingItemReaderParameterTests {

	protected ItemReader<Foo> getItemReader() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(dataSource);
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisPagingItemReader reader = new IbatisPagingItemReader();
		reader.setQueryId("getFoos3AndUp");
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

package org.springframework.batch.item.database;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.IbatisKeyCollector;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.junit.runner.RunWith;
import org.junit.internal.runners.JUnit4ClassRunner;

import com.ibatis.sqlmap.client.SqlMapClient;

@SuppressWarnings({"unchecked", "deprecation"})
@RunWith(JUnit4ClassRunner.class)
public class IbatisItemReaderCommonTests extends AbstractDatabaseItemStreamItemReaderTests {

	protected ItemReader<Foo> getItemReader() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(getDataSource());
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = createSqlMapClient();

		IbatisDrivingQueryItemReader reader = new IbatisDrivingQueryItemReader();
		IbatisKeyCollector<Long> keyGenerator = new IbatisKeyCollector<Long>();
		keyGenerator.setDrivingQueryId("getAllFooIds");
		reader.setDetailsQueryId("getFooById");
		keyGenerator.setRestartQueryId("getAllFooIdsRestart");
		keyGenerator.setSqlMapClient(sqlMapClient);
		reader.setSqlMapClient(sqlMapClient);
		reader.setKeyCollector(keyGenerator);
		reader.setSaveState(true);

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
		IbatisDrivingQueryItemReader reader = (IbatisDrivingQueryItemReader) tested;
		reader.close();
		
		IbatisKeyCollector<Long> keyCollector = new IbatisKeyCollector<Long>();
		keyCollector.setDrivingQueryId("getNoFoos");
		keyCollector.setSqlMapClient(createSqlMapClient());
		
		reader.setKeyCollector(keyCollector);
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
	}

}

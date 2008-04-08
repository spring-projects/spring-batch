package org.springframework.batch.item.database;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.IbatisKeyCollector;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;

import com.ibatis.sqlmap.client.SqlMapClient;

public class IbatisItemReaderCommonTests extends CommonDatabaseItemStreamItemReaderTests {

	protected ItemReader getItemReader() throws Exception {
		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(getDataSource());
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = (SqlMapClient) factory.getObject();

		IbatisDrivingQueryItemReader reader = new IbatisDrivingQueryItemReader();
		IbatisKeyCollector keyGenerator = new IbatisKeyCollector();
		keyGenerator.setDrivingQueryId("getAllFooIds");
		reader.setDetailsQueryId("getFooById");
		keyGenerator.setRestartQueryId("getAllFooIdsRestart");
		keyGenerator.setSqlMapClient(sqlMapClient);
		reader.setSqlMapClient(sqlMapClient);
		reader.setKeyCollector(keyGenerator);
		reader.setSaveState(true);

		return reader;
	}

}

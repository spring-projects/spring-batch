package org.springframework.batch.item.database;

import org.junit.runner.RunWith;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.support.IbatisKeyCollector;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Tests for {@link IbatisDrivingQueryItemReader}
 *
 * @author Robert Kasanicky
 * 
 * @deprecated
 */
@SuppressWarnings("unchecked")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "data-source-context.xml")
public class IbatisItemReaderIntegrationTests extends AbstractDataSourceItemReaderIntegrationTests {

	protected ItemReader<Foo> createItemReader() throws Exception {

		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(dataSource);
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = (SqlMapClient) factory.getObject();

		IbatisDrivingQueryItemReader inputSource = new IbatisDrivingQueryItemReader();
		IbatisKeyCollector<Long> keyGenerator = new IbatisKeyCollector<Long>();
		keyGenerator.setDrivingQueryId("getAllFooIds");
		inputSource.setDetailsQueryId("getFooById");
		keyGenerator.setRestartQueryId("getAllFooIdsRestart");
		keyGenerator.setSqlMapClient(sqlMapClient);
		inputSource.setSqlMapClient(sqlMapClient);
		inputSource.setKeyCollector(keyGenerator);
		inputSource.setSaveState(true);

		return inputSource;
	}


}

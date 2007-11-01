package org.springframework.batch.io.driving;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.driving.IbatisInputSource;
import org.springframework.batch.io.driving.support.IbatisKeyGenerator;
import org.springframework.batch.io.support.AbstractDataSourceInputSourceIntegrationTests;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;
import org.springframework.orm.ibatis.SqlMapClientTemplate;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * Tests for {@link IbatisDrivingQueryInputSource}
 *
 * @author Robert Kasanicky
 */
public class IbatisInputSourceIntegrationTests extends AbstractDataSourceInputSourceIntegrationTests {

	protected InputSource createInputSource() throws Exception {

		SqlMapClientFactoryBean factory = new SqlMapClientFactoryBean();
		factory.setConfigLocation(new ClassPathResource("ibatis-config.xml", getClass()));
		factory.setDataSource(super.getJdbcTemplate().getDataSource());
		factory.afterPropertiesSet();
		SqlMapClient sqlMapClient = (SqlMapClient) factory.getObject();
		SqlMapClientTemplate sqlMapClientTemplate = new SqlMapClientTemplate(sqlMapClient);

		IbatisInputSource inputSource = new IbatisInputSource();
		IbatisKeyGenerator keyGenerator = new IbatisKeyGenerator();
		keyGenerator.setDrivingQueryId("getAllFooIds");
		inputSource.setDetailsQueryId("getFooById");
		keyGenerator.setRestartQueryId("getAllFooIdsRestart");
		keyGenerator.setSqlMapClient(sqlMapClient);
		inputSource.setSqlMapClientTemplate(sqlMapClientTemplate);
		inputSource.setKeyGenerator(keyGenerator);

		return inputSource;
	}


}

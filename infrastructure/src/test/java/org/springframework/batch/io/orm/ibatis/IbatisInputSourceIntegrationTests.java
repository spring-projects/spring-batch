package org.springframework.batch.io.orm.ibatis;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.orm.ibatis.IbatisDrivingQueryInputSource;
import org.springframework.batch.io.support.AbstractDataSourceInputSourceIntegrationTests;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ibatis.SqlMapClientFactoryBean;

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

		IbatisInputSource inputSource = new IbatisInputSource();
		inputSource.setDrivingQueryId("getAllFooIds");
		inputSource.setDetailsQueryId("getFooById");
		inputSource.setRestartQueryId("getAllFooIdsRestart");
		inputSource.setSqlMapClient(sqlMapClient);

		return inputSource;
	}


}

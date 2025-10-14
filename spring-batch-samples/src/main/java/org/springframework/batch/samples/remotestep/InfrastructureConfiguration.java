/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.samples.remotestep;

import javax.sql.DataSource;
import jakarta.jms.JMSException;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.postgresql.ds.PGSimpleDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.support.JdbcTransactionManager;

@Configuration
@PropertySource("classpath:org/springframework/batch/samples/remotestep/remote-step-sample.properties")
public class InfrastructureConfiguration {

	/*
	 * Data source configuration
	 */
	@Bean
	public DataSource dataSource(Environment environment) {
		PGSimpleDataSource dataSource = new PGSimpleDataSource();
		dataSource.setUrl(environment.getProperty("spring.datasource.url"));
		dataSource.setUser(environment.getProperty("spring.datasource.username"));
		dataSource.setPassword(environment.getProperty("spring.datasource.password"));
		return dataSource;
	}

	@Bean
	public JdbcTransactionManager transactionManager(DataSource dataSource) {
		return new JdbcTransactionManager(dataSource);
	}

	/*
	 * Broker configuration
	 */
	@Bean
	public ActiveMQConnectionFactory connectionFactory(@Value("${broker.url}") String brokerUrl) throws JMSException {
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
		connectionFactory.setBrokerURL(brokerUrl);
		return connectionFactory;
	}

}

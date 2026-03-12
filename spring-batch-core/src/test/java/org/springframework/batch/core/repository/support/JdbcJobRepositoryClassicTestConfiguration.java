package org.springframework.batch.core.repository.support;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.batch.core.converter.*;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.database.support.DefaultDataFieldMaxValueIncrementerFactory;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;

import javax.sql.DataSource;

/**
 * @author Thomas Risberg
 */
@Configuration
public class JdbcJobRepositoryClassicTestConfiguration {

	@Bean
	public DataSource dataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
		dataSource.setUrl("jdbc:hsqldb:mem:test;sql.enforce_strict_size=true;hsqldb.tx=mvcc");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		return dataSource;
	}

	@Bean
	public TransactionManager transactionManager(DataSource dataSource) {
		PlatformTransactionManager transactionManager = new JdbcTransactionManager(dataSource);
		return transactionManager;
	}

	@Bean
	public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager)
			throws Exception {
		System.setProperty("spring.batch.jdbc.schema.classic", "true");
		JdbcJobRepositoryFactoryBean jobRepositoryFactoryBean = new JdbcJobRepositoryFactoryBean();
		jobRepositoryFactoryBean.setDataSource(dataSource);
		jobRepositoryFactoryBean.setTransactionManager(transactionManager);
		jobRepositoryFactoryBean.setDatabaseType(DatabaseType.fromMetaData(dataSource).name());
		jobRepositoryFactoryBean.setIncrementerFactory(new DefaultDataFieldMaxValueIncrementerFactory(dataSource));
		jobRepositoryFactoryBean.setConversionService(getConversionService());
		jobRepositoryFactoryBean.setJdbcOperations(new JdbcTemplate(dataSource));
		jobRepositoryFactoryBean.afterPropertiesSet();
		return jobRepositoryFactoryBean.getObject();
	}

	protected ConfigurableConversionService getConversionService() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new DateToStringConverter());
		conversionService.addConverter(new StringToDateConverter());
		conversionService.addConverter(new LocalDateToStringConverter());
		conversionService.addConverter(new StringToLocalDateConverter());
		conversionService.addConverter(new LocalTimeToStringConverter());
		conversionService.addConverter(new StringToLocalTimeConverter());
		conversionService.addConverter(new LocalDateTimeToStringConverter());
		conversionService.addConverter(new StringToLocalDateTimeConverter());
		return conversionService;
	}

}

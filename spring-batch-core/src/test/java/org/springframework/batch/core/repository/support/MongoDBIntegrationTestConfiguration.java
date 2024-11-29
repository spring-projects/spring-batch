package org.springframework.batch.core.repository.support;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MongoJobExplorerFactoryBean;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
class MongoDBIntegrationTestConfiguration {

	@Bean
	public JobRepository jobRepository(MongoTemplate mongoTemplate, MongoTransactionManager transactionManager)
			throws Exception {
		MongoJobRepositoryFactoryBean jobRepositoryFactoryBean = new MongoJobRepositoryFactoryBean();
		jobRepositoryFactoryBean.setMongoOperations(mongoTemplate);
		jobRepositoryFactoryBean.setTransactionManager(transactionManager);
		jobRepositoryFactoryBean.afterPropertiesSet();
		return jobRepositoryFactoryBean.getObject();
	}

	@Bean
	public JobExplorer jobExplorer(MongoTemplate mongoTemplate, MongoTransactionManager transactionManager)
			throws Exception {
		MongoJobExplorerFactoryBean jobExplorerFactoryBean = new MongoJobExplorerFactoryBean();
		jobExplorerFactoryBean.setMongoOperations(mongoTemplate);
		jobExplorerFactoryBean.setTransactionManager(transactionManager);
		jobExplorerFactoryBean.afterPropertiesSet();
		return jobExplorerFactoryBean.getObject();
	}

	@Bean
	public MongoDatabaseFactory mongoDatabaseFactory(@Value("${mongo.connectionString}") String connectionString) {
		MongoClient mongoClient = MongoClients.create(connectionString);
		return new SimpleMongoClientDatabaseFactory(mongoClient, "test");
	}

	@Bean
	public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
		MongoTemplate template = new MongoTemplate(mongoDatabaseFactory);
		MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
		converter.setMapKeyDotReplacement(".");
		return template;
	}

	@Bean
	public MongoTransactionManager transactionManager(MongoDatabaseFactory mongoDatabaseFactory) {
		MongoTransactionManager mongoTransactionManager = new MongoTransactionManager();
		mongoTransactionManager.setDatabaseFactory(mongoDatabaseFactory);
		mongoTransactionManager.afterPropertiesSet();
		return mongoTransactionManager;
	}

	@Bean
	public Job job(JobRepository jobRepository, MongoTransactionManager transactionManager) {
		return new JobBuilder("job", jobRepository)
			.start(new StepBuilder("step1", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build())
			.next(new StepBuilder("step2", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
				.build())
			.build();
	}

}

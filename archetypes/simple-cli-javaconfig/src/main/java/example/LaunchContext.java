package example;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@PropertySource("classpath:batch.properties")
@EnableBatchProcessing
@Import(ModuleContext.class)
public class LaunchContext {
	
	@Autowired
	Environment env;
	
	@Bean
	static PropertyPlaceholderConfigurer configurer() {
		return new PropertyPlaceholderConfigurer();
	}
	
	@Bean
	ItemReader<Person> itemReader() {
		FlatFileItemReader<Person> reader = new FlatFileItemReader<Person>();
		reader.setResource(new ClassPathResource("support/sample-data.csv"));
		reader.setLineMapper(new DefaultLineMapper<Person>() {{
			setLineTokenizer(new DelimitedLineTokenizer() {{
				setNames(new String[] { "firstName", "lastName" });
			}});
			setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
				setTargetType(Person.class);
			}});
		}});
		return reader;
	}

	@Bean
	PersonItemProcessor itemProcess() {
		return new PersonItemProcessor();
	}
	
	@Bean
	ItemWriter<Person> itemWriter(DataSource dataSource) {
		JdbcBatchItemWriter<Person> writer = new JdbcBatchItemWriter<Person>();
		writer.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Person>());
		writer.setSql(env.getProperty("person.insert.sql"));
		writer.setDataSource(dataSource);
		return writer;
	}
	
	@Bean
	BasicDataSource dataSource() {
		BasicDataSource dataSource = new BasicDataSource();
		dataSource.setDriverClassName(env.getProperty("batch.jdbc.driver"));
		dataSource.setUrl(env.getProperty("batch.jdbc.url"));
		dataSource.setUsername(env.getProperty("batch.jdbc.user"));
		dataSource.setPassword(env.getProperty("batch.jdbc.password"));
		return dataSource;
	}
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@PostConstruct
	protected void initialize() throws Exception {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(this.resourceLoader.getResource(env.getProperty("batch.drop.script")));
        populator.addScript(this.resourceLoader.getResource(env.getProperty("person.sql.location")));
        populator.addScript(this.resourceLoader.getResource(env.getProperty("batch.schema.script")));
		populator.setContinueOnError(true);
		DatabasePopulatorUtils.execute(populator, dataSource());
	}
	
}

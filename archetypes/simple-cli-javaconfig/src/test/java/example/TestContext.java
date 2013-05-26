package example;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableBatchProcessing
@Import(LaunchContext.class)
public class TestContext {

	@Bean
	JobLauncherTestUtils jobLauncherTestUtils() {
		return new JobLauncherTestUtils();
	}
	
	@Bean
	JobOperator jobOperator(final JobLauncher jobLauncher, final JobExplorer jobExplorer,
			final JobRepository jobRepository, final JobRegistry jobRegistry) {
		return new SimpleJobOperator() {{
			setJobLauncher(jobLauncher);
			setJobExplorer(jobExplorer);
			setJobRepository(jobRepository);
			setJobRegistry(jobRegistry);
		}};
	}
	
	@Bean
	JobExplorerFactoryBean jobExplorer(final DataSource dataSource) {
		return new JobExplorerFactoryBean() {{
			setDataSource(dataSource);
		}};
	}
	
	@Bean
	MapJobRegistry jobRegister() {
		return new MapJobRegistry();
	}
	
	@Bean
	JobRegistryBeanPostProcessor jobRegisterBeanPostProcess(final JobRegistry jobRegistry) {
		return new JobRegistryBeanPostProcessor() {{
			setJobRegistry(jobRegistry);
		}};
	}
}

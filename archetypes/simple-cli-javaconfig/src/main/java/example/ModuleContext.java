package example;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModuleContext {

	@Bean
	public Job personJob(JobBuilderFactory jobs, Step s1) {
		return jobs.get("personJob")
				.incrementer(new RunIdIncrementer())
				.flow(s1)
				.end()
				.build();
	}

	@Bean
	public Step step1(StepBuilderFactory stepBuilderFactory, ItemReader<Person> reader,
			ItemWriter<Person> writer, ItemProcessor<Person, Person> processor) {
		return stepBuilderFactory.get("step1")
				.<Person, Person> chunk(10)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.build();
	}
	
}

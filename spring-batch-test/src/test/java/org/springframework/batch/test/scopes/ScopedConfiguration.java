package org.springframework.batch.test.scopes;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ScopedConfiguration {

	@Bean
	@Scope("step")
	ScopedInterface stepScope() {
		return new ScopedImplementation();
	}

	@Bean
	@Scope("job")
	ScopedInterface jobScope() {
		return new ScopedImplementation();
	}

	@Bean
	@StepScope
	ScopedInterface stepScopeAnnotaion() {
		return new ScopedImplementation();
	}

	@Bean
	@JobScope
	ScopedInterface jobScopeAnnotation() {
		return new ScopedImplementation();
	}

	@Bean
	Tasklet scopeTestTasklet() {
		return ((contribution, chunkContext) -> {
			System.out.println("StepScope:" + stepScope().getInstanceNumber());
			System.out.println("StepScopeAnnotation:" + stepScopeAnnotaion().getInstanceNumber());
			System.out.println("JobScope:" + jobScope().getInstanceNumber());
			System.out.println("JobScopeAnnotation:" + jobScopeAnnotation().getInstanceNumber());
			return RepeatStatus.FINISHED;
		});
	}

}

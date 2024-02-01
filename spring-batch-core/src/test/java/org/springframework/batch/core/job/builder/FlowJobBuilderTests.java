/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.batch.core.job.builder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
class FlowJobBuilderTests {

    private JobRepository jobRepository;

    private JobExecution execution;

    private final StepSupport step1 = new StepSupport("step1") {
        @Override
        public void execute(StepExecution stepExecution)
                throws JobInterruptedException, UnexpectedJobExecutionException {
            stepExecution.upgradeStatus(BatchStatus.COMPLETED);
            stepExecution.setExitStatus(ExitStatus.COMPLETED);
            jobRepository.update(stepExecution);
        }
    };

    private final StepSupport fails = new StepSupport("fails") {
        @Override
        public void execute(StepExecution stepExecution)
                throws JobInterruptedException, UnexpectedJobExecutionException {
            stepExecution.upgradeStatus(BatchStatus.FAILED);
            stepExecution.setExitStatus(ExitStatus.FAILED);
            jobRepository.update(stepExecution);
        }
    };

    private final StepSupport step2 = new StepSupport("step2") {
        @Override
        public void execute(StepExecution stepExecution)
                throws JobInterruptedException, UnexpectedJobExecutionException {
            stepExecution.upgradeStatus(BatchStatus.COMPLETED);
            stepExecution.setExitStatus(ExitStatus.COMPLETED);
            jobRepository.update(stepExecution);
        }
    };

    private final StepSupport step3 = new StepSupport("step3") {
        @Override
        public void execute(StepExecution stepExecution)
                throws JobInterruptedException, UnexpectedJobExecutionException {
            stepExecution.upgradeStatus(BatchStatus.COMPLETED);
            stepExecution.setExitStatus(ExitStatus.COMPLETED);
            jobRepository.update(stepExecution);
        }
    };

    @BeforeEach
    void init() throws Exception {
        EmbeddedDatabase embeddedDatabase = new EmbeddedDatabaseBuilder()
                .addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
                .addScript("/org/springframework/batch/core/schema-hsqldb.sql")
                .build();
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(embeddedDatabase);
        factory.setTransactionManager(new JdbcTransactionManager(embeddedDatabase));
        factory.afterPropertiesSet();
        jobRepository = factory.getObject();
        execution = jobRepository.createJobExecution("flow", new JobParameters());
    }

    @Test
    void testBuildOnOneLine() {
        FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1)
                .on("COMPLETED")
                .to(step2)
                .end()
                .preventRestart();
        builder.build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildSingleFlow() {
        Flow flow = new FlowBuilder<Flow>("subflow").from(step1).next(step2).build();
        FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(flow).end().preventRestart();
        builder.build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildSingleFlowAddingStepsViaNext() {
        Flow flow = new FlowBuilder<Flow>("subflow").next(step1).next(step2).build();
        FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(flow).end().preventRestart();
        builder.build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildOverTwoLines() {
        FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1).on("COMPLETED").to(step2).end();
        builder.preventRestart();
        builder.build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildSubflow() {
        Flow flow = new FlowBuilder<Flow>("subflow").from(step1).end();
        JobFlowBuilder builder = new JobBuilder("flow", jobRepository).start(flow);
        builder.on("COMPLETED").to(step2);
        builder.end().preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildSplit() {
        Flow flow = new FlowBuilder<Flow>("subflow").from(step1).end();
        SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step2);
        builder.split(new SimpleAsyncTaskExecutor()).add(flow).end();
        builder.preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testNestedSplitsWithSingleThread() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(1);

        FlowBuilder<SimpleFlow> flowBuilder = new FlowBuilder<>("flow");
        FlowBuilder.SplitBuilder<SimpleFlow> splitBuilder = flowBuilder.split(taskExecutor);
        splitBuilder.add(new FlowBuilder<Flow>("subflow1").from(step1).end());
        splitBuilder.add(new FlowBuilder<Flow>("subflow2").from(step2).end());
        Job job = new JobBuilder("job", jobRepository).start(flowBuilder.build()).end().build();
        job.execute(execution);

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildSplitUsingStartAndAdd_BATCH_2346() {
        Flow subflow1 = new FlowBuilder<Flow>("subflow1").from(step2).end();
        Flow subflow2 = new FlowBuilder<Flow>("subflow2").from(step3).end();
        Flow splitflow = new FlowBuilder<Flow>("splitflow").start(subflow1)
                .split(new SimpleAsyncTaskExecutor())
                .add(subflow2)
                .build();

        FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(splitflow).end();
        builder.preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildSplit_BATCH_2282() {
        Flow flow1 = new FlowBuilder<Flow>("subflow1").from(step1).end();
        Flow flow2 = new FlowBuilder<Flow>("subflow2").from(step2).end();
        Flow splitFlow = new FlowBuilder<Flow>("splitflow").split(new SimpleAsyncTaskExecutor())
                .add(flow1, flow2)
                .build();
        FlowJobBuilder builder = new JobBuilder("flow", jobRepository).start(splitFlow).end();
        builder.preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildDecision() {
        JobExecutionDecider decider = new JobExecutionDecider() {
            private int count = 0;

            @Override
            public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
                count++;
                return count < 2 ? new FlowExecutionStatus("ONGOING") : FlowExecutionStatus.COMPLETED;
            }
        };
        step1.setAllowStartIfComplete(true);
        SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
        builder.next(decider).on("COMPLETED").end().from(decider).on("*").to(step1).end();
        builder.preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildWithDeciderAtStart() {
        JobExecutionDecider decider = new JobExecutionDecider() {
            private int count = 0;

            @Override
            public FlowExecutionStatus decide(JobExecution jobExecution, @Nullable StepExecution stepExecution) {
                count++;
                return count < 2 ? new FlowExecutionStatus("ONGOING") : FlowExecutionStatus.COMPLETED;
            }
        };
        JobFlowBuilder builder = new JobBuilder("flow", jobRepository).start(decider);
        builder.on("COMPLETED").end().from(decider).on("*").to(step1).end();
        builder.build().preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(1, execution.getStepExecutions().size());
    }

    @Test
    void testBuildWithDeciderPriority() {
        JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("COMPLETED_PARTIALLY");
        JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
        builder.on("COMPLETED_PARTIALLY").end();
        builder.on("COMPLETED*").fail();
        builder.build().preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    @Test
    void testBuildWithWildcardDeciderPriority() {
        JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("COMPLETED_PARTIALLY");
        JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
        builder.on("COMPLETED_?ARTIALLY").end();
        builder.on("COMPLETED_*ARTIALLY").fail();
        builder.build().preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    @Test
    void testBuildWithDeciderPrioritySubstringAndWildcard() {
        JobExecutionDecider decider = (jobExecution, stepExecution) -> new FlowExecutionStatus("CONTINUABLE");
        JobFlowBuilder builder = new JobBuilder("flow_priority", jobRepository).start(decider);
        builder.on("CONTINUABLE").end();
        builder.on("CONTIN*").fail();
        builder.build().preventRestart().build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
    }

    @Test
    void testBuildWithIntermediateSimpleJob() {
        SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
        builder.on("COMPLETED").to(step2).end();
        builder.preventRestart();
        builder.build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildWithIntermediateSimpleJobTwoSteps() {
        SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1).next(step2);
        builder.on("FAILED").to(step3).end();
        builder.build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(2, execution.getStepExecutions().size());
    }

    @Test
    void testBuildWithCustomEndState() {
        SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
        builder.on("COMPLETED").end("FOO");
        builder.preventRestart();
        builder.build().execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals("FOO", execution.getExitStatus().getExitCode());
        assertEquals(1, execution.getStepExecutions().size());
    }

    @Test
    void testBuildWithStop() {
        SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(step1);
        builder.on("COMPLETED").stop();
        builder.preventRestart();
        builder.build().execute(execution);
        assertEquals(BatchStatus.STOPPED, execution.getStatus());
        assertEquals("STOPPED", execution.getExitStatus().getExitCode());
        assertEquals(1, execution.getStepExecutions().size());
    }

    @Test
    void testBuildWithStopAndRestart() throws Exception {
        SimpleJobBuilder builder = new JobBuilder("flow", jobRepository).start(fails);
        builder.on("FAILED").stopAndRestart(step2);
        Job job = builder.build();
        job.execute(execution);
        assertEquals(BatchStatus.STOPPED, execution.getStatus());
        assertEquals(1, execution.getStepExecutions().size());
        execution = jobRepository.createJobExecution("flow", new JobParameters());
        job.execute(execution);
        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertEquals(1, execution.getStepExecutions().size());
        assertEquals("step2", execution.getStepExecutions().iterator().next().getStepName());
    }

    @Test
    void testBuildWithJobScopedStep() throws Exception {
        // given
        ApplicationContext context = new AnnotationConfigApplicationContext(JobConfiguration.class);
        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        Job job = context.getBean(Job.class);
        JobParameters jobParameters = new JobParametersBuilder().addLong("chunkSize", 2L).toJobParameters();

        // when
        JobExecution jobExecution = jobLauncher.run(job, jobParameters);

        // then
        assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

	// https://github.com/spring-projects/spring-batch/issues/3757#issuecomment-1821593539
	@Test
	void testStepNamesMustBeUniqueWithinFlowDefinition() {
		ApplicationContext context = new AnnotationConfigApplicationContext(JobConfigurationForStepNameUnique.class);
		JobLauncher jobLauncher = context.getBean(JobLauncher.class);
		Job job = context.getBean(Job.class);
		assertThrows(AlreadyUsedStepNameException.class, ()->jobLauncher.run(job, new JobParametersBuilder().addLong("random", 2L).addString("stepTwo.name", JobConfigurationForStepNameUnique.SHARED_NAME).toJobParameters()));
		assertThrows(AlreadyUsedStepNameException.class, ()->jobLauncher.run(job, new JobParametersBuilder().addLong("random", 1L).addString("stepTwo.name",JobConfigurationForStepNameUnique.SHARED_NAME).toJobParameters()));
	}

    @EnableBatchProcessing
    @Configuration
    static class JobConfigurationForStepNameUnique{

        private static final String SHARED_NAME ="sharedName";

        private static final Log logger = LogFactory.getLog(FlowJobBuilderTests.class);


        @Bean
        @JobScope
        public Step conditionalStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                         @Value("#{jobParameters['random']}") Integer random) {
            return new StepBuilder("conditionalStep", jobRepository).tasklet(
                    (StepContribution contribution, ChunkContext chunkContext) ->{
                        String exitStatus = (random % 2 == 0) ? "EVEN" : "ODD";
                        logger.info("'conditionalStep' with exitStatus "+exitStatus);
                        contribution.setExitStatus(new ExitStatus(exitStatus));
                        return RepeatStatus.FINISHED;
                    }, transactionManager
            ).build();
        }

		@Bean
		@JobScope
		public Step stepTwo(JobRepository jobRepository, PlatformTransactionManager transactionManager,
							@Value("#{jobParameters['stepTwo.name']}") String name) {
			return new StepBuilder(name, jobRepository)
				.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
					logger.info("Hello from stepTwo");
					return RepeatStatus.FINISHED;
				}, transactionManager)
				.build();
		}

		@Bean
		public Step stepThree(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder(SHARED_NAME, jobRepository)
				.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
					logger.info("Hello from stepThree");
					return RepeatStatus.FINISHED;
				}, transactionManager)
				.build();
		}

		@Bean
		public Step stepFour(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new StepBuilder(SHARED_NAME, jobRepository)
				.tasklet((StepContribution contribution, ChunkContext chunkContext) -> {
					logger.info("Hello from stepFour");
					return RepeatStatus.FINISHED;
				}, transactionManager)
				.build();
		}

		@Bean
		public Job job(JobRepository jobRepository, @Qualifier("conditionalStep") Step conditionalStep,
				@Qualifier("stepFour") Step step4, @Qualifier("stepTwo") Step step2,
				@Qualifier("stepThree") Step step3) {
            JobBuilder jobBuilder = new JobBuilder("flow", jobRepository);
            return jobBuilder.start(conditionalStep)
                    .on("ODD").to(step2)
                    .from(conditionalStep).on("EVEN").to(step3)
                    .from(step3)
            .next(step4)
            .from(step2).next(step4).end().build();
        }

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
                    .addScript("/org/springframework/batch/core/schema-hsqldb.sql")
                    .generateUniqueName(true)
                    .build();
        }

        @Bean
        public JdbcTransactionManager transactionManager(DataSource dataSource) {
            return new JdbcTransactionManager(dataSource);
        }


    }


    @EnableBatchProcessing
    @Configuration
    static class JobConfiguration {

        @Bean
        @JobScope
        public Step step(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                         @Value("#{jobParameters['chunkSize']}") Integer chunkSize) {
            return new StepBuilder("step", jobRepository).<Integer, Integer>chunk(chunkSize, transactionManager)
                    .reader(new ListItemReader<>(Arrays.asList(1, 2, 3, 4)))
                    .writer(items -> {
                    })
                    .build();
        }

        @Bean
        public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
            Step step = step(jobRepository, transactionManager, null);
            return new JobBuilder("job", jobRepository).flow(step).build().build();
        }

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().addScript("/org/springframework/batch/core/schema-drop-hsqldb.sql")
                    .addScript("/org/springframework/batch/core/schema-hsqldb.sql")
                    .generateUniqueName(true)
                    .build();
        }

        @Bean
        public JdbcTransactionManager transactionManager(DataSource dataSource) {
            return new JdbcTransactionManager(dataSource);
        }

    }

}

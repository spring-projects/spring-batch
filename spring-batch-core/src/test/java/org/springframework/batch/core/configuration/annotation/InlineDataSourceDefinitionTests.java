/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration
public class InlineDataSourceDefinitionTests {

    @Test
    public void testInlineDataSourceDefinition() throws Exception {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(MyJobConfiguration.class);
        Job job = applicationContext.getBean(Job.class);
        JobLauncher jobLauncher = applicationContext.getBean(JobLauncher.class);
        JobExecution jobExecution = jobLauncher.run(job, new JobParameters());
        Assert.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
    }

    @Configuration
    @EnableBatchProcessing
    static class MyJobConfiguration {

        private JobBuilderFactory jobs;
        private StepBuilderFactory steps;

        public MyJobConfiguration(JobBuilderFactory jobs, StepBuilderFactory steps) {
            this.jobs = jobs;
            this.steps = steps;
        }

        @Bean
        public Job job() {
            return jobs.get("job")
                    .start(steps.get("step")
                            .tasklet((contribution, chunkContext) -> {
                                System.out.println("hello world");
                                return RepeatStatus.FINISHED;
                            })
                            .build())
                    .build();
        }

        @Bean
        public DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2)
                    .addScript("/org/springframework/batch/core/schema-drop-h2.sql")
                    .addScript("/org/springframework/batch/core/schema-h2.sql")
                    .generateUniqueName(true)
                    .build();
        }
    }
}

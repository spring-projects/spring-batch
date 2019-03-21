/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.chunk.RemoteChunkingMasterStepBuilderFactory;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.context.annotation.Import;
import org.springframework.integration.config.EnableIntegration;

/**
 * Enable Spring Batch Integration features and provide a base configuration for
 * setting up remote chunking or partitioning infrastructure beans.
 *
 * By adding this annotation on a {@link org.springframework.context.annotation.Configuration}
 * class, it will be possible to autowire the following beans:
 *
 * <ul>
 *     <li>{@link RemoteChunkingMasterStepBuilderFactory}:
 *     used to create a master step of a remote chunking setup by automatically
 *     setting the job repository and transaction manager.</li>
 *     <li>{@link RemoteChunkingWorkerBuilder}: used to create the integration
 *     flow on the worker side of a remote chunking setup.</li>
 *     <li>{@link RemotePartitioningMasterStepBuilderFactory}: used to create
 *     a master step of a remote partitioning setup by automatically setting
 *     the job repository, job explorer, bean factory and transaction manager.</li>
 *     <li>{@link RemotePartitioningWorkerStepBuilderFactory}: used to create
 *     a worker step of a remote partitioning setup by automatically setting
 *     the job repository, job explorer, bean factory and transaction manager.</li>
 * </ul>
 *
 * For remote chunking, an example of a configuration class would be:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchIntegration
 * &#064;EnableBatchProcessing
 * public class RemoteChunkingAppConfig {
 *
 * 	&#064;Autowired
 * 	private RemoteChunkingMasterStepBuilderFactory masterStepBuilderFactory;
 *
 * 	&#064;Autowired
 * 	private RemoteChunkingWorkerBuilder workerBuilder;
 *
 * 	&#064;Bean
 * 	public TaskletStep masterStep() {
 *       	 return this.masterStepBuilderFactory
 *       		.get("masterStep")
 *       		.chunk(100)
 *       		.reader(itemReader())
 *       		.outputChannel(outgoingRequestsToWorkers())
 *       		.inputChannel(incomingRepliesFromWorkers())
 *       		.build();
 * 	}
 *
 * 	&#064;Bean
 * 	public IntegrationFlow worker() {
 *       	 return this.workerBuilder
 *       		.itemProcessor(itemProcessor())
 *       		.itemWriter(itemWriter())
 *       		.inputChannel(incomingRequestsFromMaster())
 *       		.outputChannel(outgoingRepliesToMaster())
 *       		.build();
 * 	}
 *
 * 	// Middleware beans omitted
 *
 * }
 * </pre>
 *
 * For remote partitioning, an example of a configuration class would be:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableBatchIntegration
 * &#064;EnableBatchProcessing
 * public class RemotePartitioningAppConfig {
 *
 * 	&#064;Autowired
 * 	private RemotePartitioningMasterStepBuilderFactory masterStepBuilderFactory;
 *
 * 	&#064;Autowired
 * 	private RemotePartitioningWorkerStepBuilderFactory workerStepBuilderFactory;
 *
 * 	&#064;Bean
 * 	public Step masterStep() {
 *       	 return this.masterStepBuilderFactory
 *       		.get("masterStep")
 *       		.partitioner("workerStep", partitioner())
 *       		.gridSize(10)
 *       		.outputChannel(outgoingRequestsToWorkers())
 *       		.inputChannel(incomingRepliesFromWorkers())
 *       		.build();
 * 	}
 *
 * 	&#064;Bean
 * 	public Step workerStep() {
 *       	 return this.workerStepBuilderFactory
 *       		.get("workerStep")
 *       		.inputChannel(incomingRequestsFromMaster())
 *       		.outputChannel(outgoingRepliesToMaster())
 *       		.chunk(100)
 *       		.reader(itemReader())
 *       		.processor(itemProcessor())
 *       		.writer(itemWriter())
 *       		.build();
 * 	}
 *
 * 	// Middleware beans omitted
 *
 * }
 * </pre>
 * @since 4.1
 * @author Mahmoud Ben Hassine
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableIntegration
@Import(BatchIntegrationConfiguration.class)
public @interface EnableBatchIntegration {
}

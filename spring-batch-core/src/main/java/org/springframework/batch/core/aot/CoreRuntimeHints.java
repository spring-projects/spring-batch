/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.core.aot;

import java.lang.reflect.Method;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.AbstractOwnableSynchronizer;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.SerializationHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.batch.core.Entity;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.DecoratingProxy;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for Spring Batch core module.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @author Alexander Arshavskiy
 * @since 5.0
 */
public class CoreRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		Set<String> jdkTypes = Set.of("java.time.Ser", "java.util.Collections$SynchronizedSet",
				"java.util.Collections$SynchronizedCollection", "java.util.concurrent.locks.ReentrantLock$Sync",
				"java.util.concurrent.locks.ReentrantLock$FairSync",
				"java.util.concurrent.locks.ReentrantLock$NonfairSync",
				"java.util.concurrent.ConcurrentHashMap$Segment");

		// resource hints
		hints.resources().registerPattern("org/springframework/batch/core/schema-h2.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-derby.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-hsqldb.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-sqlite.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-db2.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-hana.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-mysql.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-mariadb.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-oracle.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-postgresql.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-sqlserver.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-sybase.sql");

		// proxy hints
		hints.proxies()
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.StepExecutionListener"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.ItemReadListener"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.ItemProcessListener"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.ItemWriteListener"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.ChunkListener"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.SkipListener"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.JobExecutionListener"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.repository.JobRepository"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.explore.JobExplorer"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class))
			.registerJdkProxy(builder -> builder
				.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.launch.JobOperator"))
				.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class));

		// reflection hints: types
		hints.reflection().registerType(Types.class, MemberCategory.DECLARED_FIELDS);
		hints.reflection().registerType(JobContext.class, MemberCategory.INVOKE_PUBLIC_METHODS);
		hints.reflection().registerType(StepContext.class, MemberCategory.INVOKE_PUBLIC_METHODS);
		hints.reflection().registerType(JobParameter.class, MemberCategory.values());
		hints.reflection().registerType(JobParameters.class, MemberCategory.values());
		hints.reflection().registerType(ExitStatus.class, MemberCategory.values());
		hints.reflection().registerType(JobInstance.class, MemberCategory.values());
		hints.reflection().registerType(JobExecution.class, MemberCategory.values());
		hints.reflection().registerType(StepExecution.class, MemberCategory.values());
		hints.reflection().registerType(StepContribution.class, MemberCategory.values());
		hints.reflection().registerType(Entity.class, MemberCategory.values());
		hints.reflection().registerType(ExecutionContext.class, MemberCategory.values());
		hints.reflection().registerType(Chunk.class, MemberCategory.values());
		jdkTypes.stream()
			.map(TypeReference::of)
			.forEach(type -> hints.reflection().registerType(type, MemberCategory.values()));

		// reflection hints: methods
		Method jobContextGetJobParametersMethod = ReflectionUtils.findMethod(JobContext.class, "getJobParameters");
		Assert.state(jobContextGetJobParametersMethod != null, "JobContext#getJobParameters must not be null");
		Method stepContextGetJobParametersMethod = ReflectionUtils.findMethod(StepContext.class, "getJobParameters");
		Assert.state(stepContextGetJobParametersMethod != null, "StepContext#getJobParameters must not be null");

		List<Method> methods = List.of(jobContextGetJobParametersMethod, stepContextGetJobParametersMethod);
		methods.forEach(method -> hints.reflection().registerMethod(method, ExecutableMode.INVOKE));

		// serialization hints
		SerializationHints serializationHints = hints.serialization();
		Stream.of(LinkedHashSet.class, LinkedHashMap.class, HashSet.class, ReentrantLock.class, ConcurrentHashMap.class,
				AbstractOwnableSynchronizer.class, AbstractQueuedSynchronizer.class, Number.class, Byte.class,
				Short.class, Integer.class, Long.class, Double.class, Float.class, Character.class, String.class,
				Boolean.class, Date.class, Calendar.class, LocalDate.class, LocalTime.class, LocalDateTime.class,
				OffsetTime.class, OffsetDateTime.class, ZonedDateTime.class, Instant.class, Duration.class,
				Period.class, HashMap.class, Hashtable.class, ArrayList.class, JobParameter.class, JobParameters.class,
				ExitStatus.class, JobInstance.class, JobExecution.class, StepExecution.class, StepContribution.class,
				Entity.class, ExecutionContext.class, Chunk.class, Properties.class, Exception.class, UUID.class)
			.forEach(serializationHints::registerType);
		jdkTypes.stream().map(TypeReference::of).forEach(serializationHints::registerType);
	}

}

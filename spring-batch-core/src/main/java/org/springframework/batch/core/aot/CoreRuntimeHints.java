/*
 * Copyright 2022-2022 the original author or authors.
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

import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;

/**
 * {@link RuntimeHintsRegistrar} for Spring Batch core module.
 *
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class CoreRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {

		hints.resources().registerPattern("org/springframework/batch/core/schema-h2.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-derby.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-hsqldb.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-sqlite.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-db2.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-hana.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-mysql.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-oracle.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-postgresql.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-sqlserver.sql");
		hints.resources().registerPattern("org/springframework/batch/core/schema-sybase.sql");

		hints.proxies()
				.registerJdkProxy(builder -> builder
						.proxiedInterfaces(TypeReference.of("org.springframework.batch.core.repository.JobRepository"))
						.proxiedInterfaces(SpringProxy.class, Advised.class, DecoratingProxy.class));

	}

}

/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.batch.core.repository.explore.support;

import org.springframework.batch.core.repository.support.JdbcJobRepositoryFactoryBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that automates the creation of a {@link SimpleJobExplorer} by
 * using JDBC DAO implementations. Requires the user to describe what kind of database
 * they use.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @deprecated since 6.0 in favor of {@link JdbcJobRepositoryFactoryBean}. Scheduled for
 * removal in 6.2 or later.
 */
@SuppressWarnings("removal")
@Deprecated(since = "6.0", forRemoval = true)
public class JdbcJobExplorerFactoryBean extends JobExplorerFactoryBean {

}

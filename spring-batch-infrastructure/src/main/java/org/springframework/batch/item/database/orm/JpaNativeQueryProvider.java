/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.item.database.orm;

import jakarta.persistence.Query;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This query provider creates JPA {@link Query queries} from injected native SQL queries.
 * <p>
 * This is useful if there is a need to utilize database-specific features such as query
 * hints, the {@code CONNECT} keyword in Oracle, etc.
 *
 * @author Anatoly Polinsky
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @param <E> entity returned by executing the query
 */
public class JpaNativeQueryProvider<E> extends AbstractJpaQueryProvider {

	private @Nullable Class<E> entityClass;

	private @Nullable String sqlQuery;

	@SuppressWarnings("DataFlowIssue")
	@Override
	public Query createQuery() {
		return getEntityManager().createNativeQuery(sqlQuery, entityClass);
	}

	public void setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
	}

	public void setEntityClass(Class<E> entityClazz) {
		this.entityClass = entityClazz;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(StringUtils.hasText(sqlQuery), "Native SQL query cannot be empty");
		Assert.state(entityClass != null, "Entity class cannot be NULL");
	}

}

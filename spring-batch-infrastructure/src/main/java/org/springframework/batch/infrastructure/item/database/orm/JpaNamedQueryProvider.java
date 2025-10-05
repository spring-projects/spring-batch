/*
 * Copyright 2020-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.database.orm;

import jakarta.persistence.Query;

import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * This query provider creates JPA named {@link Query}s.
 *
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 * @author Stefano Cordio
 * @since 4.3
 * @param <E> entity returned by executing the query
 */
public class JpaNamedQueryProvider<E> extends AbstractJpaQueryProvider {

	private @Nullable Class<E> entityClass;

	private @Nullable String namedQuery;

	@SuppressWarnings("DataFlowIssue")
	@Override
	public Query createQuery() {
		return getEntityManager().createNamedQuery(this.namedQuery, this.entityClass);
	}

	/**
	 * @param namedQuery name of a jpa named query
	 */
	public void setNamedQuery(String namedQuery) {
		this.namedQuery = namedQuery;
	}

	/**
	 * @param entityClazz name of a jpa entity class
	 */
	public void setEntityClass(Class<E> entityClazz) {
		this.entityClass = entityClazz;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(StringUtils.hasText(this.namedQuery), "Named query cannot be empty");
		Assert.state(this.entityClass != null, "Entity class cannot be NULL");
	}

}

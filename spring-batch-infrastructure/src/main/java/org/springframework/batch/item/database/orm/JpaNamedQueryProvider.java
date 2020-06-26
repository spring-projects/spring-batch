/*
 * Copyright 2020 the original author or authors.
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

import javax.persistence.Query;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * This query provider creates JPA named {@link Query}s.
 * </p>
 *
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 * @since 4.3
 *
 * @param <E> entity returned by executing the query
 */
public class JpaNamedQueryProvider<E> extends AbstractJpaQueryProvider {

	private Class<E> entityClass;

	private String namedQuery;

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
		Assert.isTrue(StringUtils.hasText(this.namedQuery), "Named query cannot be empty");
		Assert.notNull(this.entityClass, "Entity class cannot be NULL");
	}
}

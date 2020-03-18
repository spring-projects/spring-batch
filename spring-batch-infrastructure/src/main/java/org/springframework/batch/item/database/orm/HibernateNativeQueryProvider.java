/*
 * Copyright 2006-2008 the original author or authors.
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

import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * <p>
 * This query provider creates Hibernate {@link Query}s from injected native SQL
 * queries. This is useful if there is a need to utilize database-specific
 * features such as query hints, the CONNECT keyword in Oracle, etc.
 * </p>
 * 
 * @author Anatoly Polinsky
 * 
 * @param <E> entity returned by executing the query
 */
public class HibernateNativeQueryProvider<E> extends AbstractHibernateQueryProvider<E> {

	private String sqlQuery;

	private Class<E> entityClass;

	/**
	 * <p>
	 * Create an {@link NativeQuery} from the session provided (preferring
	 * stateless if both are available).
	 * </p>
	 */
    @Override
	@SuppressWarnings("unchecked")
	public NativeQuery<E> createQuery() {

		if (isStatelessSession()) {
			return getStatelessSession().createNativeQuery(sqlQuery).addEntity(entityClass);
		}
		else {
			return getStatefulSession().createNativeQuery(sqlQuery).addEntity(entityClass);
		}
	}

	public void setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
	}

	public void setEntityClass(Class<E> entityClazz) {
		this.entityClass = entityClazz;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.isTrue(StringUtils.hasText(sqlQuery), "Native SQL query cannot be empty");
		Assert.notNull(entityClass, "Entity class cannot be NULL");
	}
}

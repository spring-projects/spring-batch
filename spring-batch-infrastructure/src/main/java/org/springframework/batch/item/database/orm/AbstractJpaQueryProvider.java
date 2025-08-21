/*
 * Copyright 2006-2024 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>
 * Abstract JPA Query Provider to serve as a base class for all JPA {@link Query}
 * providers.
 * </p>
 *
 * @author Anatoly Polinsky
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.1
 */
public abstract class AbstractJpaQueryProvider implements JpaQueryProvider, InitializingBean {

	private @Nullable EntityManager entityManager;

	/**
	 * <p>
	 * Public setter to override the entityManager that was created by this
	 * {@link JpaQueryProvider}. This is currently needed to allow
	 * {@link JpaQueryProvider} to participate in a user's managed transaction.
	 * </p>
	 * @param entityManager EntityManager to use
	 */
	@Override
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * <p>
	 * Getter for {@link EntityManager}
	 * </p>
	 * @return entityManager the injected {@link EntityManager}
	 */
	protected @Nullable EntityManager getEntityManager() {
		return entityManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(entityManager != null, "Entity manager must be set");
	}

}

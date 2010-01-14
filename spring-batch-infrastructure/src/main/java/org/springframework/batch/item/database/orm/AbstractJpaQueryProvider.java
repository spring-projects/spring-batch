/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.database.orm;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.InitializingBean;

/**
 * <p>
 * Abstract JPA Query Provider to serve as a base class for all JPA
 * {@link Query} providers.
 * </p>
 * 
 * @author Anatoly Polinsky
 * @author Dave Syer
 * 
 * @since 2.1
 */
public abstract class AbstractJpaQueryProvider implements JpaQueryProvider, InitializingBean {

	private EntityManager entityManager;

	/**
	 * <p>
	 * Public setter to override the entityManager that was created by this
	 * {@link HibernateQueryProvider}. This is currently needed to allow
	 * {@link HibernateQueryProvider} to participate in a user's managed transaction.
	 * </p>
	 * 
	 * @param entityManager
	 */
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * <p>
	 * Getter for {@link EntityManager}
	 * </p>
	 * 
	 * @return entityManager the injected {@link EntityManager}
	 */
	protected EntityManager getEntityManager() {
		return entityManager;
	}
}

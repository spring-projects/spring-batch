/*
 * Copyright 2006-2021 the original author or authors.
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

import org.springframework.batch.item.ItemReader;

/**
 * <p>
 * Interface defining the functionality to be provided for generating queries for use with
 * JPA {@link ItemReader}s or other custom built artifacts.
 * </p>
 *
 * @author Anatoly Polinsky
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.1
 *
 */
public interface JpaQueryProvider {

	/**
	 * <p>
	 * Create the query object.
	 * </p>
	 * @return created query
	 */
	public Query createQuery();

	/**
	 * Provide an {@link EntityManager} for the query to be built.
	 * @param entityManager to be used by the {@link JpaQueryProvider}.
	 */
	void setEntityManager(EntityManager entityManager);

}

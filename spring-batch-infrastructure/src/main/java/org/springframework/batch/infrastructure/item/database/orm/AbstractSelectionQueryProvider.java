/*
 * Copyright 2026 the original author or authors.
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

import org.springframework.util.Assert;

import org.hibernate.query.SelectionQuery;

/**
 * Abstract Selection Query Provider to serve as a base class for all Hibernate
 * {@link SelectionQuery} providers.
 *
 * @author Philippe Marschall
 * @param <T> the result type
 * @since 6.1
 */
public abstract class AbstractSelectionQueryProvider<T> implements SelectionQueryProvider<T> {

	private final Class<T> resultType;

	public AbstractSelectionQueryProvider(Class<T> resultType) {
		Assert.notNull(resultType, "resultType must not be null.");
		this.resultType = resultType;
	}

	protected Class<T> getResultType() {
		return resultType;
	}

}

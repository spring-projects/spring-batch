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

import org.hibernate.StatelessSession;
import org.hibernate.query.SelectionQuery;
import org.springframework.util.Assert;

import jakarta.persistence.Query;

/**
 * This query provider creates Hibernate named {@link SelectionQuery}s.
 *
 * @author Philippe Marschall
 * @param <T> the result type
 * @since 6.1
 */
public class NamedSelectionQueryProvider<T> extends AbstractSelectionQueryProvider<T> {

	private final String name;

	public NamedSelectionQueryProvider(String name, Class<T> resultType) {
		super(resultType);
		Assert.hasLength(name, "query name is required.");
		this.name = name;
	}

	@Override
	public SelectionQuery<T> createSelectionQuery(StatelessSession statelessSession) {
		return statelessSession.createNamedSelectionQuery(this.name, this.getResultType());
	}

}

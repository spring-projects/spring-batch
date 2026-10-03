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
import org.springframework.batch.infrastructure.item.ItemReader;

/**
 * Interface defining the functionality to be provided for generating queries for use with
 * Hibernate StatelessSession {@link ItemReader}s or other custom-built artifacts.
 *
 * Implementations are thread-safe, so it can be used to write in multiple concurrent
 * readers.
 *
 * @author Philippe Marschall
 * @since 6.1
 */
public interface SelectionQueryProvider<T> {

	/**
	 * Create the selection query object.
	 * @param statelessSession to be used by the {@link SelectionQueryProvider}.
	 * @return created query
	 */
	SelectionQuery<T> createSelectionQuery(StatelessSession statelessSession);

}

/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.database.builder;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.util.Assert;

/**
 * A builder for the {@link JpaItemWriter}.
 *
 * @author Mahmoud Ben Hassine
 * @since 4.1
 * @see JpaItemWriter
 */
public class JpaItemWriterBuilder<T> {

	private EntityManagerFactory entityManagerFactory;
	private boolean usePersist = false;

	/**
	 * The JPA {@link EntityManagerFactory} to obtain an entity manager from. Required.
	 *
	 * @param entityManagerFactory the {@link EntityManagerFactory}
	 * @return this instance for method chaining
	 * @see JpaItemWriter#setEntityManagerFactory(EntityManagerFactory)
	 */
	public JpaItemWriterBuilder<T> entityManagerFactory(EntityManagerFactory entityManagerFactory) {
		this.entityManagerFactory = entityManagerFactory;

		return this;
	}

	/**
	 * Set whether the entity manager should perform a persist instead of a merge.
	 *
	 * @param usePersist defaults to false
	 * @return this instance for method chaining
	 * @see JpaItemWriter#setUsePersist(boolean)
	 */
	public JpaItemWriterBuilder<T> usePersist(boolean usePersist) {
		this.usePersist = usePersist;

		return this;
	}

	/**
	 * Returns a fully built {@link JpaItemWriter}.
	 *
	 * @return the writer
	 */
	public JpaItemWriter<T> build() {
		Assert.state(this.entityManagerFactory != null,
				"EntityManagerFactory must be provided");

		JpaItemWriter<T> writer = new JpaItemWriter<>();
		writer.setEntityManagerFactory(this.entityManagerFactory);
		writer.setUsePersist(this.usePersist);

		return writer;
	}
}

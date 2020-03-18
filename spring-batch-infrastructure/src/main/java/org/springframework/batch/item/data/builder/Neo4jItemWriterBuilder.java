/*
 * Copyright 2017 the original author or authors.
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.batch.item.data.builder;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

import org.springframework.batch.item.data.Neo4jItemWriter;
import org.springframework.util.Assert;

/**
 * A builder implementation for the {@link Neo4jItemWriter}
 *
 * @author Glenn Renfro
 * @since 4.0
 * @see Neo4jItemWriter
 */
public class Neo4jItemWriterBuilder<T> {

	private boolean delete = false;

	private SessionFactory sessionFactory;

	/**
	 * Boolean flag indicating whether the writer should save or delete the item at write
	 * time.
	 * @param delete true if write should delete item, false if item should be saved.
	 * Default is false.
	 * @return The current instance of the builder
	 * @see Neo4jItemWriter#setDelete(boolean)
	 */
	public Neo4jItemWriterBuilder<T> delete(boolean delete) {
		this.delete = delete;

		return this;
	}

	/**
	 * Establish the session factory that will be used to create {@link Session} instances
	 * for interacting with Neo4j.
	 * @param sessionFactory sessionFactory to be used.
	 * @return The current instance of the builder
	 * @see Neo4jItemWriter#setSessionFactory(SessionFactory)
	 */
	public Neo4jItemWriterBuilder<T> sessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		return this;
	}

	/**
	 * Validates and builds a {@link org.springframework.batch.item.data.Neo4jItemWriter}.
	 *
	 * @return a {@link Neo4jItemWriter}
	 */
	public Neo4jItemWriter<T> build() {
		Assert.notNull(sessionFactory, "sessionFactory is required.");
		Neo4jItemWriter<T> writer = new Neo4jItemWriter<>();
		writer.setDelete(this.delete);
		writer.setSessionFactory(this.sessionFactory);
		return writer;
	}
}

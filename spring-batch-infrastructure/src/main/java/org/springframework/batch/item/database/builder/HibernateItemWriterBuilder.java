/*
 * Copyright 2017 the original author or authors.
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
package org.springframework.batch.item.database.builder;

import org.hibernate.SessionFactory;

import org.springframework.batch.item.database.HibernateItemWriter;
import org.springframework.util.Assert;

/**
 * A builder for the {@link HibernateItemWriter}
 *
 * @author Michael Minella
 * @since 4.0
 * @see HibernateItemWriter
 */
public class HibernateItemWriterBuilder<T> {

	private boolean clearSession = true;

	private SessionFactory sessionFactory;

	/**
	 * If set to false, the {@link org.hibernate.Session} will not be cleared at the end
	 * of the chunk.
	 *
	 * @param clearSession defaults to true
	 * @return this instance for method chaining
	 * @see HibernateItemWriter#setClearSession(boolean)
	 */
	public HibernateItemWriterBuilder<T> clearSession(boolean clearSession) {
		this.clearSession = clearSession;

		return this;
	}

	/**
	 * The Hibernate {@link SessionFactory} to obtain a session from.  Required.
	 *
	 * @param sessionFactory the {@link SessionFactory}
	 * @return this instance for method chaining
	 * @see HibernateItemWriter#setSessionFactory(SessionFactory)
	 */
	public HibernateItemWriterBuilder<T> sessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		return this;
	}

	/**
	 * Returns a fully built {@link HibernateItemWriter}
	 *
	 * @return the writer
	 */
	public HibernateItemWriter<T> build() {
		Assert.state(this.sessionFactory != null,
				"SessionFactory must be provided");

		HibernateItemWriter<T> writer = new HibernateItemWriter<>();
		writer.setSessionFactory(this.sessionFactory);
		writer.setClearSession(this.clearSession);

		return writer;
	}
}

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

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ItemReader;

/**
 * <p>
 * Interface defining the functionality to be provided for generating queries
 * for use with Hibernate {@link ItemReader}s or other custom built artifacts.
 * </p>
 * 
 * @author Anatoly Polinsky
 * @author Dave Syer
 * 
 * @since 2.1
 * 
 */
public interface HibernateQueryProvider {

	/**
	 * <p>
	 * Create the query object which type will be determined by the underline
	 * implementation (e.g. Hibernate, JPA, etc.)
	 * </p>
	 * 
	 * @return created query
	 */
	Query createQuery();

	/**
	 * <p>
	 * Inject a {@link Session} that can be used as a factory for queries. The
	 * state of the session is controlled by the caller (i.e. it should be
	 * closed if necessary).
	 * </p>
	 * 
	 * <p>
	 * Use either this method or {@link #setStatelessSession(StatelessSession)}
	 * </p>
	 * 
	 * @param session the {@link Session} to set
	 */
	void setSession(Session session);

	/**
	 * <p>
	 * Inject a {@link StatelessSession} that can be used as a factory for
	 * queries. The state of the session is controlled by the caller (i.e. it
	 * should be closed if necessary).
	 * </p>
	 * 
	 * <p>
	 * Use either this method or {@link #setSession(Session)}
	 * </p>
	 * 
	 * @param session the {@link StatelessSession} to set
	 */
	void setStatelessSession(StatelessSession session);

}

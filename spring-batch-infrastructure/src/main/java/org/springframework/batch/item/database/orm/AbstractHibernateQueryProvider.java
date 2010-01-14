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

/**
 * <p>Abstract Hibernate Query Provider to serve as a base class for all 
 * Hibernate {@link Query} providers.</p>
 * 
 * <p>The implementing provider can be configured to use either 
 * {@link StatelessSession} sufficient for simple mappings without the need 
 * to cascade to associated objects or standard Hibernate {@link Session} 
 * for more advanced mappings or when caching is desired.</p>
 * 
 * @author Anatoly Polinsky
 * @author Dave Syer
 * 
 * @since 2.1
 *
 */
public abstract class AbstractHibernateQueryProvider implements HibernateQueryProvider {
    
    private StatelessSession statelessSession;
    private Session statefulSession;
    
    public void setStatelessSession(StatelessSession statelessSession) {
		this.statelessSession = statelessSession;
	}

	public void setSession(Session statefulSession) {
		this.statefulSession = statefulSession;
	}

	public boolean isStatelessSession() {
        return this.statefulSession==null && this.statelessSession!=null;
    }

	protected StatelessSession getStatelessSession() {
	    return statelessSession;
	}

    protected Session getStatefulSession() {
        return statefulSession;
    }	
}

/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.io.cursor;

import java.util.Properties;

import org.hibernate.ScrollableResults;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.io.InputSource;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link InputSource} for reading database records built on top of Hibernate.
 * 
 * @author Robert Kasanicky
 */
public class HibernateCursorInputSource implements InputSource, Restartable, InitializingBean, DisposableBean,
	ResourceLifecycle {
	
	private static final String RESTART_DATA_ROW_NUMBER_KEY = ClassUtils.getShortName(HibernateCursorInputSource.class)+".rowNumber";

	private SessionFactory sessionFactory;
	private StatelessSession session;
	private ScrollableResults cursor;
	private String queryString;
	
	private int lastCommitRowNumber = 0;
	
	private boolean initialized = false;
	private TransactionSynchronization synchronization = new HibernateInputSourceTransactionSynchronization();
	
	
	public Object read() {
		if (!initialized) {
			open();
		}
		if (cursor.next()) {
			return cursor.get(0);
		}
		return null;
	}

	/**
	 * Close the resultset cursor and hibernate session.
	 */
	public void close() {
		initialized = false;
		cursor.close();
		session.close();
	}

	/**
	 * Create cursor for the query
	 */
	public void open() {
		session = sessionFactory.openStatelessSession();
		cursor = session.createQuery(queryString).scroll();
		
		BatchTransactionSynchronizationManager.registerSynchronization(synchronization );
		initialized = true;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(sessionFactory);
		Assert.hasLength(queryString);
	}

	public void destroy() throws Exception {
		close();
	}

	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * @return the current row number wrapped as <code>RestartData</code>
	 */
	public RestartData getRestartData() {
		Properties props = new Properties();
		props.setProperty(RESTART_DATA_ROW_NUMBER_KEY, String.valueOf(cursor.getRowNumber()));
		
		return new GenericRestartData(props);
	}

	/**
	 * Set the cursor to the received row number.
	 */
	public void restoreFrom(RestartData data) {
		Assert.state(!initialized,
				"Cannot restore when already intialized.  Call close() first before restore()");
		
		Properties props = data.getProperties();
		if (props.getProperty(RESTART_DATA_ROW_NUMBER_KEY) == null) {
			return;
		}
		int rowNumber = Integer.parseInt(props.getProperty(RESTART_DATA_ROW_NUMBER_KEY));
		open();
		cursor.setRowNumber(rowNumber);
	}
	
	/**
	 * Encapsulates transaction events handling.
	 */
	private class HibernateInputSourceTransactionSynchronization extends TransactionSynchronizationAdapter {
		
		public void afterCompletion(int status) {
			if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
				cursor.setRowNumber(lastCommitRowNumber);
			} else if (status == TransactionSynchronization.STATUS_COMMITTED) {
				lastCommitRowNumber = cursor.getRowNumber();
			}
		}
	}

}

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
package org.springframework.batch.item.database;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ExecutionContextUserSupport;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link ItemReader} for reading database records built on top of Hibernate.
 * 
 * It executes the HQL {@link #queryString} when initialized and iterates over
 * the result set as {@link #read()} method is called, returning an object
 * corresponding to current row.
 * 
 * Input source can be configured to use either {@link StatelessSession}
 * sufficient for simple mappings without the need to cascade to associated
 * objects or standard hibernate {@link Session} for more advanced mappings or
 * when caching is desired.
 * 
 * When stateful session is used it will be cleared after successful commit
 * without being flushed (no inserts or updates are expected).
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 */
public class HibernateCursorItemReader extends ExecutionContextUserSupport implements ItemReader, ItemStream,
		InitializingBean {

	private static final String RESTART_DATA_ROW_NUMBER_KEY = "row.number";

	private SessionFactory sessionFactory;

	private StatelessSession statelessSession;

	private Session statefulSession;

	private ScrollableResults cursor;

	private String queryString;

	private boolean useStatelessSession = true;

	private int lastCommitRowNumber = 0;

	/* Current count of processed records. */
	private int currentProcessedRow = 0;

	private boolean initialized = false;

	private boolean saveState = false;

	public HibernateCursorItemReader() {
		setName(ClassUtils.getShortName(HibernateCursorItemReader.class));
	}

	public Object read() {
		if (!initialized) {
			open(new ExecutionContext());
		}
		if (cursor.next()) {
			currentProcessedRow++;
			Object[] data = cursor.get();
			if (data.length>1) {
				return data;
			}
			return data[0];
		}
		return null;
	}

	/**
	 * Closes the result set cursor and hibernate session.
	 */
	public void close(ExecutionContext executionContext) {
		initialized = false;
		if (cursor != null) {
			cursor.close();
		}
		currentProcessedRow = 0;
		if (useStatelessSession) {
			if (statelessSession != null) {
				statelessSession.close();
			}
		}
		else {
			if (statefulSession != null) {
				statefulSession.close();
			}
		}
	}

	/**
	 * Creates cursor for the query.
	 */
	public void open(ExecutionContext executionContext) {
		Assert.state(!initialized, "Cannot open an already opened ItemReader, call close first");

		if (useStatelessSession) {
			statelessSession = sessionFactory.openStatelessSession();
			cursor = statelessSession.createQuery(queryString).scroll();
		}
		else {
			statefulSession = sessionFactory.openSession();
			cursor = statefulSession.createQuery(queryString).scroll();
		}
		initialized = true;

		if (executionContext.containsKey(getKey(RESTART_DATA_ROW_NUMBER_KEY))) {
			currentProcessedRow = Integer.parseInt(executionContext.getString(getKey(RESTART_DATA_ROW_NUMBER_KEY)));
			cursor.setRowNumber(currentProcessedRow - 1);
		}

	}

	/**
	 * @param sessionFactory hibernate session factory
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(sessionFactory);
		Assert.hasLength(queryString);
	}

	/**
	 * @param queryString HQL query string
	 */
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}

	/**
	 * Can be set only in uninitialized state.
	 * 
	 * @param useStatelessSession <code>true</code> to use
	 * {@link StatelessSession} <code>false</code> to use standard hibernate
	 * {@link Session}
	 */
	public void setUseStatelessSession(boolean useStatelessSession) {
		Assert.state(!initialized);
		this.useStatelessSession = useStatelessSession;
	}

	/**
	 */
	public void update(ExecutionContext executionContext) {
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putString(getKey(RESTART_DATA_ROW_NUMBER_KEY), "" + currentProcessedRow);
		}
	}

	/**
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.ItemReader#mark()
	 */
	public void mark() {
		lastCommitRowNumber = currentProcessedRow;
		if (!useStatelessSession) {
			statefulSession.clear();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.stream.ItemStreamAdapter#reset(org.springframework.batch.item.ExecutionContext)
	 */
	public void reset() {
		currentProcessedRow = lastCommitRowNumber;
		if (lastCommitRowNumber == 0) {
			cursor.beforeFirst();
		}
		else {
			// Set the cursor so that next time it is advanced it will
			// come back to the committed row.
			cursor.setRowNumber(lastCommitRowNumber - 1);
		}
	}

	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}
}

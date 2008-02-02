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

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.StreamContext;
import org.springframework.batch.item.reader.AbstractItemStreamItemReader;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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
public class HibernateCursorItemReader extends AbstractItemStreamItemReader implements Skippable, InitializingBean,
		DisposableBean {

	private static final String RESTART_DATA_ROW_NUMBER_KEY = ClassUtils.getShortName(HibernateCursorItemReader.class)
			+ ".rowNumber";

	private static final String SKIPPED_ROWS = ClassUtils.getShortName(HibernateCursorItemReader.class)
			+ ".skippedRows";;

	private SessionFactory sessionFactory;

	private StatelessSession statelessSession;

	private Session statefulSession;

	private ScrollableResults cursor;

	private String queryString;

	private boolean useStatelessSession = true;

	private int lastCommitRowNumber = 0;

	private final List skippedRows = new ArrayList();

	private int skipCount = 0;

	/* Current count of processed records. */
	private int currentProcessedRow = 0;

	private boolean initialized = false;

	public Object read() {
		if (!initialized) {
			open();
		}
		if (cursor.next()) {
			currentProcessedRow++;
			if (!skippedRows.isEmpty()) {
				// while is necessary to handle successive skips.
				while (skippedRows.contains(new Integer(currentProcessedRow))) {
					if (!cursor.next()) {
						return null;
					}
					currentProcessedRow++;
				}
			}
			Object data = cursor.get(0);
			return data;
		}
		return null;
	}

	/**
	 * Closes the result set cursor and hibernate session.
	 */
	public void close() {
		initialized = false;
		cursor.close();
		currentProcessedRow = 0;
		skippedRows.clear();
		skipCount = 0;
		if (useStatelessSession) {
			statelessSession.close();
		}
		else {
			statefulSession.close();
		}
	}

	/**
	 * Creates cursor for the query.
	 */
	public void open() {
		if (useStatelessSession) {
			statelessSession = sessionFactory.openStatelessSession();
			cursor = statelessSession.createQuery(queryString).scroll();
		}
		else {
			statefulSession = sessionFactory.openSession();
			cursor = statefulSession.createQuery(queryString).scroll();
		}
		initialized = true;
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

	public void destroy() throws Exception {
		close();
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
	 * @return the current row number wrapped as <code>StreamContext</code>
	 */
	public StreamContext getStreamContext() {
		Properties props = new Properties();
		props.setProperty(RESTART_DATA_ROW_NUMBER_KEY, "" + currentProcessedRow);
		String skipped = skippedRows.toString();
		props.setProperty(SKIPPED_ROWS, skipped.substring(1, skipped.length() - 1));
		return new StreamContext(props);
	}

	/**
	 * Sets the cursor to the received row number.
	 */
	public void restoreFrom(StreamContext data) {
		Assert.state(!initialized, "Cannot restore when already intialized.  Call close() first before restore()");

		Properties props = data.getProperties();
		if (props.getProperty(RESTART_DATA_ROW_NUMBER_KEY) == null) {
			return;
		}
		currentProcessedRow = Integer.parseInt(props.getProperty(RESTART_DATA_ROW_NUMBER_KEY));
		open();
		cursor.setRowNumber(currentProcessedRow - 1);

		if (!props.containsKey(SKIPPED_ROWS)) {
			return;
		}

		String[] skipped = StringUtils.commaDelimitedListToStringArray(props.getProperty(SKIPPED_ROWS));
		for (int i = 0; i < skipped.length; i++) {
			this.skippedRows.add(new Integer(skipped[i]));
		}
	}

	/**
	 * Skip the current row. If the transaction is rolled back, this row will
	 * not be represented when read() is called. For example, if you read in row
	 * 2, find the data to be bad, and call skip(), then continue processing and
	 * find
	 */
	public void skip() {
		skippedRows.add(new Integer(currentProcessedRow));
		skipCount++;
	}

	/**
	 * Always true, but only supported through a single processed row count, so
	 * do not use in an asynchronous setting.
	 * 
	 * @see org.springframework.batch.item.ItemStream#isMarkSupported()
	 */
	public boolean isMarkSupported() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#mark(org.springframework.batch.item.StreamContext)
	 */
	public void mark(StreamContext streamContext) {
		lastCommitRowNumber = currentProcessedRow;
		if (!useStatelessSession) {
			statefulSession.clear();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.ItemStream#reset(org.springframework.batch.item.StreamContext)
	 */
	public void reset(StreamContext streamContext) {
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

}

/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.batch.infrastructure.item.database;

import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.datasource.ConnectionProxy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SmartDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Implementation of {@link SmartDataSource} that is capable of keeping a single JDBC
 * Connection which is NOT closed after each use even if {@link Connection#close()} is
 * called.
 * <p>
 * The connection can be kept open over multiple transactions when used together with any
 * of Spring's {@link org.springframework.transaction.PlatformTransactionManager}
 * implementations.
 *
 * <p>
 * Loosely based on the SingleConnectionDataSource implementation in Spring Core. Intended
 * to be used with the {@link JdbcCursorItemReader} to provide a connection that remains
 * open across transaction boundaries, It remains open for the life of the cursor, and can
 * be shared with the main transaction of the rest of the step processing.
 *
 * <p>
 * Once close suppression has been turned on for a connection, it will be returned for the
 * first {@link #getConnection()} call. Any subsequent calls to {@link #getConnection()}
 * will retrieve a new connection from the wrapped {@link DataSource} until the
 * {@link DataSourceUtils} queries whether the connection should be closed or not by
 * calling {@link #shouldClose(Connection)} for the close-suppressed {@link Connection}.
 * At that point the cycle starts over again, and the next {@link #getConnection()} call
 * will have the {@link Connection} that is being close-suppressed returned. This allows
 * the use of the close-suppressed {@link Connection} to be the main {@link Connection}
 * for an extended data access process. The close suppression is turned off by calling
 * {@link #stopCloseSuppression(Connection)}.
 *
 * <p>
 * This class is not multi-threading capable.
 *
 * <p>
 * The connection returned will be a close-suppressing proxy instead of the physical
 * {@link Connection}. Be aware that you will not be able to cast this to a native
 * <code>OracleConnection</code> or the like anymore; you'd be required to use
 * {@link Connection#unwrap(Class)}.
 *
 * @author Thomas Risberg
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @see #getConnection()
 * @see Connection#close()
 * @see DataSourceUtils#releaseConnection
 * @see Connection#unwrap(Class)
 * @since 2.0
 */
public class ExtendedConnectionDataSourceProxy implements SmartDataSource, InitializingBean {

	/** Provided DataSource */
	private @Nullable DataSource dataSource;

	/** The connection to suppress close calls for */
	private @Nullable Connection closeSuppressedConnection;

	/** The connection to suppress close calls for */
	private boolean borrowedConnection = false;

	/** Synchronization monitor for the shared Connection */
	private final Lock connectionMonitor = new ReentrantLock();

	/**
	 * No arg constructor for use when configured using JavaBean style.
	 */
	public ExtendedConnectionDataSourceProxy() {
	}

	/**
	 * Constructor that takes as a parameter with the {@link DataSource} to be wrapped.
	 * @param dataSource DataSource to be used
	 */
	public ExtendedConnectionDataSourceProxy(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Setter for the {@link DataSource} that is to be wrapped.
	 * @param dataSource the DataSource
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * @see SmartDataSource
	 */
	@Override
	public boolean shouldClose(Connection connection) {
		if (borrowedConnection && isCloseSuppressionActive(connection)) {
			borrowedConnection = false;
		}
		return !isCloseSuppressionActive(connection);
	}

	/**
	 * Return the status of close suppression being activated for a given
	 * {@link Connection}
	 * @param connection the {@link Connection} that the close suppression status is
	 * requested for
	 * @return true or false
	 */
	public boolean isCloseSuppressionActive(Connection connection) {
		return connection.equals(closeSuppressedConnection);
	}

	/**
	 * @param connection the {@link Connection} that close suppression is requested for
	 */
	public void startCloseSuppression(Connection connection) {
		this.connectionMonitor.lock();
		try {
			closeSuppressedConnection = connection;
			if (TransactionSynchronizationManager.isActualTransactionActive()) {
				borrowedConnection = true;
			}
		}
		finally {
			this.connectionMonitor.unlock();
		}
	}

	/**
	 * @param connection the {@link Connection} that close suppression should be turned
	 * off for
	 */
	public void stopCloseSuppression(Connection connection) {
		this.connectionMonitor.lock();
		try {
			closeSuppressedConnection = null;
			borrowedConnection = false;
		}
		finally {
			this.connectionMonitor.unlock();
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		this.connectionMonitor.lock();
		try {
			return initConnection(null, null);
		}
		finally {
			this.connectionMonitor.unlock();
		}
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		this.connectionMonitor.lock();
		try {
			return initConnection(username, password);
		}
		finally {
			this.connectionMonitor.unlock();
		}
	}

	@SuppressWarnings("DataFlowIssue")
	private Connection initConnection(@Nullable String username, @Nullable String password) throws SQLException {
		if (closeSuppressedConnection != null) {
			if (!borrowedConnection) {
				borrowedConnection = true;
				return closeSuppressedConnection;
			}
		}
		Connection target;
		if (username != null) {
			target = dataSource.getConnection(username, password);
		}
		else {
			target = dataSource.getConnection();
		}

		return getCloseSuppressingConnectionProxy(target);
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return dataSource.getLogWriter();
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public int getLoginTimeout() throws SQLException {
		return dataSource.getLoginTimeout();
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		dataSource.setLogWriter(out);
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		dataSource.setLoginTimeout(seconds);
	}

	/**
	 * Wrap the given Connection with a proxy that delegates every method call to it but
	 * suppresses close calls.
	 * @param target the original Connection to wrap
	 * @return the wrapped Connection
	 */
	protected Connection getCloseSuppressingConnectionProxy(Connection target) {
		return (Connection) Proxy.newProxyInstance(ConnectionProxy.class.getClassLoader(),
				new Class[] { ConnectionProxy.class }, new CloseSuppressingInvocationHandler(target, this));
	}

	/**
	 * Invocation handler that suppresses close calls on JDBC Connections until the
	 * associated instance of the ExtendedConnectionDataSourceProxy determines the
	 * connection should actually be closed.
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		private final ExtendedConnectionDataSourceProxy dataSource;

		public CloseSuppressingInvocationHandler(Connection target, ExtendedConnectionDataSourceProxy dataSource) {
			this.dataSource = dataSource;
			this.target = target;
		}

		@Override
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ConnectionProxy interface coming in...

			switch (method.getName()) {
				case "equals" -> {
					// Only consider equal when proxies are identical.
					return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
				}
				case "hashCode" -> {
					// Use hashCode of Connection proxy.
					return System.identityHashCode(proxy);
				}
				case "close" -> {
					// Handle close method: don't pass the call on if we are
					// suppressing close calls.
					if (dataSource.shouldClose((Connection) proxy)) {
						this.target.close();
					}
					return null;
				}
				case "getTargetConnection" -> {
					// Handle getTargetConnection method: return underlying
					// Connection.
					return this.target;
				}
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}

	}

	/**
	 * Performs only a 'shallow' non-recursive check of self's and delegate's class to
	 * retain Java 5 compatibility.
	 */
	@SuppressWarnings("DataFlowIssue")
	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return iface.isAssignableFrom(SmartDataSource.class) || iface.isAssignableFrom(dataSource.getClass());
	}

	/**
	 * Returns either self or delegate (in this order) if one of them can be cast to
	 * supplied parameter class. Does *not* support recursive unwrapping of the delegate
	 * to retain Java 5 compatibility.
	 */
	@SuppressWarnings("DataFlowIssue")
	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isAssignableFrom(SmartDataSource.class)) {
			@SuppressWarnings("unchecked")
			T casted = (T) this;
			return casted;
		}
		else if (iface.isAssignableFrom(dataSource.getClass())) {
			@SuppressWarnings("unchecked")
			T casted = (T) dataSource;
			return casted;
		}
		throw new SQLException("Unsupported class " + iface.getSimpleName());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(dataSource != null, "DataSource is required");
	}

	@SuppressWarnings("DataFlowIssue")
	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return dataSource.getParentLogger();
	}

}

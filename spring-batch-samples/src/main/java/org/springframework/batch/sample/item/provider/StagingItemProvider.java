package org.springframework.batch.sample.item.provider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepContextAware;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.sample.item.processor.StagingItemProcessor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class StagingItemProvider extends JdbcDaoSupport implements
		ItemProvider, ResourceLifecycle, DisposableBean,
		StepContextAware {

	// Key for buffer in transaction synchronization manager
	private static final String BUFFER_KEY = StagingItemProvider.class.getName()+".BUFFER";

	private static Log logger = LogFactory.getLog(StagingItemProvider.class);

	private StepContext stepContext;

	private LobHandler lobHandler = new DefaultLobHandler();

	private Object lock = new Object();

	private boolean initialized = false;

	private volatile Iterator keys;

	private final TransactionSynchronization synchronization = new StagingInputTransactionSynchronization();

	/**
	 * 
	 * @see org.springframework.batch.io.driving.DrivingQueryInputSource#close()
	 */
	public void close() {
		initialized = false;
		keys = null;
		if (TransactionSynchronizationManager.hasResource(BUFFER_KEY)) {
			TransactionSynchronizationManager.unbindResource(BUFFER_KEY);
		}
	}

	/**
	 * @throws Exception
	 * @see org.springframework.batch.io.driving.DrivingQueryInputSource#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	/**
	 * 
	 * @see org.springframework.batch.io.driving.DrivingQueryInputSource#open()
	 */
	public void open() {
		Assert.state(keys == null || initialized,
				"Cannot open an already open StagingItemProvider"
						+ ", call close() first.");
		keys = retrieveKeys().iterator();
		logger.info("keys: " + keys);
		registerSynchronization();
		initialized = true;
	}

	/**
	 * Callback for injection of the step context.
	 * 
	 * @param stepContext
	 *            the stepContext to set
	 */
	public void setStepContext(StepContext stepContext) {
		this.stepContext = stepContext;
	}

	private List retrieveKeys() {

		synchronized (lock) {

			return getJdbcTemplate()
					.query(

							"SELECT ID FROM BATCH_STAGING WHERE JOB_ID=? AND PROCESSED=? ORDER BY ID",

							new Object[] {
									stepContext.getStepExecution()
											.getJobExecution().getJobId(),
									StagingItemProcessor.NEW },

							new RowMapper() {
								public Object mapRow(ResultSet rs, int rowNum)
										throws SQLException {
									return new Long(rs.getLong(1));
								}
							}

					);

		}

	}

	public Object getKey(Object item) {
		return item;
	}

	public Object next() throws Exception {
		Long id = read();

		if (id == null) {
			return null;
		}
		Object result = getJdbcTemplate().queryForObject(
				"SELECT VALUE FROM BATCH_STAGING WHERE ID=?",
				new Object[] { id }, new RowMapper() {
					public Object mapRow(ResultSet rs, int rowNum)
							throws SQLException {
						byte[] blob = lobHandler.getBlobAsBytes(rs, 1);
						return SerializationUtils.deserialize(blob);
					}
				});
		// Update now - changes will rollback if there is a problem later.
		int count = getJdbcTemplate()
				.update(
						"UPDATE BATCH_STAGING SET PROCESSED=? WHERE ID=? AND PROCESSED=?",
						new Object[] { StagingItemProcessor.DONE, id,
								StagingItemProcessor.NEW });
		if (count != 1) {
			throw new OptimisticLockingFailureException(
					"The staging record with ID="
							+ id
							+ " was updated concurrently when trying to mark as complete.");
		}
		return result;
	}

	private Long read() {
		if (!initialized) {
			open();
		}

		Long key = getBuffer().next();
		if (key == null) {
			synchronized (lock) {
				if (keys.hasNext()) {
					Long next = (Long) keys.next();
					getBuffer().add(next);
					key = next;
					logger.debug("Retrieved key from list: " + key);
				}
			}
		} else {
			logger.debug("Retrieved key from buffer: " + key);
		}
		return key;

	}

	private StagingBuffer getBuffer() {
		if (!TransactionSynchronizationManager.hasResource(BUFFER_KEY)) {
			TransactionSynchronizationManager.bindResource(BUFFER_KEY,
					new StagingBuffer());
		}
		return (StagingBuffer) TransactionSynchronizationManager
				.getResource(BUFFER_KEY);
	}

	public boolean recover(Object data, Throwable cause) {
		return false;
	}

	/**
	 * Register for Synchronization. This method is left protected because
	 * clients of this class should not be registering for synchronization, but
	 * rather only subclasses, at the appropriate time, i.e. when they are not
	 * initialized.
	 */
	protected void registerSynchronization() {
		BatchTransactionSynchronizationManager
				.registerSynchronization(synchronization);
	}

	/*
	 * Called when a transaction has been committed.
	 * 
	 * @see TransactionSynchronization#afterCompletion
	 */
	protected void transactionCommitted() {
		getBuffer().commit();
	}

	/*
	 * Called when a transaction has been rolled back.
	 * 
	 * @see TransactionSynchronization#afterCompletion
	 */
	protected void transactionRolledBack() {
		getBuffer().rollback();
	}

	/**
	 * Encapsulates transaction events handling.
	 */
	private class StagingInputTransactionSynchronization extends
			TransactionSynchronizationAdapter {
		public void afterCompletion(int status) {
			if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
				transactionRolledBack();
			} else if (status == TransactionSynchronization.STATUS_COMMITTED) {
				transactionCommitted();
			}
		}
	}

	private class StagingBuffer {

		private List list = new ArrayList();
		private Iterator iter = new ArrayList().iterator();

		public Long next() {
			if (iter.hasNext()) {
				return (Long) iter.next();
			}
			return null;
		}

		public void add(Long next) {
			list.add(next);
		}

		public void rollback() {
			iter = new ArrayList(list).iterator();
		}

		public void commit() {
			list.clear();
			iter = new ArrayList().iterator();
		}

		public String toString() {
			return "list=" + list + "; iter.hasNext()=" + iter.hasNext();
		}
	}

}

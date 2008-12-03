package org.springframework.batch.sample.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ReaderNotOpenException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

/**
 * Thread-safe database {@link ItemReader} implementing the process indicator
 * pattern.
 */
public class StagingItemReader<T> implements ItemReader<T>, StepExecutionListener, InitializingBean, DisposableBean {

	private static Log logger = LogFactory.getLog(StagingItemReader.class);

	private StepExecution stepExecution;

	private final Object lock = new Object();

	private volatile boolean initialized = false;

	private volatile Iterator<Long> keys;

	private SimpleJdbcTemplate jdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	public void destroy() throws Exception {
		initialized = false;
		keys = null;
	}

	public final void afterPropertiesSet() throws Exception {
		Assert.notNull(jdbcTemplate, "You must provide a DataSource.");
	}

	private List<Long> retrieveKeys() {

		synchronized (lock) {

			return jdbcTemplate.query(

			"SELECT ID FROM BATCH_STAGING WHERE JOB_ID=? AND PROCESSED=? ORDER BY ID",

			new ParameterizedRowMapper<Long>() {
				public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getLong(1);
				}
			},

			stepExecution.getJobExecution().getJobId(), StagingItemWriter.NEW);

		}

	}

	public T read() throws DataAccessException {

		if (!initialized) {
			throw new ReaderNotOpenException("ItemStream must be open before it can be read.");
		}

		Long id = null;
		synchronized (lock) {
			if (keys.hasNext()) {
				id = keys.next();
			}
		}
		logger.debug("Retrieved key from list: " + id);

		if (id == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		T result = (T) jdbcTemplate.queryForObject("SELECT VALUE FROM BATCH_STAGING WHERE ID=?",
				new ParameterizedRowMapper<Object>() {
					public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
						byte[] blob = rs.getBytes(1);
						return SerializationUtils.deserialize(blob);
					}
				}, id);

		// Update now - changes will rollback if there is a problem later.
		int count = jdbcTemplate.update("UPDATE BATCH_STAGING SET PROCESSED=? WHERE ID=? AND PROCESSED=?",
				StagingItemWriter.DONE, id, StagingItemWriter.NEW);
		if (count != 1) {
			throw new OptimisticLockingFailureException("The staging record with ID=" + id
					+ " was updated concurrently when trying to mark as complete (updated " + count + " records.");
		}

		return result;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.domain.StepListener#afterStep(StepExecution
	 * )
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.springframework.batch.core.domain.StepListener#beforeStep(org.
	 * springframework.batch.core.domain.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
		synchronized (lock) {
			if (keys == null) {
				keys = retrieveKeys().iterator();
				logger.info("Keys obtained for staging.");
				initialized = true;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.domain.StepListener#onErrorInStep(java
	 * .lang.Throwable)
	 */
	public ExitStatus onErrorInStep(StepExecution stepExecution, Throwable e) {
		return null;
	}

}

package org.springframework.batch.sample.item.writer;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang.SerializationUtils;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepContextAware;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ClearFailedException;
import org.springframework.batch.item.exception.FlushFailedException;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class StagingItemWriter extends JdbcDaoSupport implements StepContextAware, ItemWriter {

	public static final String NEW = "N";

	public static final String DONE = "Y";

	public static final Object WORKING = "W";

	private DataFieldMaxValueIncrementer incrementer;

	private StepContext stepContext;

	private LobHandler lobHandler = new DefaultLobHandler();

	/**
	 * Public setter for the {@link LobHandler}.
	 * @param lobHandler the {@link LobHandler} to set (defaults to
	 * {@link DefaultLobHandler}).
	 */
	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	/**
	 * Check mandatory properties.
	 * 
	 * @see org.springframework.dao.support.DaoSupport#initDao()
	 */
	protected void initDao() throws Exception {
		super.initDao();
		Assert.notNull(incrementer, "DataFieldMaxValueIncrementer is required - set the incrementer property in the "
				+ ClassUtils.getShortName(StagingItemWriter.class));
	}

	/**
	 * Callback for injection of the step context.
	 * 
	 * @param stepContext the stepContext to set
	 */
	public void setStepContext(StepContext stepContext) {
		this.stepContext = stepContext;
	}

	/**
	 * Setter for the key generator for the staging table.
	 * 
	 * @param incrementer the {@link DataFieldMaxValueIncrementer} to set
	 */
	public void setIncrementer(DataFieldMaxValueIncrementer incrementer) {
		this.incrementer = incrementer;
	}

	/**
	 * Serialize the item to the staging table, and add a NEW processed flag.
	 * 
	 * @see ItemWriter#write(java.lang.Object)
	 */
	public void write(Object data) {
		final long id = incrementer.nextLongValue();
		final long jobId = stepContext.getStepExecution().getJobExecution().getJobId().longValue();
		final byte[] blob = SerializationUtils.serialize((Serializable) data);
		getJdbcTemplate()
				.update("INSERT into BATCH_STAGING (ID, JOB_ID, VALUE, PROCESSED) values (?,?,?,?)",
						new PreparedStatementSetter() {
							public void setValues(PreparedStatement ps) throws SQLException {
								ps.setLong(1, id);
								ps.setLong(2, jobId);
								lobHandler.getLobCreator().setBlobAsBytes(ps, 3, blob);
								ps.setString(4, NEW);
							}
					
				});
	}

	public void close() throws Exception {
	}

	public void clear() throws ClearFailedException {
	}

	public void flush() throws FlushFailedException {
	}

}

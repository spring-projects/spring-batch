package org.springframework.batch.sample.item.writer;

import java.io.Serializable;
import java.sql.Types;

import org.apache.commons.lang.SerializationUtils;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepContextAware;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

public class StagingItemWriter extends JdbcDaoSupport implements
		StepContextAware, ItemWriter {

	public static final String NEW = "N";
	public static final String DONE = "Y";
	public static final Object WORKING = "W";

	private DataFieldMaxValueIncrementer incrementer;
	private StepContext stepContext;

	/**
	 * Check mandatory properties.
	 * 
	 * @see org.springframework.dao.support.DaoSupport#initDao()
	 */
	protected void initDao() throws Exception {
		super.initDao();
		Assert
				.notNull(
						incrementer,
						"DataFieldMaxValueIncrementer is required - set the incrementer property in the "
								+ ClassUtils
										.getShortName(StagingItemWriter.class));
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

	/**
	 * Setter for the key generator for the staging table.
	 * 
	 * @param incrementer
	 *            the {@link DataFieldMaxValueIncrementer} to set
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
		Long id = new Long(incrementer.nextLongValue());
		Long jobId = stepContext.getStepExecution().getJobExecution().getJobId();
		byte[] blob = SerializationUtils.serialize((Serializable) data);
		getJdbcTemplate()
				.update(
						"INSERT into BATCH_STAGING (ID, JOB_ID, VALUE, PROCESSED) values (?,?,?,?)",
						new Object[] { id, jobId, blob, NEW }, 
						new int[] { Types.BIGINT, Types.BIGINT, Types.BLOB, Types.CHAR});
	}

	public void close() throws Exception {
	}

}

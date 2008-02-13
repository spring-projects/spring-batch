package org.springframework.batch.execution.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;
import org.springframework.util.Assert;

/**
 * Jdbc implementation of {@link StepInstanceDao}.<br/>
 * 
 * Allows customization of the tables names used by Spring Batch for step meta
 * data via a prefix property.<br/>
 * 
 * Uses sequences or tables (via Spring's {@link DataFieldMaxValueIncrementer}
 * abstraction) to create all primary keys before inserting a new row. All
 * objects are checked to ensure all fields to be stored are not null. If any
 * are found to be null, an IllegalArgumentException will be thrown. This could
 * be left to JdbcTemplate, however, the exception will be fairly vague, and
 * fails to highlight which field caused the exception.<br/>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 * @see StepInstanceDao
 */
public class JdbcStepInstanceDao extends AbstractJdbcBatchMetadataDao implements StepInstanceDao, InitializingBean {

	private static final String CREATE_STEP = "INSERT into %PREFIX%STEP_INSTANCE(STEP_INSTANCE_ID, JOB_INSTANCE_ID, STEP_NAME) values (?, ?, ?)";

	private static final String FIND_STEP = "SELECT STEP_INSTANCE_ID from %PREFIX%STEP_INSTANCE where JOB_INSTANCE_ID = ? "
			+ "and STEP_NAME = ?";

	private static final String FIND_STEPS = "SELECT STEP_INSTANCE_ID, STEP_NAME from %PREFIX%STEP_INSTANCE where JOB_INSTANCE_ID = ?";

	private DataFieldMaxValueIncrementer stepIncrementer;

	/**
	 * Create a step with the given job's id, and the provided step name. A
	 * unique id is created for the step using an incrementer. (@link
	 * DataFieldMaxValueIncrementer)
	 * 
	 * @see StepDao#createStepInstance(JobInstance, String)
	 * @throws IllegalArgumentException if job or stepName is null.
	 */
	public StepInstance createStepInstance(JobInstance job, String stepName) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(stepName, "StepName cannot be null.");

		Long stepId = new Long(stepIncrementer.nextLongValue());
		Object[] parameters = new Object[] { stepId, job.getId(), stepName };
		getJdbcTemplate().update(getQuery(CREATE_STEP), parameters);

		StepInstance step = new StepInstance(job, stepName, stepId);
		return step;
	}

	/**
	 * Find one step for given job and stepName. A RowMapper is used to map each
	 * row returned to a step object. If none are found, the list will be empty
	 * and null will be returned. If one step is found, it will be returned. If
	 * anymore than one step is found, an exception is thrown.
	 * 
	 * @see StepDao#findStepInstance(Long, String)
	 * @throws IllegalArgumentException if job, stepName, or job.id is null.
	 * @throws IncorrectResultSizeDataAccessException if more than one step is
	 * found.
	 */
	public StepInstance findStepInstance(JobInstance jobInstance, String stepName) {

		Assert.notNull(jobInstance, "Job cannot be null.");
		Assert.notNull(jobInstance.getId(), "Job ID cannot be null");
		Assert.notNull(stepName, "StepName cannot be null");

		Object[] parameters = new Object[] { jobInstance.getId(), stepName };

		RowMapper rowMapper = new StepInstanceRowMapper(jobInstance, stepName);

		List steps = getJdbcTemplate().query(getQuery(FIND_STEP), parameters, rowMapper);

		if (steps.size() == 0) {
			// No step found
			return null;
		}
		else if (steps.size() == 1) {
			StepInstance step = (StepInstance) steps.get(0);
			return step;
		}
		else {
			// This error will likely never be thrown, because there should
			// never be two steps with the same name and JOB_INSTANCE_ID due to
			// database
			// constraints.
			throw new IncorrectResultSizeDataAccessException("Step Invalid, multiple steps found for StepName:"
					+ stepName + " and JobId:" + jobInstance.getId(), 1, steps.size());
		}

	}

	/**
	 * @see StepDao#findStepInstances(JobInstance)
	 * 
	 * Sql implementation which uses a RowMapper to populate a list of all rows
	 * in the step table with the same JOB_INSTANCE_ID.
	 * 
	 * @throws IllegalArgumentException if jobId is null.
	 */
	public List findStepInstances(final JobInstance jobInstance) {

		Assert.notNull(jobInstance, "Job cannot be null.");

		Object[] parameters = new Object[] { jobInstance.getId() };

		RowMapper rowMapper = new StepInstanceRowMapper(jobInstance, null);

		return getJdbcTemplate().query(getQuery(FIND_STEPS), parameters, rowMapper);
	}

//	/**
//	 * @see StepDao#updateStepInstance(StepInstance)
//	 * @throws IllegalArgumentException if step, or it's status and id is null.
//	 */
//	public void updateStepInstance(final StepInstance step) {
//
//		Assert.notNull(step, "Step cannot be null.");
//		Assert.notNull(step.getId(), "Step Id cannot be null.");
//
//		Object[] parameters = new Object[] { step.getLastExecution().getId(), step.getId() };
//
//		getJdbcTemplate().update(getQuery(UPDATE_STEP), parameters);
//	}

	public void setStepIncrementer(DataFieldMaxValueIncrementer stepIncrementer) {
		this.stepIncrementer = stepIncrementer;
	}

	private class StepInstanceRowMapper implements RowMapper {

		private final JobInstance jobInstance;

		private String stepName;

		public StepInstanceRowMapper(JobInstance jobInstance, String stepName) {
			this.jobInstance = jobInstance;
			this.stepName = stepName;
		}

		public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

			if (stepName == null) {
				stepName = rs.getString(2);
			}
			StepInstance stepInstance = new StepInstance(jobInstance, stepName, new Long(rs.getLong(1)));
			return stepInstance;
		}

	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(stepIncrementer, "StepIncrementer cannot be null.");
	}

}

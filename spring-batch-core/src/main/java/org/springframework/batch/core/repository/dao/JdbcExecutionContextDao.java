package org.springframework.batch.core.repository.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.support.ExecutionContextStringSerializer;
import org.springframework.batch.core.repository.support.XStreamExecutionContextStringSerializer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;

/**
 * JDBC DAO for {@link ExecutionContext}.
 * 
 * Stores execution context data related to both Step and Job using
 * discriminator column to distinguish between the two.
 * 
 * @author Lucas Ward
 * @author Robert Kasanicky
 * @author Thomas Risberg
 */
public class JdbcExecutionContextDao extends AbstractJdbcBatchMetadataDao implements ExecutionContextDao {

	private static final String COUNT_JOB_EXECUTION_CONTEXT = "SELECT COUNT(*) FROM %PREFIX%JOB_EXECUTION_CONTEXT "
			+ "WHERE JOB_EXECUTION_ID = ?";

	private static final String FIND_JOB_EXECUTION_CONTEXT = "SELECT SHORT_CONTEXT, SERIALIZED_CONTEXT " +
			"FROM %PREFIX%JOB_EXECUTION_CONTEXT WHERE JOB_EXECUTION_ID = ?";

	private static final String INSERT_JOB_EXECUTION_CONTEXT = "INSERT INTO %PREFIX%JOB_EXECUTION_CONTEXT " +
			"(JOB_EXECUTION_ID, SHORT_CONTEXT, SERIALIZED_CONTEXT) " +
			"VALUES(?, ?, ?)";

	private static final String UPDATE_JOB_EXECUTION_CONTEXT = "UPDATE %PREFIX%JOB_EXECUTION_CONTEXT " +
			"SET SHORT_CONTEXT = ?, SERIALIZED_CONTEXT = ? " +
			"WHERE JOB_EXECUTION_ID = ?";

	private static final String COUNT_STEP_EXECUTION_CONTEXT = "SELECT COUNT(*) FROM %PREFIX%STEP_EXECUTION_CONTEXT "
			+ "WHERE STEP_EXECUTION_ID = ?";

	private static final String FIND_STEP_EXECUTION_CONTEXT = "SELECT SHORT_CONTEXT, SERIALIZED_CONTEXT " +
			"FROM %PREFIX%STEP_EXECUTION_CONTEXT WHERE STEP_EXECUTION_ID = ?";


	private static final String INSERT_STEP_EXECUTION_CONTEXT = "INSERT INTO %PREFIX%STEP_EXECUTION_CONTEXT " +
			"(STEP_EXECUTION_ID, SHORT_CONTEXT, SERIALIZED_CONTEXT) " +
			"VALUES(?, ?, ?)";

	private static final String UPDATE_STEP_EXECUTION_CONTEXT = "UPDATE %PREFIX%STEP_EXECUTION_CONTEXT " +
			"SET SHORT_CONTEXT = ?, SERIALIZED_CONTEXT = ? " +
			"WHERE STEP_EXECUTION_ID = ?";

	private static final int MAX_VARCHAR_LENGTH = 2500;

	private LobHandler lobHandler = new DefaultLobHandler();

	private ExecutionContextStringSerializer serializer;

	/**
	 * @param jobExecution
	 * @return execution context associated with the given jobExecution.
	 */
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		Long executionId = jobExecution.getId();
		Assert.notNull(executionId, "ExecutionId must not be null.");

		List<ExecutionContext> results = getJdbcTemplate().query(getQuery(FIND_JOB_EXECUTION_CONTEXT),
				new ExecutionContextRowMapper(),
				executionId);
		if (results.size() > 0) {
			return results.get(0);
		}
		else {
			return new ExecutionContext();
		}
	}

	/**
	 * @param stepExecution
	 * @return execution context associated with the given stepExecution.
	 */
	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		Long executionId = stepExecution.getId();
		Assert.notNull(executionId, "ExecutionId must not be null.");

		List<ExecutionContext> results = getJdbcTemplate().query(getQuery(FIND_STEP_EXECUTION_CONTEXT),
				new ExecutionContextRowMapper(),
				executionId);
		if (results.size() > 0) {
			return results.get(0);
		}
		else {
			return new ExecutionContext();
		}
	}

	/**
	 * Persist or update the execution context associated with the given
	 * jobExecution
	 * @param jobExecution
	 */
	public void persistExecutionContext(final JobExecution jobExecution) {
		Long executionId = jobExecution.getId();
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		Assert.notNull(executionId, "ExecutionId must not be null.");
		Assert.notNull(executionContext, "The ExecutionContext must not be null.");

		String serializedContext = serializeContext(executionContext);

		persistSerializedContext(executionId, serializedContext, true);
	}

	/**
	 * Persist or update the execution context associated with the given
	 * stepExecution
	 * @param stepExecution
	 */
	public void persistExecutionContext(final StepExecution stepExecution) {

		Long executionId = stepExecution.getId();
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		Assert.notNull(executionId, "ExecutionId must not be null.");
		Assert.notNull(executionContext, "The ExecutionContext must not be null.");

		String serializedContext = serializeContext(executionContext);

		persistSerializedContext(executionId, serializedContext, false);
	}

	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		serializer = new XStreamExecutionContextStringSerializer();
		((XStreamExecutionContextStringSerializer)serializer).afterPropertiesSet();
	}

	private void persistSerializedContext(final Long executionId, String serializedContext, boolean isJobExecutionContext) {
		String countSql = isJobExecutionContext ? COUNT_JOB_EXECUTION_CONTEXT : COUNT_STEP_EXECUTION_CONTEXT;
		String updateSql = isJobExecutionContext ? UPDATE_JOB_EXECUTION_CONTEXT : UPDATE_STEP_EXECUTION_CONTEXT;
		String insertSql = isJobExecutionContext ? INSERT_JOB_EXECUTION_CONTEXT : INSERT_STEP_EXECUTION_CONTEXT;

		int count = getJdbcTemplate().queryForInt(getQuery(countSql), executionId);

		final String shortContext;
		final String longContext;
		if (serializedContext.length() > MAX_VARCHAR_LENGTH) {
			shortContext = serializedContext.substring(0, MAX_VARCHAR_LENGTH - 4) + " ...";
			longContext = serializedContext;
		}
		else {
			shortContext = serializedContext;
			longContext = null;
		}


		if (count > 0) {
			getJdbcTemplate().getJdbcOperations().update(getQuery(updateSql),
					new PreparedStatementSetter() {
						public void setValues(PreparedStatement ps) throws SQLException {
							ps.setString(1, shortContext);
							if (longContext != null) {
								lobHandler.getLobCreator().setClobAsString(ps, 2, longContext);
							}
							else {
								ps.setNull(2, Types.CLOB);
							}
							ps.setLong(3, executionId);
						}
					});
		}
		else {
			getJdbcTemplate().getJdbcOperations().update(getQuery(insertSql),
					new PreparedStatementSetter() {
						public void setValues(PreparedStatement ps) throws SQLException {
							ps.setLong(1, executionId);
							ps.setString(2, shortContext);
							if (longContext != null) {
								lobHandler.getLobCreator().setClobAsString(ps, 3, longContext);
							}
							else {
								ps.setNull(3, Types.CLOB);
							}
						}
					});
		}
	}

	private String serializeContext(ExecutionContext ctx) {
		Map<String, Object> m = new HashMap<String, Object>();
		for (Entry<String, Object> me : ctx.entrySet()) {
			m.put(me.getKey(), me.getValue());
		}
		return serializer.serialize(m);
	}

	private class ExecutionContextRowMapper implements ParameterizedRowMapper<ExecutionContext> {
		public ExecutionContext mapRow(ResultSet rs, int i) throws SQLException {
			ExecutionContext executionContext = new ExecutionContext();
			String serializedContext = rs.getString("SERIALIZED_CONTEXT");
			if (serializedContext == null) {
				serializedContext = rs.getString("SHORT_CONTEXT");
			}
			Map<String, Object> map = serializer.deserialize(serializedContext);
			for (Map.Entry<String, Object> entry : map.entrySet()) {
				executionContext.put(entry.getKey(), entry.getValue());
			}
			return executionContext;
		}
	}
}

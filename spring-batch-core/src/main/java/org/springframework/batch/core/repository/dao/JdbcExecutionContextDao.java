package org.springframework.batch.core.repository.dao;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map.Entry;

import org.apache.commons.lang.SerializationUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
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
 */
public class JdbcExecutionContextDao extends AbstractJdbcBatchMetadataDao implements ExecutionContextDao {

	private static final String STEP_DISCRIMINATOR = "S";

	private static final String JOB_DISCRIMINATOR = "J";

	private static final String FIND_EXECUTION_CONTEXT = "SELECT TYPE_CD, KEY_NAME, STRING_VAL, DOUBLE_VAL, LONG_VAL, OBJECT_VAL "
			+ "from %PREFIX%EXECUTION_CONTEXT where EXECUTION_ID = ? and DISCRIMINATOR = ?";

	private static final String INSERT_STEP_EXECUTION_CONTEXT = "INSERT into %PREFIX%EXECUTION_CONTEXT(EXECUTION_ID, DISCRIMINATOR, TYPE_CD,"
			+ " KEY_NAME, STRING_VAL, DOUBLE_VAL, LONG_VAL, OBJECT_VAL) values(?,?,?,?,?,?,?,?)";

	private static final String UPDATE_STEP_EXECUTION_CONTEXT = "UPDATE %PREFIX%EXECUTION_CONTEXT set "
			+ "TYPE_CD = ?, STRING_VAL = ?, DOUBLE_VAL = ?, LONG_VAL = ?, OBJECT_VAL = ? where EXECUTION_ID = ? and KEY_NAME = ?";

	private LobHandler lobHandler = new DefaultLobHandler();

	/**
	 * @param jobExecution
	 * @return execution context associated with the given jobExecution.
	 */
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		final Long executionId = jobExecution.getId();
		Assert.notNull(executionId, "ExecutionId must not be null.");

		final ExecutionContext executionContext = new ExecutionContext();

		getJdbcTemplate().getJdbcOperations().query(getQuery(FIND_EXECUTION_CONTEXT),
				new Object[] { executionId, JOB_DISCRIMINATOR },
				new ExecutionContextRowCallbackHandler(executionContext));

		return executionContext;
	}

	/**
	 * @param stepExecution
	 * @return execution context associated with the given stepExecution.
	 */
	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		final Long executionId = stepExecution.getId();
		Assert.notNull(executionId, "ExecutionId must not be null.");

		final ExecutionContext executionContext = new ExecutionContext();

		getJdbcTemplate().getJdbcOperations().query(getQuery(FIND_EXECUTION_CONTEXT),
				new Object[] { executionId, STEP_DISCRIMINATOR },
				new ExecutionContextRowCallbackHandler(executionContext));

		return executionContext;
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

		saveOrUpdateExecutionContext(executionContext, executionId, JOB_DISCRIMINATOR);
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

		saveOrUpdateExecutionContext(executionContext, executionId, STEP_DISCRIMINATOR);
	}

	/**
	 * Resolves attribute's class to corresponding {@link AttributeType} and
	 * persists or updates the attribute.
	 */
	private void saveOrUpdateExecutionContext(ExecutionContext ctx, Long executionId, String discriminator) {

		for (Entry<String, Object> entry : ctx.entrySet()) {

			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof String) {
				updateExecutionAttribute(executionId, discriminator, key, value, AttributeType.STRING);
			}
			else if (value instanceof Double) {
				updateExecutionAttribute(executionId, discriminator, key, value, AttributeType.DOUBLE);
			}
			else if (value instanceof Long) {
				updateExecutionAttribute(executionId, discriminator, key, value, AttributeType.LONG);
			}
			else {
				updateExecutionAttribute(executionId, discriminator, key, value, AttributeType.OBJECT);
			}
		}
	}

	/**
	 * Creates {@link PreparedStatement} from the provided arguments and tries
	 * to update the attribute - if the attribute does not exist in the database
	 * yet it is inserted.
	 */
	private void updateExecutionAttribute(final Long executionId, final String discriminator, final String key,
			final Object value, final AttributeType type) {

		PreparedStatementCallback callback = new AbstractLobCreatingPreparedStatementCallback(lobHandler) {

			protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException,
					DataAccessException {

				ps.setLong(6, executionId.longValue());
				ps.setString(7, key);
				if (type == AttributeType.STRING) {
					ps.setString(1, AttributeType.STRING.toString());
					ps.setString(2, value.toString());
					ps.setDouble(3, 0.0);
					ps.setLong(4, 0);
					lobCreator.setBlobAsBytes(ps, 5, null);
				}
				else if (type == AttributeType.DOUBLE) {
					ps.setString(1, AttributeType.DOUBLE.toString());
					ps.setString(2, null);
					ps.setDouble(3, ((Double) value).doubleValue());
					ps.setLong(4, 0);
					lobCreator.setBlobAsBytes(ps, 5, null);
				}
				else if (type == AttributeType.LONG) {
					ps.setString(1, AttributeType.LONG.toString());
					ps.setString(2, null);
					ps.setDouble(3, 0.0);
					ps.setLong(4, ((Long) value).longValue());
					lobCreator.setBlobAsBytes(ps, 5, null);
				}
				else {
					ps.setString(1, AttributeType.OBJECT.toString());
					ps.setString(2, null);
					ps.setDouble(3, 0.0);
					ps.setLong(4, 0);
					lobCreator.setBlobAsBytes(ps, 5, SerializationUtils.serialize((Serializable) value));
				}
			}
		};

		// LobCreating callbacks always return the affect row count for SQL DML
		// statements, if less than 1 row
		// is affected, then this row is new and should be inserted.
		Integer affectedRows = (Integer) getJdbcTemplate().getJdbcOperations().execute(
				getQuery(UPDATE_STEP_EXECUTION_CONTEXT), callback);
		if (affectedRows.intValue() < 1) {
			insertExecutionAttribute(executionId, discriminator, key, value, type);
		}
	}

	/**
	 * Creates {@link PreparedStatement} from provided arguments and inserts new
	 * row for the attribute.
	 */
	private void insertExecutionAttribute(final Long executionId, final String discriminator, final String key,
			final Object value, final AttributeType type) {
		PreparedStatementCallback callback = new AbstractLobCreatingPreparedStatementCallback(lobHandler) {

			protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException,
					DataAccessException {

				ps.setLong(1, executionId.longValue());
				ps.setString(2, discriminator);
				ps.setString(4, key);
				if (type == AttributeType.STRING) {
					ps.setString(3, AttributeType.STRING.toString());
					ps.setString(5, value.toString());
					ps.setDouble(6, 0.0);
					ps.setLong(7, 0);
					lobCreator.setBlobAsBytes(ps, 8, null);
				}
				else if (type == AttributeType.DOUBLE) {
					ps.setString(3, AttributeType.DOUBLE.toString());
					ps.setString(5, null);
					ps.setDouble(6, ((Double) value).doubleValue());
					ps.setLong(7, 0);
					lobCreator.setBlobAsBytes(ps, 8, null);
				}
				else if (type == AttributeType.LONG) {
					ps.setString(3, AttributeType.LONG.toString());
					ps.setString(5, null);
					ps.setDouble(6, 0.0);
					ps.setLong(7, ((Long) value).longValue());
					lobCreator.setBlobAsBytes(ps, 8, null);
				}
				else {
					ps.setString(3, AttributeType.OBJECT.toString());
					ps.setString(5, null);
					ps.setDouble(6, 0.0);
					ps.setLong(7, 0);
					lobCreator.setBlobAsBytes(ps, 8, SerializationUtils.serialize((Serializable) value));
				}
			}
		};
		getJdbcTemplate().getJdbcOperations().execute(getQuery(INSERT_STEP_EXECUTION_CONTEXT), callback);
	}

	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}

	/**
	 * Attribute types supported by the {@link ExecutionContext}.
	 */
	private enum AttributeType {
		STRING, OBJECT, LONG, DOUBLE
	}

	/**
	 * Reads attributes from {@link ResultSet} and puts them into
	 * {@link ExecutionContext}, resolving the attributes' types using the
	 * 'TYPE_CD' column.
	 */
	private static class ExecutionContextRowCallbackHandler implements RowCallbackHandler {

		private ExecutionContext executionContext;

		public ExecutionContextRowCallbackHandler(ExecutionContext ctx) {
			executionContext = ctx;
		}

		public void processRow(ResultSet rs) throws SQLException {

			String typeCd = rs.getString("TYPE_CD");
			AttributeType type = AttributeType.valueOf(typeCd);
			String key = rs.getString("KEY_NAME");
			if (type == AttributeType.STRING) {
				executionContext.putString(key, rs.getString("STRING_VAL"));
			}
			else if (type == AttributeType.LONG) {
				executionContext.putLong(key, rs.getLong("LONG_VAL"));
			}
			else if (type == AttributeType.DOUBLE) {
				executionContext.putDouble(key, rs.getDouble("DOUBLE_VAL"));
			}
			else if (type == AttributeType.OBJECT) {
				executionContext.put(key, SerializationUtils.deserialize(rs.getBinaryStream("OBJECT_VAL")));
			}
			else {
				throw new UnexpectedJobExecutionException("Invalid type found: [" + typeCd + "]");
			}
		}
	};
}

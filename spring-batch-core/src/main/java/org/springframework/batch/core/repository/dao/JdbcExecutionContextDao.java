package org.springframework.batch.core.repository.dao;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
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
 * TODO 'DRY' the implementation.
 * 
 */
class JdbcExecutionContextDao extends AbstractJdbcBatchMetadataDao {
	
	private static final String STEP_DISCRIMINATOR = "S";
	
	private static final String JOB_DISCRIMINATOR = "J";
	
	private static final String FIND_EXECUTION_CONTEXT = "SELECT TYPE_CD, KEY_NAME, STRING_VAL, DOUBLE_VAL, LONG_VAL, OBJECT_VAL "
		+ "from %PREFIX%EXECUTION_CONTEXT where EXECUTION_ID = ? and DISCRIMINATOR = ?";
	
	private static final String INSERT_STEP_EXECUTION_CONTEXT = "INSERT into %PREFIX%EXECUTION_CONTEXT(EXECUTION_ID, DISCRIMINATOR, TYPE_CD,"
		+ " KEY_NAME, STRING_VAL, DOUBLE_VAL, LONG_VAL, OBJECT_VAL) values(?,?,?,?,?,?,?,?)";

	private static final String UPDATE_STEP_EXECUTION_CONTEXT = "UPDATE %PREFIX%EXECUTION_CONTEXT set "
		+ "TYPE_CD = ?, STRING_VAL = ?, DOUBLE_VAL = ?, LONG_VAL = ?, OBJECT_VAL = ? where EXECUTION_ID = ? and KEY_NAME = ?";

	private LobHandler lobHandler = new DefaultLobHandler();
	
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		final Long executionId = jobExecution.getId();
		Assert.notNull(executionId, "ExecutionId must not be null.");

		final ExecutionContext executionContext = new ExecutionContext();

		RowCallbackHandler callback = new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {

				String typeCd = rs.getString("TYPE_CD");
				AttributeType type = AttributeType.getType(typeCd);
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
					throw new UnexpectedJobExecutionException("Invalid type found: [" + typeCd
							+ "] for execution id: [" + executionId + "]");
				}
			}
		};

		getJdbcTemplate().query(getQuery(FIND_EXECUTION_CONTEXT), new Object[] { executionId, JOB_DISCRIMINATOR }, callback);

		return executionContext;
	}
	
	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		final Long executionId = stepExecution.getId();
		Assert.notNull(executionId, "ExecutionId must not be null.");

		final ExecutionContext executionContext = new ExecutionContext();

		RowCallbackHandler callback = new RowCallbackHandler() {

			public void processRow(ResultSet rs) throws SQLException {

				String typeCd = rs.getString("TYPE_CD");
				AttributeType type = AttributeType.getType(typeCd);
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
					throw new UnexpectedJobExecutionException("Invalid type found: [" + typeCd
							+ "] for execution id: [" + executionId + "]");
				}
			}
		};

		getJdbcTemplate().query(getQuery(FIND_EXECUTION_CONTEXT), new Object[] { executionId, STEP_DISCRIMINATOR }, callback);

		return executionContext;
	}
	
	public void saveOrUpdateExecutionContext(final JobExecution jobExecution) {
		Long executionId = jobExecution.getId();
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		Assert.notNull(executionId, "ExecutionId must not be null.");
		Assert.notNull(executionContext, "The ExecutionContext must not be null.");
		
		for (Iterator it = executionContext.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof String) {
				updateExecutionAttribute(executionId, JOB_DISCRIMINATOR, key, value, AttributeType.STRING);
			}
			else if (value instanceof Double) {
				updateExecutionAttribute(executionId, JOB_DISCRIMINATOR, key, value, AttributeType.DOUBLE);
			}
			else if (value instanceof Long) {
				updateExecutionAttribute(executionId, JOB_DISCRIMINATOR, key, value, AttributeType.LONG);
			}
			else {
				updateExecutionAttribute(executionId, JOB_DISCRIMINATOR, key, value, AttributeType.OBJECT);
			}
		}
	}
	
	/**
	 * Save or update execution attributes. A lob creator must be used, since
	 * any attributes that don't match a provided type must be serialized into a
	 * blob.
	 * 
	 * @see LobCreator
	 */
	public void saveOrUpdateExecutionContext(final StepExecution stepExecution) {

		Long executionId = stepExecution.getId();
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		Assert.notNull(executionId, "ExecutionId must not be null.");
		Assert.notNull(executionContext, "The ExecutionContext must not be null.");

		for (Iterator it = executionContext.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			final String key = entry.getKey().toString();
			final Object value = entry.getValue();

			if (value instanceof String) {
				updateExecutionAttribute(executionId, STEP_DISCRIMINATOR, key, value, AttributeType.STRING);
			}
			else if (value instanceof Double) {
				updateExecutionAttribute(executionId, STEP_DISCRIMINATOR, key, value, AttributeType.DOUBLE);
			}
			else if (value instanceof Long) {
				updateExecutionAttribute(executionId, STEP_DISCRIMINATOR, key, value, AttributeType.LONG);
			}
			else {
				updateExecutionAttribute(executionId, STEP_DISCRIMINATOR, key, value, AttributeType.OBJECT);
			}
		}
	}
	
	private void updateExecutionAttribute(final Long executionId, final String discriminator, final String key, final Object value,
			final AttributeType type) {

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
		Integer affectedRows = (Integer) getJdbcTemplate().execute(getQuery(UPDATE_STEP_EXECUTION_CONTEXT), callback);
		if (affectedRows.intValue() < 1) {
			insertExecutionAttribute(executionId, discriminator, key, value, type);
		}
	}
	
	private void insertExecutionAttribute(final Long executionId, final String discriminator, final String key, final Object value,
			final AttributeType type) {
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
		getJdbcTemplate().execute(getQuery(INSERT_STEP_EXECUTION_CONTEXT), callback);
	}

	public void setLobHandler(LobHandler lobHandler) {
		this.lobHandler = lobHandler;
	}
	
	public static class AttributeType {

		private final String type;

		private AttributeType(String type) {
			this.type = type;
		}

		public String toString() {
			return type;
		}

		public static final AttributeType STRING = new AttributeType("STRING");

		public static final AttributeType LONG = new AttributeType("LONG");

		public static final AttributeType OBJECT = new AttributeType("OBJECT");

		public static final AttributeType DOUBLE = new AttributeType("DOUBLE");

		private static final AttributeType[] VALUES = { STRING, OBJECT, LONG, DOUBLE };

		public static AttributeType getType(String typeAsString) {

			for (int i = 0; i < VALUES.length; i++) {
				if (VALUES[i].toString().equals(typeAsString)) {
					return (AttributeType) VALUES[i];
				}
			}

			return null;
		}
	}
	
}

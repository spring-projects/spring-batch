package org.springframework.batch.sample.common;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.lang.SerializationUtils;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Thread-safe database {@link ItemReader} implementing the process indicator
 * pattern.
 */
public class StagingItemReader<T> implements ItemReader<T>, ItemStream, InitializingBean {

	private JdbcCursorItemReader<Long> delegate;
	
	private SimpleJdbcTemplate jdbcTemplate;

	private long jobId;
	
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}

	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new SimpleJdbcTemplate(dataSource);
		delegate = new JdbcCursorItemReader<Long>();
		delegate.setDataSource(dataSource);
		delegate.setSql("SELECT ID FROM BATCH_STAGING WHERE JOB_ID=? AND PROCESSED=? ORDER BY ID");
		delegate.setPreparedStatementSetter(new PreparedStatementSetter() {
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setLong(1, jobId);
				ps.setString(2, StagingItemWriter.NEW);
			}
		});
		delegate.setMapper(new RowMapper() {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getLong(1);
			}
		});
	}

	public final void afterPropertiesSet() throws Exception {
		delegate.afterPropertiesSet();
	}

	public T read() throws Exception {

		Long id = delegate.read();
		@SuppressWarnings("unchecked")
		T result = (T) jdbcTemplate.queryForObject("SELECT VALUE FROM BATCH_STAGING WHERE ID=?",
				new ParameterizedRowMapper<Object>() {
					public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
						byte[] blob = rs.getBytes(1);
						return SerializationUtils.deserialize(blob);
					}
				}, id);

		return result;

	}

	public void close(ExecutionContext executionContext) throws ItemStreamException {
		delegate.close(executionContext);
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		delegate.open(executionContext);
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		delegate.update(executionContext);
	}

}

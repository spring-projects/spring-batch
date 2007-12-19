package org.springframework.batch.execution.repository.dao;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class MockJdbcTemplate implements JdbcOperations {

	private String sqlStatement = "";
	
	public String getSqlStatement() {
		return sqlStatement;
	}
	
	public int[] batchUpdate(String[] sql) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public int[] batchUpdate(String sql, BatchPreparedStatementSetter pss)
			throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Map call(CallableStatementCreator csc, List declaredParameters)
			throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object execute(ConnectionCallback action) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object execute(StatementCallback action) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public void execute(String sql) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
	}

	public Object execute(PreparedStatementCreator psc,
			PreparedStatementCallback action) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object execute(String sql, PreparedStatementCallback action)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object execute(CallableStatementCreator csc,
			CallableStatementCallback action) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object execute(String callString, CallableStatementCallback action)
			throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object query(String sql, ResultSetExtractor rse)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public void query(String sql, RowCallbackHandler rch)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
	}

	public List query(String sql, RowMapper rowMapper)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object query(PreparedStatementCreator psc, ResultSetExtractor rse)
			throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public void query(PreparedStatementCreator psc, RowCallbackHandler rch)
			throws DataAccessException {
		// TODO Auto-generated method stub
	}

	public List query(PreparedStatementCreator psc, RowMapper rowMapper)
			throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public Object query(String sql, PreparedStatementSetter pss,
			ResultSetExtractor rse) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object query(String sql, Object[] args, ResultSetExtractor rse)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public void query(String sql, PreparedStatementSetter pss,
			RowCallbackHandler rch) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
	}

	public void query(String sql, Object[] args, RowCallbackHandler rch)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
	}

	public List query(String sql, PreparedStatementSetter pss,
			RowMapper rowMapper) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public List query(String sql, Object[] args, RowMapper rowMapper)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object query(String sql, Object[] args, int[] argTypes,
			ResultSetExtractor rse) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public void query(String sql, Object[] args, int[] argTypes,
			RowCallbackHandler rch) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
	}

	public List query(String sql, Object[] args, int[] argTypes,
			RowMapper rowMapper) throws DataAccessException {
		// TODO Auto-generated method stub
		return null;
	}

	public int queryForInt(String sql) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public int queryForInt(String sql, Object[] args)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public int queryForInt(String sql, Object[] args, int[] argTypes)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public List queryForList(String sql) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public List queryForList(String sql, Class elementType)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public List queryForList(String sql, Object[] args)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public List queryForList(String sql, Object[] args, Class elementType)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public List queryForList(String sql, Object[] args, int[] argTypes)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public List queryForList(String sql, Object[] args, int[] argTypes,
			Class elementType) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public long queryForLong(String sql) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public long queryForLong(String sql, Object[] args)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public long queryForLong(String sql, Object[] args, int[] argTypes)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public Map queryForMap(String sql) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Map queryForMap(String sql, Object[] args)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Map queryForMap(String sql, Object[] args, int[] argTypes)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object queryForObject(String sql, RowMapper rowMapper)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object queryForObject(String sql, Class requiredType)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object queryForObject(String sql, Object[] args, RowMapper rowMapper)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object queryForObject(String sql, Object[] args, Class requiredType)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object queryForObject(String sql, Object[] args, int[] argTypes,
			RowMapper rowMapper) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public Object queryForObject(String sql, Object[] args, int[] argTypes,
			Class requiredType) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public SqlRowSet queryForRowSet(String sql) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public SqlRowSet queryForRowSet(String sql, Object[] args)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return null;
	}

	public int update(String sql) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public int update(PreparedStatementCreator psc) throws DataAccessException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder)
			throws DataAccessException {
		// TODO Auto-generated method stub
		return 0;
	}

	public int update(String sql, PreparedStatementSetter pss)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public int update(String sql, Object[] args) throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

	public int update(String sql, Object[] args, int[] argTypes)
			throws DataAccessException {
		// TODO Auto-generated method stub
		sqlStatement = sql;
		return 0;
	}

}

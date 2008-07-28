package org.springframework.batch.sample.support;

import static org.easymock.EasyMock.*;

import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.springframework.jdbc.core.RowMapper;

/**
 * Encapsulates logic for testing custom {@link RowMapper} implementations.
 * 
 * @author Robert Kasanicky
 */
public abstract class AbstractRowMapperTests extends TestCase {

	//row number should be irrelevant
	private static final int IGNORED_ROW_NUMBER = 0;
	
	//mock result set
	private ResultSet rs = createMock(ResultSet.class);

	/**
	 * @return Expected result of mapping the mock <code>ResultSet</code> by
	 * the mapper being tested.
	 */
	abstract protected Object expectedDomainObject();
	
	/**
	 * @return <code>RowMapper</code> implementation that is being tested.
	 */
	abstract protected RowMapper rowMapper();
	
	/*
	 * Define the behaviour of mock <code>ResultSet</code>.
	 */
	abstract protected void setUpResultSetMock(ResultSet rs) throws SQLException;
	
	
	/*
	 * Regular usage scenario.
	 */
	public void testRegularUse() throws SQLException {
		setUpResultSetMock(rs);
		replay(rs);
		
		assertEquals(expectedDomainObject(), rowMapper().mapRow(rs, IGNORED_ROW_NUMBER));
	}
}

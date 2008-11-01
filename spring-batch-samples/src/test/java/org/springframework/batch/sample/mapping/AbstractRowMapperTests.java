package org.springframework.batch.sample.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.easymock.MockControl;
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
	private MockControl rsControl = MockControl.createControl(ResultSet.class);
	private ResultSet rs = (ResultSet) rsControl.getMock();
	
	/**
	 * @return Expected result of mapping the mock <code>ResultSet</code> by
	 * the mapper being tested.
	 */
	abstract protected Object expectedDomainObject();
	
	/**
	 * @return <code>RowMapper</code> implementation that is being tested.
	 */
	abstract protected RowMapper rowMapper();
	
	/**
	 * Define the behaviour of mock <code>ResultSet</code>.
	 */
	abstract protected void setUpResultSetMock(ResultSet rs, MockControl rsControl) throws SQLException;
	
	
	/**
	 * Regular usage scenario.
	 */
	public void testRegularUse() throws SQLException {
		setUpResultSetMock(rs, rsControl);
		rsControl.replay();
		
		assertEquals(expectedDomainObject(), rowMapper().mapRow(rs, IGNORED_ROW_NUMBER));
	}
}

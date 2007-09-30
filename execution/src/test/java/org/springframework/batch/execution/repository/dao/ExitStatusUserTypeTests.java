package org.springframework.batch.execution.repository.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.repeat.ExitStatus;

public class ExitStatusUserTypeTests extends TestCase {

	ExitStatusUserType type = new ExitStatusUserType();
	
	public void testReturnedClass() {
		assertEquals(ExitStatus.class, type.returnedClass());
	}

	public void testSqlTypes() {
		int[] types = type.sqlTypes();
		assertEquals(3, types.length);
		assertEquals(Types.CHAR, types[0]);
	}

	public void testNullSafeGet() throws Exception {
		String[] names = new String[] {"a", "b", "c"};
		MockControl control = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) control.getMock();
		control.expectAndReturn(rs.getString(names[0]), "Y");
		control.expectAndReturn(rs.getString(names[1]), "foo");
		control.expectAndReturn(rs.getString(names[2]), "bar");
		control.replay();
		Object result = type.nullSafeGet(rs, names, null);
		assertNotNull(result);
		assertTrue(result instanceof ExitStatus);
		control.verify();
	}

	public void testNullSafeSet() throws Exception {
		MockControl control = MockControl.createControl(PreparedStatement.class);
		PreparedStatement st = (PreparedStatement) control.getMock();
		st.setString(2, "Y");
		st.setString(3, ExitStatus.CONTINUABLE.getExitCode());
		st.setString(4, ExitStatus.CONTINUABLE.getExitDescription());
		control.replay();
		type.nullSafeSet(st, ExitStatus.CONTINUABLE, 2);
		control.verify();
	}

}

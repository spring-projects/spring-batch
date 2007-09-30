package org.springframework.batch.execution.repository.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;

import junit.framework.TestCase;

public class ImmutableValueUserTypeTests extends TestCase {

	ImmutableValueUserType type = new ImmutableValueUserType() {
		public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
				throws HibernateException, SQLException {
			return null;
		}
		public void nullSafeSet(PreparedStatement st, Object value, int index)
				throws HibernateException, SQLException {
		}
		public Class returnedClass() {
			return null;
		}
		public int[] sqlTypes() {
			return null;
		}		
	};

	public void testAssemble() {
		assertEquals("foo", type.assemble("foo", null));
	}

	public void testDeepCopy() {
		assertEquals("foo", type.deepCopy("foo"));
	}

	public void testDisassemble() {
		assertEquals("foo", type.disassemble("foo"));
	}

	public void testEqualsWithEqualObjects() {
		assertTrue(type.equals("foo", "foo"));
	}

	public void testEqualsWithUnEqualObjects() {
		assertFalse(type.equals("foo", "bar"));
	}

	public void testHashCodeObject() {
		assertEquals("foo".hashCode(), type.hashCode("foo"));
	}

	public void testIsMutable() {
		assertFalse(type.isMutable());
	}

	public void testReplace() {
		assertEquals("foo",type.replace("foo", "bar", null));
	}

}

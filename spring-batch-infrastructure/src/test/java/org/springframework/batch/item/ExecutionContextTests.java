/*
 * Copyright 2006-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item;

import java.io.Serializable;

import static org.junit.Assert.*;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Lucas Ward
 *
 */
public class ExecutionContextTests {

	private ExecutionContext context;

	@Before
	public void setUp() throws Exception {
		context = new ExecutionContext();
	}
	
	@Test
	public void testNormalUsage(){
		
		context.putString("1", "testString1");
		context.putString("2", "testString2");
		context.putLong("3", 3);
		context.putDouble("4", 4.4);
		
		assertEquals("testString1", context.getString("1"));
		assertEquals("testString2", context.getString("2"));
		assertEquals("defaultString", context.getString("5", "defaultString"));
		assertEquals(4.4, context.getDouble("4"), 0);
		assertEquals(5.5, context.getDouble("5", 5.5), 0);
		assertEquals(3, context.getLong("3"));
		assertEquals(5, context.getLong("5", 5));
	}
	
	@Test
	public void testInvalidCast(){
		
		context.putLong("1", 1);
		try{
			context.getDouble("1");
			fail();
		}
		catch(ClassCastException ex){
			//expected
		}
	}
	
	@Test
	public void testIsEmpty(){
		assertTrue(context.isEmpty());
		context.putString("1", "test");
		assertFalse(context.isEmpty());
	}
	
	@Test
	public void testDirtyFlag(){
		assertFalse(context.isDirty());
		context.putString("1", "test");
		assertTrue(context.isDirty());
		context.clearDirtyFlag();
		assertFalse(context.isDirty());
	}
	
	@Test
	public void testContains(){
		context.putString("1", "testString");
		assertTrue(context.containsKey("1"));
		assertTrue(context.containsValue("testString"));
	}
	
	@Test
	public void testEquals(){
		context.putString("1", "testString");
		ExecutionContext tempContext = new ExecutionContext();
		assertFalse(tempContext.equals(context));
		tempContext.putString("1", "testString");
		assertTrue(tempContext.equals(context));
	}
	
	@Test
	public void testSerializationCheck(){
		//adding a non serializable object should cause an error.
		try{
			context.put("1", new Object());
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	@Test
	public void testPutNull(){
		//putting null should work
		context.put("1", null);
	}
	
	@Test
	public void testGetNull() {
		assertNull(context.get("does not exist"));
	}
	
	@Test
	public void testSerialization() {
		
		TestSerializable s = new TestSerializable();
		s.value = 7;
		
		context.putString("1", "testString1");
		context.putString("2", "testString2");
		context.putLong("3", 3);
		context.putDouble("4", 4.4);
		context.put("5", s);
		
		byte[] serialized = SerializationUtils.serialize(context);
		
		ExecutionContext deserialized = (ExecutionContext) SerializationUtils.deserialize(serialized);
		
		assertEquals(context, deserialized);
		assertEquals(7, ((TestSerializable)deserialized.get("5")).value);
	}
	
	@Test
	public void testCopyConstructor() throws Exception {
		ExecutionContext context = new ExecutionContext();
		context.put("foo", "bar");
		ExecutionContext copy = new ExecutionContext(context);
		assertEquals(copy, context);
	}
	
	@Test
	public void testCopyConstructorNullnNput() throws Exception {
		ExecutionContext context = new ExecutionContext((ExecutionContext)null);
		assertTrue(context.isEmpty());
	}
	
	/**
	 * Value object for testing serialization
	 */
	private static class TestSerializable implements Serializable {

		int value;

		public boolean equals(Object obj) {
			return EqualsBuilder.reflectionEquals(this, obj);
		}

		public int hashCode() {
			return HashCodeBuilder.reflectionHashCode(this);
		}
		
		
		
	}
	
}

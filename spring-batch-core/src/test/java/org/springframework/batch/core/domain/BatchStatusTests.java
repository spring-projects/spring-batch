/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.core.domain;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class BatchStatusTests extends TestCase {

	/**
	 * Test method for {@link org.springframework.batch.core.domain.BatchStatus#toString()}.
	 */
	public void testToString() {
		assertEquals("FAILED", BatchStatus.FAILED.toString());
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.BatchStatus#getStatus(java.lang.String)}.
	 */
	public void testGetStatus() {
		assertEquals(BatchStatus.FAILED, BatchStatus.getStatus(BatchStatus.FAILED.toString()));
	}

	/**
	 * Test method for {@link org.springframework.batch.core.domain.BatchStatus#getStatus(java.lang.String)}.
	 */
	public void testGetStatusWrongCode() {
		try{
			BatchStatus.getStatus("foo");
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	/**
	 * Test method for {@link org.springframework.batch.core.domain.BatchStatus#getStatus(java.lang.String)}.
	 */
	public void testGetStatusNullCode() {
		try{
			BatchStatus.getStatus(null);
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}

	public void testSerialization() throws Exception{
		
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    ObjectOutputStream out = new ObjectOutputStream (bout);

	    out.writeObject (BatchStatus.COMPLETED);
	    out.flush ();

	    ByteArrayInputStream bin = new ByteArrayInputStream (bout.toByteArray());
	    ObjectInputStream in = new ObjectInputStream(bin);

	    BatchStatus status = (BatchStatus) in.readObject ();
	    assertEquals(BatchStatus.COMPLETED, status);
	}
}

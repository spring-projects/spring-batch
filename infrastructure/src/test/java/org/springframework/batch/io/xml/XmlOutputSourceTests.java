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

package org.springframework.batch.io.xml;

import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.Random;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.easymock.internal.Range;
import org.springframework.batch.io.xml.ObjectOutput;
import org.springframework.batch.io.xml.ObjectOutputFactory;
import org.springframework.batch.io.xml.XmlOutputSource;
import org.springframework.batch.restart.RestartData;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.support.TransactionSynchronization;

import sun.security.action.GetPropertyAction;

/**
 * Unit tests for XmlOutputTemplate
 * @author peter.zozom
 * 
 */
public class XmlOutputSourceTests extends TestCase {

	private final static String OUTPUT_NAME = "xmlOutputTemplate";

	private XmlOutputSource xmlOutput;

	private MockControl ooControl;

	private ObjectOutput objectOutput;

	private MockControl oofControl;

	private ObjectOutputFactory objectOutputFactory;
	
	private String getTempDir() {
		GetPropertyAction a = new GetPropertyAction("java.io.tmpdir");
		return ((String) AccessController.doPrivileged(a));
	}

	/**
	 * Set up XmlOutputTemplate: create mock for FileLocator,
	 * ObjectOutputFactory and ObjectOutput.
	 */
	public void setUp() throws Exception {

		// create File object
		Resource file = new FileSystemResource(new File(getTempDir() + "test" + Integer.toString(new Random().nextInt() & 0xffff) + ".tmp"));

		// Create mock for ObjectOutput
		ooControl = MockControl.createControl(ObjectOutput.class);
		objectOutput = (ObjectOutput) ooControl.getMock();

		// Create mock for ObjectOutputFactory, which will return mock
		// ObjectOutput
		oofControl = MockControl.createControl(ObjectOutputFactory.class);
		objectOutputFactory = (ObjectOutputFactory) oofControl.getMock();
		objectOutputFactory.createObjectOutput(file, "UTF-8");
		oofControl.setReturnValue(objectOutput, new Range(1,2));
		oofControl.replay();

		// Create output template
		xmlOutput = new XmlOutputSource() {
			protected void registerSynchronization() {
			}
		};

		// Set up output template
		xmlOutput.setResource(file);
		xmlOutput.setEncoding("UTF-8");
		xmlOutput.setName(OUTPUT_NAME);
		xmlOutput.setOutputFactory(objectOutputFactory);
	}

	/**
	 * Tests write and close method. Also tests statistics.
	 * @throws IOException
	 */
	public void testWrite() throws IOException {

		// initialize xmlOutput
		xmlOutput.open();

		// set up ObjectOutput mock
		objectOutput.writeObject(this);
		objectOutput.writeObject(this);
		objectOutput.close();
		ooControl.replay();

		// call write method
		xmlOutput.write(this);

		// verify statistics TODO
//		Map statistics = xmlOutput.getStatistics();
//		assertEquals("1", statistics.get(XmlOutputTemplate.WRITTEN_STATISTICS_NAME));

		// call write method again
		xmlOutput.write(this);

		// call close method
		xmlOutput.close();

		// verify statistics again: count of written objects should be changed TODO
//		statistics = xmlOutput.getStatistics();
//		assertEquals("2", statistics.get(XmlOutputTemplate.WRITTEN_STATISTICS_NAME));

		// verify method calls for each mock
		oofControl.verify();
		ooControl.verify();

	}

	/**
	 * Tests handling IOException raised within write() method
	 * @throws IOException
	 */
	public void testWriteWithIOException() throws IOException {

		// initialize xmlOutput
		xmlOutput.open();

		// set up ObjectOutput mock
		objectOutput.writeObject(this);
		IOException ioe = new IOException("test");
		ooControl.setThrowable(ioe);
		objectOutput.close();
		ooControl.replay();

		try {
			xmlOutput.write(this);
			fail("BatchCriticalException was expected");
		}
		catch (DataAccessResourceFailureException bce) {
			// exceptiow was expected: caused by ioe
			assertSame(ioe, bce.getCause());
		}

	}

	/**
	 * Tests commit and rollback functionality.
	 * @throws IOException 
	 */
	public void testCommitAndRollback() throws IOException {

		// Set up ObjectOutput mock:
		objectOutput.writeObject(null);
		
		// STEP1: commit: flush output and remember commit position
		objectOutput.flush();
		objectOutput.position();
		ooControl.setReturnValue(102);
		// STEP2: rollback: check size, truncate output and set new position
		objectOutput.size();
		ooControl.setReturnValue(500); // size(=500) > newSize(=102)
		objectOutput.position(102);
		objectOutput.truncate(102);
		// STEP3: rollback with bad output size
		objectOutput.size();
		ooControl.setReturnValue(10); // size(=10) < newSize(=102)
		ooControl.replay();

		// initialize xmlOutput
		xmlOutput.open();
		xmlOutput.write(null); // because output writer is initialized in write method.

		// test commit and rollback
		// STEP1: commit
		xmlOutput.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// STEP2: rollback
		xmlOutput.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		// STEP3: rollback with bad output size
		try {
			xmlOutput.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
			fail("Exception was expected because of bad output size");
		}
		catch (IllegalStateException bce) {
			// exception is expected
			assertTrue(true);
		}

		// STEP4: call afterCompletition with status "UNKNOWN" - nothing should
		// happen
		xmlOutput.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

		// verify method calls for each mock
		oofControl.verify();
		ooControl.verify();
	}

	/**
	 * Tests restart functionality.
	 * @throws IOException 
	 */
	public void testRestart() throws IOException {

		// Set up ObjectOutput mock:
		objectOutput.writeObject(null);
		// - set position (=restartData)
		objectOutput.position();
		ooControl.setReturnValue(23001);
		objectOutput.close();
		
		objectOutput.writeObject(null);
				
		// - after restart should be called with restart data
		objectOutput.afterRestart(new Long(23001));
		// - and finaly set size (it should be verified: size >
		// newSize(=restartData))
		objectOutput.size();
		ooControl.setReturnValue(54301);
		ooControl.replay();
		
		// initialize xmlOutput
		xmlOutput.open();
		
		xmlOutput.write(null); // because output writer is initialized in write method.
		
		// get restart data
		RestartData restartData = xmlOutput.getRestartData();
		assertEquals("23001", restartData.getProperties().getProperty(XmlOutputSource.RESTART_DATA_NAME));
		xmlOutput.close();

		// init for restart
		xmlOutput.open();
		
		xmlOutput.restoreFrom(restartData);
		
		xmlOutput.write(null); // because output writer is initialized in write method.

		// verify method calls for each mock
		oofControl.verify();
		ooControl.verify();
	}

	/**
	 * Tests getFileLocatorStrategy() with fileLocatorStrategy = null
	 */
	public void testGetFileLocatorStrategyWithNullParam() throws Exception {

		// set file locator strategy to null
		xmlOutput.setResource(null);
		try {
			xmlOutput.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

}

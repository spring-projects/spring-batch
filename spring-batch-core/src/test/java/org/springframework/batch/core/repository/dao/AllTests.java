package org.springframework.batch.core.repository.dao;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.springframework.batch.core.repository.dao");
		//$JUnit-BEGIN$
		suite.addTestSuite(JdbcJobInstanceDaoTests.class);
		suite.addTestSuite(JdbcStepExecutionDaoTests.class);
		suite.addTestSuite(JdbcJobExecutionDaoTests.class);
		//$JUnit-END$
		return suite;
	}

}

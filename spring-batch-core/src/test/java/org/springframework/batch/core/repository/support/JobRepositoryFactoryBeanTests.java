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
package org.springframework.batch.core.repository.support;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.support.DataFieldMaxValueIncrementerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * @author Lucas Ward
 *
 */
public class JobRepositoryFactoryBeanTests extends TestCase{

	JobRepositoryFactoryBean factory;
	MockControl incrementerControl = MockControl.createControl(DataFieldMaxValueIncrementerFactory.class);
	DataFieldMaxValueIncrementerFactory incrementerFactory;
	DataSource dataSource;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		factory = new JobRepositoryFactoryBean();
		MockControl dataSourceControl = MockControl.createControl(DataSource.class);
		dataSource = (DataSource)dataSourceControl.getMock();
		factory.setDataSource(dataSource);
		incrementerFactory = (DataFieldMaxValueIncrementerFactory)incrementerControl.getMock();
		factory.setIncrementerFactory(incrementerFactory);
	}
	
	public void testNoDatabaseType() throws Exception{
		
		try{
			factory.afterPropertiesSet();
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testInvalidDatabaseType() throws Exception{
		
		factory.setDatabaseType("invalid type");
		try{
			factory.afterPropertiesSet();
			fail();
		}
		catch(IllegalArgumentException ex){
			//expected
		}
	}
	
	public void testCreateRepository() throws Exception{
		String databaseType = "databaseType";
		factory.setDatabaseType(databaseType);
		
		incrementerFactory.getIncrementer(databaseType, "BATCH_JOB_SEQ");
		incrementerControl.setReturnValue(new StubIncrementer());
		incrementerFactory.getIncrementer(databaseType, "BATCH_JOB_EXECUTION_SEQ");
		incrementerControl.setReturnValue(new StubIncrementer());
		incrementerFactory.getIncrementer(databaseType, "BATCH_STEP_EXECUTION_SEQ");
		incrementerControl.setReturnValue(new StubIncrementer());
		incrementerControl.replay();
		
		factory.getObject();
		
		incrementerControl.verify();
	}
	
	private class StubIncrementer implements DataFieldMaxValueIncrementer {

		public int nextIntValue() throws DataAccessException {
			return 0;
		}

		public long nextLongValue() throws DataAccessException {
			return 0;
		}

		public String nextStringValue() throws DataAccessException {
			return null;
		}
		
	}
}

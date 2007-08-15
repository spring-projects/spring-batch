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

package org.springframework.batch.execution.repository.dao;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.util.ClassUtils;

public class HibernateStepDaoTests extends SqlStepDaoTests {

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	protected String[] getConfigLocations() {
		return new String[] { ClassUtils.addResourcePathToPackagePath(getClass(), "hibernate-dao-test.xml") };
	}

	public void testSaveStatistics() throws Exception {
		StepInstance step = stepDao.createStep(job, "foo");
		StepExecution stepExecution = new StepExecution(step.getId(), new Long(10));
		Properties statistics = new Properties();
		statistics.setProperty("x", "y");
		statistics.setProperty("a", "b");
		stepExecution.setStatistics(statistics);
		stepDao.save(stepExecution);
		sessionFactory.getCurrentSession().flush();
		String returnedStatistics = (String) jdbcTemplate.queryForObject(
				"SELECT TASK_STATISTICS from BATCH_STEP_EXECUTION where ID=?", new Object[] { stepExecution.getId() },
				String.class);
		
		Properties fromDb = PropertiesConverter.stringToProperties(returnedStatistics);
		//assertEquals("x=y, a=b", returnedStatistics);
		assertEquals(fromDb, statistics);
	}
}

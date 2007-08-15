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

package org.springframework.batch.sample;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.execution.JobExecutorFacade;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Only runs a job, not a real test
 * 
 * @author robert.kasanicky
 * 
 */
public abstract class AbstractJobTests extends TestCase {

	public static final String JOB_CONFIGURATION_BEAN_ID = "jobConfiguration";

	public static final String BATCH_CONTAINER_BEAN_ID = "batchContainer";

	private static final Log log = LogFactory.getLog(AbstractJobTests.class);

	public void testRunJob() throws Exception {
		BeanFactory ctx = loadContext();

		JobConfiguration jobConfig = (JobConfiguration) ctx.getBean(JOB_CONFIGURATION_BEAN_ID);
		JobExecutorFacade batchContainer = (JobExecutorFacade) ctx.getBean(BATCH_CONTAINER_BEAN_ID);
		assertNotNull(jobConfig);
		assertNotNull(batchContainer);
		
		log.info(jobConfig.getName() + " started");
//		batchContainer.start();
//		while (batchContainer.isRunning()) {
//			Thread.sleep(100);
//		}
		log.info(jobConfig.getName() + " finished successfully");
	}

	// @Override
	abstract protected ConfigurableApplicationContext loadContext();

}

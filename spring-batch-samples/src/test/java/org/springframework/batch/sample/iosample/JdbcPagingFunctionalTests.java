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

package org.springframework.batch.sample.iosample;

import org.junit.runner.RunWith;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.batch.test.MetaDataInstanceFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Garrette
 * @author Dave Syer
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/jobs/iosample/jdbcPaging.xml")
public class JdbcPagingFunctionalTests extends AbstractIoSampleTests {

	@Override
	protected void pointReaderToOutput(ItemReader<CustomerCredit> reader) {
		JobParameters jobParameters = new JobParametersBuilder(super.getUniqueJobParameters()).addDouble("credit", 0.)
				.toJobParameters();
		StepExecution stepExecution = MetaDataInstanceFactory.createStepExecution(jobParameters);
		StepSynchronizationManager.close();
		StepSynchronizationManager.register(stepExecution);
	}

	@Override
	protected JobParameters getUniqueJobParameters() {
		return new JobParametersBuilder(super.getUniqueJobParameters()).addDouble("credit", 10000.).toJobParameters();
	}

}

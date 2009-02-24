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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dan Garrette
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/ioSampleJob.xml",
		"/jobs/iosample/multiResource.xml" })
public class MultiResourceFunctionalTests extends AbstractIoSampleTests {

	@Override
	protected void pointReaderToOutput(ItemReader<CustomerCredit> reader) {
		MultiResourceItemReader<?> multiReader = (MultiResourceItemReader<?>) reader;
		multiReader.setResources(new Resource[] {
				new FileSystemResource("target/test-outputs/multiResourceOutput.csv.1"),
				new FileSystemResource("target/test-outputs/multiResourceOutput.csv.2") });

	}
	
	@Override
	protected JobParameters getUniqueJobParameters() {
		JobParametersBuilder builder = new JobParametersBuilder(super.getUniqueJobParameters());
		return builder.addString("file.path", "classpath:data/iosample/input/").toJobParameters();
	}

}

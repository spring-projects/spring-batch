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
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/ioSampleJob.xml",
		"/jobs/iosample/fixedLength.xml" })
public class FixedLengthFunctionalTests extends AbstractIoSampleTests {

	@Autowired
	private Resource outputResource;

	@Override
	protected void pointReaderToOutput(ItemReader<CustomerCredit> reader) {
		FlatFileItemReader<?> fileReader = (FlatFileItemReader<?>) reader;
		fileReader.setResource(outputResource);
	}
}

/*
 * Copyright 2006-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.sample.iosample;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.sample.domain.trade.CustomerCredit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Dan Garrette
 * @since 2.0
 */
@SpringJUnitConfig(locations = "/jobs/iosample/xml.xml")
class XmlFunctionalTests extends AbstractIoSampleTests {

	@Autowired
	private Resource outputResource;

	@Override
	protected void pointReaderToOutput(ItemReader<CustomerCredit> reader) {
		StaxEventItemReader<?> xmlReader = (StaxEventItemReader<?>) reader;
		xmlReader.setResource(outputResource);
	}

}

/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.samples.loop;

import org.junit.jupiter.api.Test;

import org.springframework.batch.samples.domain.trade.internal.ItemTrackingTradeItemWriter;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that expected number of items have been processed.
 *
 * @author Dan Garrette
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */

@SpringJUnitConfig(locations = { "/simple-job-launcher-context.xml",
		"/org/springframework/batch/samples/loop/loopFlowSample.xml" })
class LoopFlowSampleFunctionalTests {

	@Autowired
	private ItemTrackingTradeItemWriter itemWriter;

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@Test
	void testJobLaunch() throws Exception {
		this.jobOperatorTestUtils.startJob();
		// items processed = items read + 2 exceptions
		assertEquals(10, itemWriter.getItems().size());
	}

}

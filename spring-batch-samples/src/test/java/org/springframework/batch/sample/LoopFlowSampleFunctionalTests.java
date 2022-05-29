/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.trade.internal.ItemTrackingTradeItemWriter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Checks that expected number of items have been processed.
 *
 * @author Dan Garrette
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
		locations = { "/simple-job-launcher-context.xml", "/jobs/loopFlowSample.xml", "/job-runner-context.xml" })
public class LoopFlowSampleFunctionalTests {

	@Autowired
	private ItemTrackingTradeItemWriter itemWriter;

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void testJobLaunch() throws Exception {
		jobLauncherTestUtils.launchJob();
		// items processed = items read + 2 exceptions
		assertEquals(10, itemWriter.getItems().size());
	}

}

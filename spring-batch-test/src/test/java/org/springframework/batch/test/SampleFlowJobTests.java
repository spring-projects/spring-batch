/*
 * Copyright 2009-2022 the original author or authors.
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
package org.springframework.batch.test;

import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * This class will specifically test the capabilities of {@link JobRepositoryTestUtils} to
 * test {@link FlowJob}s.
 *
 * @author Dan Garrette
 * @since 2.0
 */
@SpringJUnitConfig(locations = "/jobs/sampleFlowJob.xml")
class SampleFlowJobTests extends AbstractSampleJobTests {

}

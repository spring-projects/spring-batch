package org.springframework.batch.integration;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.batch.integration.chunk.RemoteChunkFaultTolerantStepJmsIntegrationTests;
import org.springframework.batch.integration.partition.JmsIntegrationTests;

/*
 * Copyright 2009-2010 the original author or authors.
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

/**
 * A test suite that is ignored, but can be resurrected to help debug ordering issues in tests.
 * 
 * @author Dave Syer
 * 
 */
@RunWith(Suite.class)
@SuiteClasses(value = { JmsIntegrationTests.class,
		RemoteChunkFaultTolerantStepJmsIntegrationTests.class })
@Ignore
public class IgnoredTestSuite {

}

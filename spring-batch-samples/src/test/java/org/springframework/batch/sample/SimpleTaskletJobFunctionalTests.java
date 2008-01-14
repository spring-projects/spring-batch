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

/**
 * This job differs from FixedLengthImportJob only in internal job, therefore
 * the test is reused, only the job config location is overridden
 */
public class SimpleTaskletJobFunctionalTests extends FixedLengthImportJobFunctionalTests {
	
//	@Override
	protected String[] getConfigLocations() {
		return new String[]{"jobs/simpleTaskletJob.xml"};
	}

}

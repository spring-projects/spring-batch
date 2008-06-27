/*
 * Copyright 2002-2007 the original author or authors.
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
package org.springframework.batch.core.step;

import junit.framework.TestCase;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

public class NoItemsProcessedDetectorTests extends TestCase {

    private NoItemsProcessedDetector tested = new NoItemsProcessedDetector();

    /**
     *  If item count is zero exception is thrown
     */
    public void testAfterStep() {
        StepExecution stepExecution = new StepExecution("NoProcessingStep",
                new JobExecution(
                new JobInstance(new Long(1), new JobParameters(), "NoProcessingJob")));

        stepExecution.setItemCount(0);

        try {
            tested.afterStep(stepExecution);
            fail();
        } catch (RuntimeException e) {
           assertEquals("Step has not processed any items", e.getMessage());
        }
    }
}

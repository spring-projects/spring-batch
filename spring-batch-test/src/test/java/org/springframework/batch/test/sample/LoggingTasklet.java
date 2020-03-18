/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.batch.test.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;

public class LoggingTasklet implements Tasklet {

        protected static final Log logger = LogFactory.getLog(LoggingTasklet.class);

        private int id = 0;

        public LoggingTasklet(int id) {
                this.id = id;
        }

        @Nullable
		@Override
        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                logger.info("tasklet executing: id=" + id);
                return RepeatStatus.FINISHED;
        }
}

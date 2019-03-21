/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.batch.api.chunk.AbstractItemWriter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ChunkListenerParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Autowired
	public SpringChunkListener springChunkListener;

	@Autowired
	public JsrChunkListener jsrChunkListener;

	@Test
	public void test() throws Exception {
		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.FAILED, execution.getStatus());
		assertEquals(3, execution.getStepExecutions().size());
		assertEquals(4, springChunkListener.beforeChunkCount);
		assertEquals(3, springChunkListener.afterChunkCount);
		assertEquals(4, jsrChunkListener.beforeChunkCount);
		assertEquals(3, jsrChunkListener.afterChunkCount);
		assertEquals(1, springChunkListener.afterChunkErrorCount);
		assertEquals(1, jsrChunkListener.afterChunkErrorCount);
	}

	public static class SpringChunkListener implements ChunkListener {

		protected int beforeChunkCount = 0;
		protected int afterChunkCount = 0;
		protected int afterChunkErrorCount = 0;

		@Override
		public void beforeChunk(ChunkContext context) {
			beforeChunkCount++;
		}

		@Override
		public void afterChunk(ChunkContext context) {
			afterChunkCount++;
		}

		@Override
		public void afterChunkError(ChunkContext context) {
			afterChunkErrorCount++;
		}
	}

	public static class JsrChunkListener implements javax.batch.api.chunk.listener.ChunkListener {

		protected int beforeChunkCount = 0;
		protected int afterChunkCount = 0;
		protected int afterChunkErrorCount = 0;

		@Override
		public void beforeChunk() throws Exception {
			beforeChunkCount++;
		}

		@Override
		public void onError(Exception ex) throws Exception {
			afterChunkErrorCount++;
		}

		@Override
		public void afterChunk() throws Exception {
			afterChunkCount++;
		}
	}

	public static class ErrorThrowingItemWriter extends AbstractItemWriter {

		@Override
		public void writeItems(List<Object> items) throws Exception {
			throw new Exception("This should cause the rollback");
		}
	}
}

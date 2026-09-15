/*
 * Copyright 2026 the original author or authors.
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
package org.springframework.batch.core.observability;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.observability.BatchEventRecorder.BatchEvent;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for {@link BatchEventRecorder}.
 *
 * @author Fabio Molignoni
 */
class BatchEventRecorderTests {

	@Test
	void defaultRecorderShouldCreateNoOpEvents() {
		BatchEventRecorder recorder = BatchEventRecorder.DEFAULT;

		assertAll(() -> assertSame(BatchEvent.DEFAULT, recorder.createJobLaunchEvent("job", "parameters")),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createJobExecutionEvent("job", 1, 2)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createStepExecutionEvent("step", "job", 3, 2)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createTaskletExecutionEvent("step", 3, "tasklet")),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createPartitionSplitEvent("step", 3)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createPartitionAggregateEvent("step", 3)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createChunkTransactionEvent("step", 3)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createChunkScanEvent("step", 3)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createItemReadEvent("step", 3)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createItemProcessEvent("step", 3)),
				() -> assertSame(BatchEvent.DEFAULT, recorder.createChunkWriteEvent("step", 3, 4)));
	}

	@Test
	void defaultEventShouldAcceptLifecycleCallbacks() {
		assertDoesNotThrow(() -> {
			BatchEvent.DEFAULT.begin();
			BatchEvent.DEFAULT.setStatus("COMPLETED");
			BatchEvent.DEFAULT.setCount(1);
			BatchEvent.DEFAULT.commit();
		});
	}

}

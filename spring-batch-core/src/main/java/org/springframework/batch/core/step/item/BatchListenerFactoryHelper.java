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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.CompositeChunkListener;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.listener.RepeatListenerSupport;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;

/**
 * Package private helper for step factory beans.
 * 
 * @author Dave Syer
 * 
 */
abstract class BatchListenerFactoryHelper {

	/**
	 * @param chunkOperations
	 * @param listeners
	 */
	public static RepeatOperations addChunkListeners(RepeatOperations chunkOperations, StepListener[] listeners) {

		final CompositeChunkListener multicaster = new CompositeChunkListener();

		boolean hasChunkListener = false;

		for (int i = 0; i < listeners.length; i++) {
			StepListener listener = listeners[i];
			if (listener instanceof ChunkListener) {
				hasChunkListener = true;
			}
			if (listener instanceof ChunkListener) {
				multicaster.register((ChunkListener) listener);
			}
		}

		if (hasChunkListener) {

			Assert.state(chunkOperations instanceof RepeatTemplate,
					"Chunk operations is injected but not a RepeatTemplate, so chunk listeners cannot also be registered. "
							+ "Either inject a RepeatTemplate, or remove the ChunkListener.");

			RepeatTemplate stepTemplate = (RepeatTemplate) chunkOperations;
			stepTemplate.registerListener(new RepeatListenerSupport() {
				public void open(RepeatContext context) {
					multicaster.beforeChunk();
				}

				public void close(RepeatContext context) {
					multicaster.afterChunk();
				}
			});

		}

		return chunkOperations;

	}

	public static <T> List<T> getListeners(StepListener[] listeners, Class<? super T> cls) {
		List<T> list = new ArrayList<T>();
		for (int i = 0; i < listeners.length; i++) {
			StepListener stepListener = listeners[i];
			if (cls.isAssignableFrom(stepListener.getClass())) {
				@SuppressWarnings("unchecked")
				T listener = (T) stepListener;
				list.add(listener);
			}
		}
		return list;
	}

}

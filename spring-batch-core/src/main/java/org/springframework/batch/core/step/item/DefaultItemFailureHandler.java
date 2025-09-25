/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.step.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.item.Chunk;

/**
 * Default implementation of the {@link ItemListenerSupport} class that writes all
 * exceptions via commons logging. Since generics can't be used to ensure the list
 * contains exceptions, any non exceptions will be logged out by calling toString on the
 * object.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @deprecated Since 6.0 with no replacement. Scheduled for removal in 7.0.
 */
@NullUnmarked
@Deprecated(since = "6.0", forRemoval = true)
public class DefaultItemFailureHandler extends ItemListenerSupport<Object, Object> {

	protected static final Log logger = LogFactory.getLog(DefaultItemFailureHandler.class);

	@Override
	public void onReadError(Exception ex) {
		try {
			logger.error("Error encountered while reading", ex);
		}
		catch (Exception exception) {
			logger.error("Invalid type for logging: [" + exception + "]");
		}
	}

	@Override
	public void onWriteError(Exception ex, Chunk<?> item) {
		try {
			logger.error("Error encountered while writing item: [ " + item + "]", ex);
		}
		catch (Exception exception) {
			logger.error("Invalid type for logging: [" + exception + "]");
		}
	}

}

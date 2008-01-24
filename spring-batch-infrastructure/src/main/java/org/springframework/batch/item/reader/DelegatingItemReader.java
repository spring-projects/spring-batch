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

package org.springframework.batch.item.reader;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemReader}. The input source is expected to
 * take care of open and close operations. If necessary it should be registered
 * as a step scoped bean to ensure that the lifecycle methods are called.
 *
 * @author Dave Syer
 */
public class DelegatingItemReader extends AbstractItemReader implements Restartable, Skippable, InitializingBean{

	private ItemReader inputSource;

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(inputSource, "ItemReader must not be null.");
	}
	/**
	 * Get the next object from the input source.
	 * @throws Exception 
	 * @see org.springframework.batch.item.ItemReader#read()
	 */
	public Object read() throws Exception {
		return inputSource.read();
	}

	/**
	 * @see Restartable#getRestartData()
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link Restartable}.
	 */
	public RestartData getRestartData() {
		if (!(inputSource instanceof Restartable)) {
			throw new IllegalStateException("Input Template is not Restartable");
		}
		return ((Restartable) inputSource).getRestartData();
	}

	/**
	 * @see Restartable#restoreFrom(RestartData)
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link Restartable}.
	 */
	public void restoreFrom(RestartData data) {
		if (!(inputSource instanceof Restartable)) {
			throw new IllegalStateException("Input Template is not Restartable");
		}
		((Restartable) inputSource).restoreFrom(data);
	}

	/**
	 * Setter for input source.
	 * @param source
	 */
	public void setItemReader(ItemReader source) {
		this.inputSource = source;
	}

	public ItemReader getItemReader() {
		return inputSource;
	}

	public void skip() {
		if (inputSource instanceof Skippable) {
			((Skippable)inputSource).skip();
		}
	}
}

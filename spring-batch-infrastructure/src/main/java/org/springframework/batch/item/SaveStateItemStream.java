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

package org.springframework.batch.item;

/**
 * <p>
 * Marker interface defining a contract for setting if the item stream can disable
 * state saving.
 * <p>
 * 
 * @author Dean de Bree
 * 
 */
public interface SaveStateItemStream extends ItemStream {

    
    /**
	 * Set the boolean indicating whether or not state should be saved in the provided {@link ExecutionContext} during
	 * the {@link ItemStream} call to update.
	 * 
	 * @param saveState
	 */
    
    /**
	 * Set the flag indicating whether or not state should be saved in the
	 * provided {@link ExecutionContext} during the {@link ItemStream} call to
	 * update. Setting this to false means that it will always start at the
	 * beginning on a restart.
	 * 
	 * @param saveState
	 */
    
	/**
	 * Set the flag that determines whether to save internal data for
	 * {@link ExecutionContext}. Only switch this to false if you don't want to
	 * save any state from this stream, and you don't need it to be restartable.
	 * Always set it to false if the reader is being used in a concurrent
	 * environment.
	 * 
	 * @param saveState flag value (default true).
	 */
	void setSaveState(boolean saveState);

	/**
	 * The flag that determines whether to save internal state for restarts.
	 * @return true if the flag was set
	 */
	boolean isSaveState();
}

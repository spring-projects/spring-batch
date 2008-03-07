/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item.writer;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;

/**
 * Abstract {@link ItemWriter} that allows for base classes to only
 * implement the close method if they need it.  Because it is likely
 * that the flush and clear methods may not need to be implemented,
 * they are provided in this class.
 * 
 * @author Lucas Ward
 */
public abstract class AbstractItemWriter implements ItemWriter{
	
	public void flush() throws FlushFailedException {
	}
	
	public void clear() throws ClearFailedException {
	}
}

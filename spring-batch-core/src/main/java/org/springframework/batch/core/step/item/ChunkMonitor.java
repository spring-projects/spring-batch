/*
 * Copyright 2006-2013 the original author or authors.
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
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.support.CompositeItemStream;

/**
 * Manage the offset data between the last successful commit and updates made to
 * an input chunk. Only works with single threaded steps because it has to use a
 * {@link ThreadLocal} to manage the state and coordinate between the caller
 * and the wrapped {@link ItemStream}.
 *
 * @author Dave Syer
 * @since 2.0
 */
public class ChunkMonitor extends ItemStreamSupport {

	private Log logger = LogFactory.getLog(getClass());

	private boolean streamsRegistered = false;

	public static class ChunkMonitorData {
		public int offset;

		public int chunkSize;

		public ChunkMonitorData(int offset, int chunkSize) {
			this.offset = offset;
			this.chunkSize = chunkSize;
		}
	}

	private static final String OFFSET = "OFFSET";

	private CompositeItemStream stream = new CompositeItemStream();

	private ThreadLocal<ChunkMonitorData> holder = new ThreadLocal<>();

	private ItemReader<?> reader;

	public ChunkMonitor() {
		this.setExecutionContextName(ChunkMonitor.class.getName());
	}

	/**
	 * @param stream the stream to set
	 */
	public void registerItemStream(ItemStream stream) {
		streamsRegistered = true;
		this.stream.register(stream);
	}

	/**
	 * @param reader the reader to set
	 */
	public void setItemReader(ItemReader<?> reader) {
		this.reader = reader;
	}

	public void incrementOffset() {
		ChunkMonitorData data = getData();
		data.offset ++;
		if (data.offset >= data.chunkSize) {
			resetOffset();
		}
	}

	public int getOffset() {
		return getData().offset;
	}

	public void resetOffset() {
		getData().offset = 0;
	}

	public void setChunkSize(int chunkSize) {
		getData().chunkSize = chunkSize;
		resetOffset();
	}

	@Override
	public void close() throws ItemStreamException {
		super.close();
		holder.set(null);
		if (streamsRegistered) {
			stream.close();
		}
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		super.open(executionContext);
		if (streamsRegistered) {
			stream.open(executionContext);
			ChunkMonitorData data = new ChunkMonitorData(executionContext.getInt(getExecutionContextKey(OFFSET), 0), 0);
			holder.set(data);
			if (reader == null) {
				logger.warn("No ItemReader set (must be concurrent step), so ignoring offset data.");
				return;
			}
			for (int i = 0; i < data.offset; i++) {
				try {
					reader.read();
				}
				catch (Exception e) {
					throw new ItemStreamException("Could not position reader with offset: " + data.offset, e);
				}
			}

			resetOffset();
		}
	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		if (streamsRegistered) {
			ChunkMonitorData data = getData();
			if (data.offset == 0) {
				// Only call the underlying update method if we are on a chunk
				// boundary
				stream.update(executionContext);
				executionContext.remove(getExecutionContextKey(OFFSET));
			}
			else {
				executionContext.putInt(getExecutionContextKey(OFFSET), data.offset);
			}
		}
	}

	private ChunkMonitorData getData() {
		ChunkMonitorData data = holder.get();
		if (data==null) {
			if (streamsRegistered) {
				logger.warn("ItemStream was opened in a different thread.  Restart data could be compromised.");
			}
			data = new ChunkMonitorData(0,0);
			holder.set(data);
		}
		return data;
	}

}

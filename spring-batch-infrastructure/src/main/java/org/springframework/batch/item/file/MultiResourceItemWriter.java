/*
 * Copyright 2006-2017 the original author or authors.
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

package org.springframework.batch.item.file;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.support.AbstractItemStreamItemWriter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Wraps a {@link ResourceAwareItemWriterItemStream} and creates a new output
 * resource when the count of items written in current resource exceeds
 * {@link #setItemCountLimitPerResource(int)}. Suffix creation can be customized
 * with {@link #setResourceSuffixCreator(ResourceSuffixCreator)}.
 * 
 * Note that new resources are created only at chunk boundaries i.e. the number
 * of items written into one resource is between the limit set by
 * {@link #setItemCountLimitPerResource(int)} and (limit + chunk size).
 * 
 * @param <T> item type
 * 
 * @author Robert Kasanicky
 */
public class MultiResourceItemWriter<T> extends AbstractItemStreamItemWriter<T> {

	final static private String RESOURCE_INDEX_KEY = "resource.index";

	final static private String CURRENT_RESOURCE_ITEM_COUNT = "resource.item.count";

	private Resource resource;

	private ResourceAwareItemWriterItemStream<? super T> delegate;

	private int itemCountLimitPerResource = Integer.MAX_VALUE;

	private int currentResourceItemCount = 0;

	private int resourceIndex = 1;

	private ResourceSuffixCreator suffixCreator = new SimpleResourceSuffixCreator();

	private boolean saveState = true;

	private boolean opened = false;

	public MultiResourceItemWriter() {
		this.setExecutionContextName(ClassUtils.getShortName(MultiResourceItemWriter.class));
	}

    @Override
	public void write(List<? extends T> items) throws Exception {
		if (!opened) {
			File file = setResourceToDelegate();
			// create only if write is called
			file.createNewFile();
			Assert.state(file.canWrite(), "Output resource " + file.getAbsolutePath() + " must be writable");
			delegate.open(new ExecutionContext());
			opened = true;
		}
		delegate.write(items);
		currentResourceItemCount += items.size();
		if (currentResourceItemCount >= itemCountLimitPerResource) {
			delegate.close();
			resourceIndex++;
			currentResourceItemCount = 0;
			setResourceToDelegate();
			opened = false;
		}
	}

	/**
	 * Allows customization of the suffix of the created resources based on the
	 * index.
	 *
	 * @param suffixCreator {@link ResourceSuffixCreator} to be used by the writer.
	 */
	public void setResourceSuffixCreator(ResourceSuffixCreator suffixCreator) {
		this.suffixCreator = suffixCreator;
	}

	/**
	 * After this limit is exceeded the next chunk will be written into newly
	 * created resource.
	 *
	 * @param itemCountLimitPerResource int item threshold used to determine when a new
	 * resource should be created.
	 */
	public void setItemCountLimitPerResource(int itemCountLimitPerResource) {
		this.itemCountLimitPerResource = itemCountLimitPerResource;
	}

	/**
	 * Delegate used for actual writing of the output.
	 *
	 * @param delegate {@link ResourceAwareItemWriterItemStream} that will be used
	 * to write the output.
	 */
	public void setDelegate(ResourceAwareItemWriterItemStream<? super T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Prototype for output resources. Actual output files will be created in
	 * the same directory and use the same name as this prototype with appended
	 * suffix (according to
	 * {@link #setResourceSuffixCreator(ResourceSuffixCreator)}.
	 *
	 * @param resource The prototype resource.
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}


	/**
	 * Indicates that the state of the reader will be saved after each commit.
	 *
	 * @param saveState true the state is saved.
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

    @Override
	public void close() throws ItemStreamException {
		super.close();
		resourceIndex = 1;
		currentResourceItemCount = 0;
		if (opened) {
			delegate.close();
		}
	}

    @Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		super.open(executionContext);
		resourceIndex = executionContext.getInt(getExecutionContextKey(RESOURCE_INDEX_KEY), 1);
		currentResourceItemCount = executionContext.getInt(getExecutionContextKey(CURRENT_RESOURCE_ITEM_COUNT), 0);

		try {
			setResourceToDelegate();
		}
		catch (IOException e) {
			throw new ItemStreamException("Couldn't assign resource", e);
		}

		if (executionContext.containsKey(getExecutionContextKey(CURRENT_RESOURCE_ITEM_COUNT))) {
			// It's a restart
			delegate.open(executionContext);
			opened = true;
		}
		else {
			opened = false;
		}
	}

    @Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		if (saveState) {
			if (opened) {
				delegate.update(executionContext);
			}
			executionContext.putInt(getExecutionContextKey(CURRENT_RESOURCE_ITEM_COUNT), currentResourceItemCount);
			executionContext.putInt(getExecutionContextKey(RESOURCE_INDEX_KEY), resourceIndex);
		}
	}

	/**
	 * Create output resource (if necessary) and point the delegate to it.
	 */
	private File setResourceToDelegate() throws IOException {
		String path = resource.getFile().getAbsolutePath() + suffixCreator.getSuffix(resourceIndex);
		File file = new File(path);
		delegate.setResource(new FileSystemResource(file));
		return file;
	}
}

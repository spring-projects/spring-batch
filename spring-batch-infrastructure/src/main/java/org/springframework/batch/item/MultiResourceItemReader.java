package org.springframework.batch.item;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Reads items from multiple resources sequentially - resource list is given by
 * {@link #setResources(Resource[])}, the actual reading is delegated to
 * {@link #setDelegate(ResourceAwareItemReaderItemStream)}.
 * 
 * Reset (rollback) capability is implemented by item buffering. To restart
 * correctly resource ordering needs to be preserved between runs.
 * 
 * @see SortedMultiResourceItemReader
 * 
 * @author Robert Kasanicky
 */
public class MultiResourceItemReader extends AbstractBufferedItemReaderItemStream implements InitializingBean {

	private ResourceAwareItemReaderItemStream delegate;

	private Resource[] resources;

	private int currentResourceIndex = 0;

	public MultiResourceItemReader() {
		setName(ClassUtils.getShortName(MultiResourceItemReader.class));
	}

	/**
	 * @param delegate reads items from single {@link Resource}.
	 */
	public void setDelegate(ResourceAwareItemReaderItemStream delegate) {
		this.delegate = delegate;
	}

	/**
	 * Make sure resources are set.
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notEmpty(resources, "There must be at least one input resource");
		Assert.notNull(delegate);
	}

	/**
	 * @param resources input resources
	 */
	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	/**
	 * Close the delegate and reset resource index.
	 * 
	 * @see AbstractBufferedItemReaderItemStream#doClose()
	 */
	protected void doClose() throws Exception {
		currentResourceIndex = 0;
		delegate.close(new ExecutionContext());
	}

	/**
	 * Open the delegate pointing it to the first resource.
	 * 
	 * @see AbstractBufferedItemReaderItemStream#doOpen()
	 */
	protected void doOpen() throws Exception {
		delegate.setResource(resources[0]);
		delegate.open(new ExecutionContext());
	}

	/**
	 * Use the delegate to read next item, jump to next resource if the item is
	 * null.
	 * 
	 * @see AbstractBufferedItemReaderItemStream#doRead()
	 */
	protected Object doRead() throws Exception {

		Object item = delegate.read();

		while (item == null) {

			if (++currentResourceIndex >= resources.length) {
				return null;
			}
			delegate.close(new ExecutionContext());
			delegate.setResource(resources[currentResourceIndex]);
			delegate.open(new ExecutionContext());
			item = delegate.read();
		}

		return item;
	}

}

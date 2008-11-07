package org.springframework.batch.item.file;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NoWorkFoundException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Reads items from multiple resources sequentially - resource list is given by
 * {@link #setResources(Resource[])}, the actual reading is delegated to
 * {@link #setDelegate(ResourceAwareItemReaderItemStream)}.
 * 
 * Input resources are ordered using {@link #setComparator(Comparator)} to make
 * sure resource ordering is preserved between job runs in restart scenario.
 * 
 * Reset (rollback) capability is implemented by item buffering.
 * 
 * 
 * @author Robert Kasanicky
 */
public class MultiResourceItemReader<T> implements ItemReader<T>, ItemStream {
	
	private static final Log logger = LogFactory.getLog(MultiResourceItemReader.class);

	private final ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport();

	private ResourceAwareItemReaderItemStream<? extends T> delegate;

	private Resource[] resources;

	private MultiResourceIndex index = new MultiResourceIndex();

	private boolean saveState = true;
	
	// signals there are no resources to read -> just return null on first read
	private boolean noInput;

	private Comparator<Resource> comparator = new Comparator<Resource>() {

		/**
		 * Compares resource filenames.
		 */
		public int compare(Resource r1, Resource r2) {
			return r1.getFilename().compareTo(r2.getFilename());
		}

	};

	public MultiResourceItemReader() {
		executionContextUserSupport.setName(ClassUtils.getShortName(MultiResourceItemReader.class));
	}

	/**
	 * Reads the next item, jumping to next resource if necessary.
	 */
	public T read() throws Exception, UnexpectedInputException, NoWorkFoundException, ParseException {

		if (noInput) {
			return null;
		}
		
		T item;
		item = readNextItem();
		index.incrementItemCount();

		return item;
	}

	/**
	 * Use the delegate to read the next item, jump to next resource if current
	 * one is exhausted. Items are appended to the buffer.
	 * @return next item from input
	 */
	private T readNextItem() throws Exception {

		T item = delegate.read();

		while (item == null) {

			index.incrementResourceCount();

			if (index.currentResource >= resources.length) {
				return null;
			}

			delegate.close(new ExecutionContext());
			delegate.setResource(resources[index.currentResource]);
			delegate.open(new ExecutionContext());

			item = delegate.read();
		}

		return item;
	}

	/**
	 * Close the {@link #setDelegate(ResourceAwareItemReaderItemStream)} reader
	 * and reset instance variable values.
	 */
	public void close(ExecutionContext executionContext) throws ItemStreamException {
		index = new MultiResourceIndex();
		delegate.close(new ExecutionContext());
		noInput = false;
	}

	/**
	 * Figure out which resource to start with in case of restart, open the
	 * delegate and restore delegate's position in the resource.
	 */
	public void open(ExecutionContext executionContext) throws ItemStreamException {

		Assert.notNull(resources, "Resources must be set");
		
		noInput = false;
		if (resources.length == 0) {
			logger.warn("No resources to read");
			noInput = true;
			return;
		}

		Arrays.sort(resources, comparator);

		index.open(executionContext);

		delegate.setResource(resources[index.currentResource]);

		delegate.open(new ExecutionContext());

		try {
			for (int i = 0; i < index.currentItem; i++) {
				delegate.read();
			}
		}
		catch (Exception e) {
			throw new ItemStreamException("Could not restore position on restart", e);
		}
	}

	/**
	 * Store the current resource index and position in the resource.
	 */
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (saveState) {
			index.update(executionContext);
		}
	}

	/**
	 * @param delegate reads items from single {@link Resource}.
	 */
	public void setDelegate(ResourceAwareItemReaderItemStream<? extends T> delegate) {
		this.delegate = delegate;
	}

	/**
	 * Set the boolean indicating whether or not state should be saved in the
	 * provided {@link ExecutionContext} during the {@link ItemStream} call to
	 * update.
	 * 
	 * @param saveState
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * @param comparator used to order the injected resources, by default
	 * compares {@link Resource#getFilename()} values.
	 */
	public void setComparator(Comparator<Resource> comparator) {
		this.comparator = comparator;
	}

	/**
	 * @param resources input resources
	 */
	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	/**
	 * Facilitates keeping track of the position within multi-resource input.
	 */
	private class MultiResourceIndex {

		private static final String RESOURCE_KEY = "resourceIndex";

		private static final String ITEM_KEY = "itemIndex";

		private int currentResource = 0;

		private int markedResource = 0;

		private long currentItem = 0;

		private long markedItem = 0;

		public void incrementItemCount() {
			currentItem++;
		}

		public void incrementResourceCount() {
			currentResource++;
			currentItem = 0;
		}

		public void mark() {
			markedResource = currentResource;
			markedItem = currentItem;
		}

		public void reset() {
			currentResource = markedResource;
			currentItem = markedItem;
		}

		public void open(ExecutionContext ctx) {
			if (ctx.containsKey(executionContextUserSupport.getKey(RESOURCE_KEY))) {
				currentResource = Long.valueOf(ctx.getLong(executionContextUserSupport.getKey(RESOURCE_KEY))).intValue();
			}

			if (ctx.containsKey(executionContextUserSupport.getKey(ITEM_KEY))) {
				currentItem = ctx.getLong(executionContextUserSupport.getKey(ITEM_KEY));
			}
		}

		public void update(ExecutionContext ctx) {
			ctx.putLong(executionContextUserSupport.getKey(RESOURCE_KEY), index.currentResource);
			ctx.putLong(executionContextUserSupport.getKey(ITEM_KEY), index.currentItem);
		}
	}

}

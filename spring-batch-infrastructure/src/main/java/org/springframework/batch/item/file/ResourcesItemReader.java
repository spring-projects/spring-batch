package org.springframework.batch.item.file;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;

/**
 * {@link ItemReader} which produces {@link Resource} instances from an array.
 * This can be used conveniently with a configuration entry that injects a
 * pattern (e.g. <code>mydir/*.txt</code>, which can then be converted by Spring
 * to an array of Resources by the ApplicationContext.
 * 
 * <br/>
 * <br/>
 * 
 * Thread safe between calls to {@link #open(ExecutionContext)}. The
 * {@link ExecutionContext} is not accurate in a multi-threaded environment, so
 * do not rely on that data for restart (i.e. always open with a fresh context).
 * 
 * @author Dave Syer
 * 
 * @see ResourceArrayPropertyEditor
 * 
 * @since 2.1
 */
public class ResourcesItemReader extends ExecutionContextUserSupport implements ItemStreamReader<Resource> {

	private Resource[] resources = new Resource[0];

	private AtomicInteger counter = new AtomicInteger(0);

	{
		/*
		 * Initialize the name for the key in the execution context.
		 */
		setName(getClass().getName());
	}

	/**
	 * The resources to serve up as items. Hint: use a pattern to configure.
	 * 
	 * @param resources the resources
	 */
	public void setResources(Resource[] resources) {
		this.resources = Arrays.asList(resources).toArray(new Resource[resources.length]);
	}

	/**
	 * Increments a counter and returns the next {@link Resource} instance from
	 * the input, or null if none remain.
	 */
	public synchronized Resource read() throws Exception {
		int index = counter.incrementAndGet() - 1;
		if (index >= resources.length) {
			return null;
		}
		return resources[index];
	}

	public void close() throws ItemStreamException {
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {
		counter.set(executionContext.getInt(getKey("COUNT"), 0));
	}

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putInt(getKey("COUNT"), counter.get());
	}

}

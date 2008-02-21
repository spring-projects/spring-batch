package org.springframework.batch.io.file.transform;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.batch.item.writer.ItemTransformer;

/**
 * An implementation of {@link ItemTransformer} that treats its argument
 * specially if it is an array or collection. In this case it loops though,
 * calling itself on each member in turn, until it encounters a non collection.
 * At this point, if the item is a String, that is used, or else it is passed to
 * the delegate {@link ItemTransformer}. The transformed single item Strings
 * are all concatenated with line separators.
 * 
 * @author Dave Syer
 * 
 */
public class RecursiveCollectionItemTransformer implements ItemTransformer {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private ItemTransformer delegate = new ItemTransformer() {
		public Object transform(Object item) throws Exception {
			return item;
		}
	};

	/**
	 * Public setter for the {@link ItemTransformer} to use on single items,
	 * that are not Strings. This can be used to strategise the conversion of
	 * collection and array elements to a String, e.g. via a subclass of
	 * {@link LineAggregatorItemTransformer}.<br/>
	 * 
	 * N.B. if the delegate returns an array or collection, it will not be
	 * treated the same way as the original item passed in for transformation.
	 * Rather, in this case, it will simply be converted immediately to a String
	 * by calling its toString().
	 * 
	 * @param delegate the delegate to set. Defaults to a pass through.
	 */
	public void setDelegate(ItemTransformer delegate) {
		this.delegate = delegate;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.item.writer.ItemTransformer#transform(java.lang.Object)
	 */
	public Object transform(Object input) throws Exception {
		TransformHolder holder = new TransformHolder();
		transformRecursively(input, holder);
		String result = holder.builder.toString();
		return result.substring(0, result.lastIndexOf(LINE_SEPARATOR));
	}

	public String stringify(Object item) throws Exception {
		return "" + delegate.transform(item);
	}

	/**
	 * Convert the date to a format that can be output and then write it out.
	 * @param data
	 * @param converted
	 * @throws Exception
	 */
	private void transformRecursively(Object data, TransformHolder converted) throws Exception {

		if (data instanceof Collection) {
			for (Iterator iterator = ((Collection) data).iterator(); iterator.hasNext();) {
				Object value = (Object) iterator.next();
				// (recursive)
				transformRecursively(value, new TransformHolder(converted.builder));
			}
			return;
		}
		if (data.getClass().isArray()) {
			Object[] array = (Object[]) data;
			for (int i = 0; i < array.length; i++) {
				Object value = array[i];
				// (recursive)
				transformRecursively(value, new TransformHolder(converted.builder));
			}
			return;
		}
		if (data instanceof String) {
			// This is where the output stream is actually written to
			converted.builder.append(data + LINE_SEPARATOR);
		}
		else {
			// (recursive)
			transformRecursively(stringify(data), converted);
			return;
		}
	}

	private static class TransformHolder {

		StringBuilder builder = new StringBuilder();

		TransformHolder() {
		}

		TransformHolder(StringBuilder builder) {
			this.builder = builder;
		}
	}
}
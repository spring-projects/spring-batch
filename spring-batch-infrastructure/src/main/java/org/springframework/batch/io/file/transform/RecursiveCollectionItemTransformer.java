package org.springframework.batch.io.file.transform;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.batch.item.writer.ItemTransformer;

/**
 * An implementation of {@link ItemTransformer} that just calls toString() on
 * its argument, unless it it an array or collection, in which case it loops
 * though, calling itself on each member in turn, concatenating the result with
 * line separators.
 * 
 * @author Dave Syer
 * 
 */
public class RecursiveCollectionItemTransformer implements ItemTransformer {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.writer.ItemTransformer#transform(java.lang.Object)
	 */
	public Object transform(Object input) {
		TransformHolder holder = new TransformHolder();
		transformRecursively(input, holder);
		String result = holder.builder.toString();
		return result.substring(0, result.lastIndexOf(LINE_SEPARATOR));
	}

	public String stringify(Object input) {
		return "" + input;
	}

	/**
	 * Convert the date to a format that can be output and then write it out.
	 * @param data
	 * @param converted
	 * @throws Exception
	 */
	private void transformRecursively(Object data, TransformHolder converted) {

		if (data instanceof Collection) {
			converted.value = false;
			for (Iterator iterator = ((Collection) data).iterator(); iterator.hasNext();) {
				Object value = (Object) iterator.next();
				// (recursive)
				transformRecursively(value, new TransformHolder(converted.builder));
			}
			return;
		}
		if (data.getClass().isArray()) {
			converted.value = false;
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
		else if (!converted.value) {
			// (recursive)
			converted.value = true;
			transformRecursively(stringify(data), converted);
			return;
		}
		else {
			// Should not happen...
			throw new IllegalStateException(
					"Infinite loop detected - converter did not convert to String or collection/array of objects convertible to String.");
		}
	}

	private static class TransformHolder {
		boolean value;

		StringBuilder builder = new StringBuilder();

		TransformHolder() {
		}

		TransformHolder(StringBuilder builder) {
			this.builder = builder;
		}
	}
}
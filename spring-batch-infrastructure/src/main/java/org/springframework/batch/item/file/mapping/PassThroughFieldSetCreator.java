package org.springframework.batch.item.file.mapping;

public class PassThroughFieldSetCreator<T> implements FieldSetCreator<T> {

	/**
	 * If the input is a {@link FieldSet} pass it to the caller. Otherwise
	 * convert to a String with toString() and convert it to a single field
	 * {@link FieldSet}.
	 * 
	 * @see org.springframework.batch.item.file.mapping.FieldSetCreator#mapItem(java.lang.Object)
	 */
	public FieldSet mapItem(T item) {
		if (item instanceof FieldSet) {
			return (FieldSet) item;
		}
		
		String stringItem = item.toString();
		
		return new DefaultFieldSet(new String[] { stringItem });
	}

}

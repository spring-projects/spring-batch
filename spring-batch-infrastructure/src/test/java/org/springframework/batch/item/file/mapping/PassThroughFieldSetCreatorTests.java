package org.springframework.batch.item.file.mapping;

import junit.framework.TestCase;

public class PassThroughFieldSetCreatorTests extends TestCase {
	
	private PassThroughFieldSetCreator<Object> mapper = new PassThroughFieldSetCreator<Object>();

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.file.mapping.PassThroughFieldSetCreator#mapItem(Object)}.
	 */
	public void testUnmapItemAsFieldSet() {
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" });
		assertEquals(fieldSet, mapper.mapItem(fieldSet));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.file.mapping.PassThroughFieldSetCreator#mapItem(java.lang.Object)}.
	 */
	public void testUnmapItemAsString() {
		assertEquals(new DefaultFieldSet(new String[] { "foo" }), mapper.mapItem("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.file.mapping.PassThroughFieldSetCreator#mapItem(java.lang.Object)}.
	 */
	public void testUnmapItemAsNonString() {
		Object object = new Object();
		assertEquals(new DefaultFieldSet(new String[] { "" + object }), mapper.mapItem(object));
	}
}

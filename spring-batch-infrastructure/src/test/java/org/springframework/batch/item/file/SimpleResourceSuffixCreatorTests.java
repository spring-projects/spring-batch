package org.springframework.batch.item.file;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for {@link SimpleResourceSuffixCreator}.
 */
public class SimpleResourceSuffixCreatorTests {

	private SimpleResourceSuffixCreator tested = new SimpleResourceSuffixCreator();

	@Test
	public void testGetSuffix() {
		assertEquals(".0", tested.getSuffix(0));
		assertEquals(".1", tested.getSuffix(1));
		assertEquals(".3463457", tested.getSuffix(3463457));
	}
}

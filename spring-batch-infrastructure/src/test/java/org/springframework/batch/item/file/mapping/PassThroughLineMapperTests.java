package org.springframework.batch.item.file.mapping;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for {@link PassThroughLineMapper}.
 */
public class PassThroughLineMapperTests {

	private PassThroughLineMapper tested = new PassThroughLineMapper();
	
	@Test
	public void testMapLine() throws Exception {
		assertSame("line", tested.mapLine("line", 1));
	}
}

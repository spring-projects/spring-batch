package org.springframework.batch.item.file.mapping;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.batch.item.file.transform.LineTokenizer;

/**
 * Tests for {@link DefaultLineMapper}.
 */
public class DefaultLineMapperTests {

	private DefaultLineMapper<String> tested = new DefaultLineMapper<String>();
	
	@Test
	public void testMapping() throws Exception {
		final String line = "TEST";
		final FieldSet fs = new DefaultFieldSet(new String[]{"token1", "token2"});
		final String item = "ITEM";
		
		LineTokenizer tokenizer = createStrictMock(LineTokenizer.class);
		expect(tokenizer.process(line)).andReturn(fs);
		replay(tokenizer);
		
		@SuppressWarnings("unchecked")
		FieldSetMapper<String> fsMapper = createStrictMock(FieldSetMapper.class);
		expect(fsMapper.process(fs)).andReturn(item);
		replay(fsMapper);
		
		tested.setLineTokenizer(tokenizer);
		tested.setFieldSetMapper(fsMapper);
		
		assertSame(item, tested.mapLine(line, 1));
		verify(tokenizer);
		verify(fsMapper);
		
	}
}

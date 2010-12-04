package org.springframework.batch.item.file.mapping;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.LineTokenizer;

/**
 * Tests for {@link DefaultLineMapper}.
 */
public class DefaultLineMapperTests {

	private DefaultLineMapper<String> tested = new DefaultLineMapper<String>();

	@Test(expected=IllegalArgumentException.class)
	public void testMandatoryTokenizer() throws Exception {
		tested.afterPropertiesSet();
		tested.mapLine("foo", 1);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testMandatoryMapper() throws Exception {
		tested.setLineTokenizer(new DelimitedLineTokenizer());
		tested.afterPropertiesSet();
		tested.mapLine("foo", 1);
	}
	
	@Test
	public void testMapping() throws Exception {
		final String line = "TEST";
		final FieldSet fs = new DefaultFieldSet(new String[]{"token1", "token2"});
		final String item = "ITEM";
		
		LineTokenizer tokenizer = createStrictMock(LineTokenizer.class);
		expect(tokenizer.tokenize(line)).andReturn(fs);
		replay(tokenizer);
		
		@SuppressWarnings("unchecked")
		FieldSetMapper<String> fsMapper = createStrictMock(FieldSetMapper.class);
		expect(fsMapper.mapFieldSet(fs)).andReturn(item);
		replay(fsMapper);
		
		tested.setLineTokenizer(tokenizer);
		tested.setFieldSetMapper(fsMapper);
		
		assertSame(item, tested.mapLine(line, 1));
		verify(tokenizer);
		verify(fsMapper);
		
	}
	
}

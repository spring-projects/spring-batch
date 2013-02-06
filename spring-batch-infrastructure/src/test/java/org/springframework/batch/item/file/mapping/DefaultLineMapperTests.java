package org.springframework.batch.item.file.mapping;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
		
		LineTokenizer tokenizer = mock(LineTokenizer.class);
		when(tokenizer.tokenize(line)).thenReturn(fs);
		
		@SuppressWarnings("unchecked")
		FieldSetMapper<String> fsMapper = mock(FieldSetMapper.class);
		when(fsMapper.mapFieldSet(fs)).thenReturn(item);
		
		tested.setLineTokenizer(tokenizer);
		tested.setFieldSetMapper(fsMapper);
		
		assertSame(item, tested.mapLine(line, 1));
		
	}
	
}

package org.springframework.batch.item.file.mapping;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.batch.item.file.FlatFileParseException;
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
	
	@Test
	public void testTokenizerException() throws Exception {
		final String line = "TEST";
		
		LineTokenizer tokenizer = createStrictMock(LineTokenizer.class);
		expect(tokenizer.tokenize(line)).andThrow(new RuntimeException());
		replay(tokenizer);
		
		@SuppressWarnings("unchecked")
		FieldSetMapper<String> fsMapper = createStrictMock(FieldSetMapper.class);
		
		tested.setLineTokenizer(tokenizer);
		tested.setFieldSetMapper(fsMapper);
		
		try{
			tested.mapLine(line, 1);
		}
		catch(FlatFileParseException ex){
			assertEquals(ex.getLineNumber(), 1);
			assertEquals(ex.getInput(), line);
		}
	}
	
	@Test
	public void testMapperException() throws Exception {
		final String line = "TEST";
		final FieldSet fs = new DefaultFieldSet(new String[]{"token1", "token2"});
		
		LineTokenizer tokenizer = createStrictMock(LineTokenizer.class);
		expect(tokenizer.tokenize(line)).andReturn(fs);
		replay(tokenizer);
		
		@SuppressWarnings("unchecked")
		FieldSetMapper<String> fsMapper = createStrictMock(FieldSetMapper.class);
		expect(fsMapper.mapFieldSet(fs)).andThrow(new RuntimeException());
		replay(fsMapper);
		
		tested.setLineTokenizer(tokenizer);
		tested.setFieldSetMapper(fsMapper);
		
		try{
			tested.mapLine(line, 1);
		}
		catch(FlatFileParseException ex){
			assertEquals(ex.getLineNumber(), 1);
			assertEquals(ex.getInput(), line);
		}
		
	}
}

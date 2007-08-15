package org.springframework.batch.sample.module;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.exception.TransactionInvalidException;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;

public class SkipSampleItemProviderTests extends TestCase {

	private MockControl inputControl;
	private FieldSetInputSource input;
	private MockControl mapperControl;
	private FieldSetMapper mapper;
	private SkipSampleItemProvider provider;
	
	private static final int ITER_COUNT = 7;
	
	public void setUp() {
		
		inputControl = MockControl.createControl(FieldSetInputSource.class);
		input = (FieldSetInputSource)inputControl.getMock();
		
		mapperControl = MockControl.createControl(FieldSetMapper.class);
		mapper = (FieldSetMapper)mapperControl.getMock();
		
		provider = new SkipSampleItemProvider();
		provider.setInputSource(input);
		provider.setFieldSetMapper(mapper);
	}
	
	public void testNext() {
		
		//set-up mock input
		input.readFieldSet();
		inputControl.setReturnValue(null,ITER_COUNT);
		inputControl.replay();
		
		//set-up mock mapper
		mapper.mapLine(null);
		mapperControl.setReturnValue("line",ITER_COUNT);
		mapperControl.replay();
		
		//set exception iteration count
		provider.setThrowExceptionOnRecordNumber(ITER_COUNT + 1);
		
		//call next() method multiple times and verify whether exception is thrown when expected
		for (int i = 0; i <= ITER_COUNT; i++) {
			try {
				assertEquals("line", provider.next());
				assertTrue(i < ITER_COUNT);
			} catch (TransactionInvalidException tie) {
				assertEquals(ITER_COUNT,i);
			}
		}
		
		//verify method calls
		inputControl.verify();
		mapperControl.verify();
	}

}

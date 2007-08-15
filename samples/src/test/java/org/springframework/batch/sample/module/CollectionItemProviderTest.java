package org.springframework.batch.sample.module;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;

public class CollectionItemProviderTest extends TestCase {

	private MockControl inputControl;
	private FieldSetInputSource input;
	private MockControl mapperControl;
	private FieldSetMapper mapper;
	private CollectionItemProvider provider;
	
	public void setUp() {
		
		//create mock for input
		inputControl = MockControl.createControl(FieldSetInputSource.class);
		input = (FieldSetInputSource) inputControl.getMock();
		
		//create mock for mapper
		mapperControl = MockControl.createControl(FieldSetMapper.class);
		mapper = (FieldSetMapper) mapperControl.getMock();
		mapperControl.setDefaultMatcher(MockControl.ALWAYS_MATCHER);
		mapper.mapLine(null);
		mapperControl.setDefaultReturnValue("line");
		mapperControl.replay();
		
		//create provider
		provider = new CollectionItemProvider();
		provider.setInputSource(input);
		provider.setFieldSetMapper(mapper);
	}
		
	public void testNext() {
		
		//set-up mock input
		input.readFieldSet();
		inputControl.setReturnValue(new FieldSet(new String[] {"BEGIN"}));
		input.readFieldSet();
		inputControl.setReturnValue(new FieldSet(new String[] {"line"}),3);
		input.readFieldSet();
		inputControl.setReturnValue(new FieldSet(new String[] {"END"}));
		input.readFieldSet();
		inputControl.setReturnValue(null);
		inputControl.replay();
				
		//read object
		Object result = provider.next();
		
		//it should be collection od 3 strings "line"
		assertTrue(result instanceof Collection);
		Collection lines = (Collection)result;
		assertEquals(3, lines.size());
		
		for (Iterator i = lines.iterator(); i.hasNext();) {
			assertEquals("line", i.next());
		}

		//read object again - it should return null
		assertNull(provider.next());
		
		//verify method calls
		inputControl.verify();
	}
}

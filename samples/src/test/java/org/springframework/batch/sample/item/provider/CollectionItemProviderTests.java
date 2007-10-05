package org.springframework.batch.sample.item.provider;

import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.FieldSetInputSource;
import org.springframework.batch.io.file.FieldSetMapper;
import org.springframework.batch.sample.item.provider.CollectionItemProvider;

public class CollectionItemProviderTests extends TestCase {

	private MockControl inputControl;
	private FieldSetInputSource input;
	private FieldSetMapper mapper;
	private CollectionItemProvider provider;
	
	public void setUp() {
		
		//create mock for input
		inputControl = MockControl.createControl(FieldSetInputSource.class);
		input = (FieldSetInputSource) inputControl.getMock();
		
		//create mock for mapper
		mapper = new FieldSetMapper() {
			public Object mapLine(FieldSet fs) { return fs.readString(0); }
		};
		
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
		
		//it should be collection of 3 strings "line"
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

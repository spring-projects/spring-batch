package org.springframework.batch.item.file;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

public class ResourcesItemReaderTests {

	private ResourcesItemReader reader = new ResourcesItemReader();

	@Before
	public void init() {
		reader.setResources(new Resource[] { new ByteArrayResource("foo".getBytes()),
				new ByteArrayResource("bar".getBytes()) });
	}

	@Test
	public void testRead() throws Exception {
		assertNotNull(reader.read());
	}

	@Test
	public void testExhaustRead() throws Exception {
		for (int i = 0; i < 2; i++) {
			assertNotNull(reader.read());
		}
		assertNull(reader.read());
	}

	@Test
	public void testReadAfterOpen() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.putInt(reader.getKey("COUNT"), 1);
		reader.open(executionContext);
		assertNotNull(reader.read());
		assertNull(reader.read());
	}

	@Test
	public void testReadAndUpdate() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();
		assertNotNull(reader.read());

		reader.update(executionContext);
		assertEquals(1, executionContext.getInt(reader.getKey("COUNT")));
	}

}

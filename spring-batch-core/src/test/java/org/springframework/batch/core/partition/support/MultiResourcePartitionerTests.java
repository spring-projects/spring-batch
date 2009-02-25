package org.springframework.batch.core.partition.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceArrayPropertyEditor;

public class MultiResourcePartitionerTests {

	private MultiResourcePartitioner partitioner = new MultiResourcePartitioner();

	@Before
	public void setUp() {
		ResourceArrayPropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath:log4j*");
		partitioner.setResources((Resource[]) editor.getValue());
	}

	@Test(expected = IllegalStateException.class)
	public void testMissingResource() {
		partitioner.setResources(new Resource[] { new FileSystemResource("does-not-exist") });
		partitioner.partition(0);
	}

	@Test
	public void testPartitionSizeAndKey() {
		Map<String, ExecutionContext> partition = partitioner.partition(0);
		assertEquals(1, partition.size());
		assertTrue(partition.containsKey("partition0"));
	}

	@Test
	public void testReadFile() throws Exception {
		Map<String, ExecutionContext> partition = partitioner.partition(0);
		String url = partition.get("partition0").getString("fileName");
		assertTrue(new UrlResource(url).exists());
	}

	@Test
	public void testSetKeyName() {
		partitioner.setKeyName("foo");
		Map<String, ExecutionContext> partition = partitioner.partition(0);
		assertTrue(partition.get("partition0").containsKey("foo"));
	}

}

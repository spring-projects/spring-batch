/*
 * Copyright 2008-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.file;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link MultiResourceItemReader}.
 */
class MultiResourceItemReaderIntegrationTests {

	private final MultiResourceItemReader<String> tested = new MultiResourceItemReader<>();

	private final FlatFileItemReader<String> itemReader = new FlatFileItemReader<>();

	private final ExecutionContext ctx = new ExecutionContext();

	// test input spans several resources
	private final Resource r1 = new ByteArrayResource("1\n2\n3\n".getBytes());

	private final Resource r2 = new ByteArrayResource("4\n5\n".getBytes());

	private final Resource r3 = new ByteArrayResource("".getBytes());

	private final Resource r4 = new ByteArrayResource("6\n".getBytes());

	private final Resource r5 = new ByteArrayResource("7\n8\n".getBytes());

	/**
	 * Setup the tested reader to read from the test resources.
	 */
	@BeforeEach
	void setUp() {

		itemReader.setLineMapper(new PassThroughLineMapper());

		tested.setDelegate(itemReader);
		tested.setComparator(new Comparator<Resource>() {
			@Override
			public int compare(Resource o1, Resource o2) {
				return 0; // do not change ordering
			}
		});
		tested.setResources(new Resource[] { r1, r2, r3, r4, r5 });
	}

	/**
	 * Read input from start to end.
	 */
	@Test
	void testRead() throws Exception {

		tested.open(ctx);

		assertEquals("1", tested.read());
		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());
		assertEquals("5", tested.read());
		assertEquals("6", tested.read());
		assertEquals("7", tested.read());
		assertEquals("8", tested.read());
		assertNull(tested.read());

		tested.close();
	}

	@Test
	void testRestartWhenStateNotSaved() throws Exception {

		tested.setSaveState(false);

		tested.open(ctx);

		assertEquals("1", tested.read());

		tested.update(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("1", tested.read());
	}

	/**
	 *
	 * Read items with a couple of rollbacks, requiring to jump back to items from
	 * previous resources.
	 */
	@Test
	void testRestartAcrossResourceBoundary() throws Exception {

		tested.open(ctx);

		assertEquals("1", tested.read());

		tested.update(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());
		assertEquals("5", tested.read());

		tested.update(ctx);

		assertEquals("6", tested.read());
		assertEquals("7", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("6", tested.read());
		assertEquals("7", tested.read());

		assertEquals("8", tested.read());
		assertNull(tested.read());

		tested.close();
	}

	/**
	 * Restore from saved state.
	 */
	@Test
	void testRestart() throws Exception {

		tested.open(ctx);

		assertEquals("1", tested.read());
		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertEquals("4", tested.read());

		tested.update(ctx);

		assertEquals("5", tested.read());
		assertEquals("6", tested.read());

		tested.close();

		tested.open(ctx);

		assertEquals("5", tested.read());
		assertEquals("6", tested.read());
		assertEquals("7", tested.read());
		assertEquals("8", tested.read());
		assertNull(tested.read());
	}

	/**
	 * Resources are ordered according to injected comparator.
	 */
	@Test
	void testResourceOrderingWithCustomComparator() {

		Resource r1 = new ByteArrayResource("".getBytes(), "b");
		Resource r2 = new ByteArrayResource("".getBytes(), "a");
		Resource r3 = new ByteArrayResource("".getBytes(), "c");

		Resource[] resources = new Resource[] { r1, r2, r3 };

		Comparator<Resource> comp = new Comparator<Resource>() {

			/**
			 * Reversed ordering by filename.
			 */
			@Override
			public int compare(Resource o1, Resource o2) {
				Resource r1 = o1;
				Resource r2 = o2;
				return -r1.getDescription().compareTo(r2.getDescription());
			}

		};

		tested.setComparator(comp);
		tested.setResources(resources);
		tested.open(ctx);

		resources = (Resource[]) ReflectionTestUtils.getField(tested, "resources");

		assertSame(r3, resources[0]);
		assertSame(r1, resources[1]);
		assertSame(r2, resources[2]);
	}

	/**
	 * Empty resource list is OK.
	 */
	@Test
	void testNoResourcesFound() throws Exception {
		tested.setResources(new Resource[] {});
		tested.open(new ExecutionContext());

		assertNull(tested.read());

		tested.close();
	}

	/**
	 * Missing resource is OK.
	 */
	@Test
	void testNonExistentResources() throws Exception {
		tested.setResources(new Resource[] { new FileSystemResource("no/such/file.txt") });
		itemReader.setStrict(false);
		tested.open(new ExecutionContext());

		assertNull(tested.read());

		tested.close();
	}

	/**
	 * Test {@link org.springframework.batch.item.ItemStream} lifecycle symmetry
	 */
	@Test
	void testNonExistentResourcesItemStreamLifecycle() throws Exception {
		ItemStreamReaderImpl delegate = new ItemStreamReaderImpl();
		tested.setDelegate(delegate);
		tested.setResources(new Resource[] {});
		itemReader.setStrict(false);
		tested.open(new ExecutionContext());

		assertNull(tested.read());
		assertFalse(delegate.openCalled);
		assertFalse(delegate.closeCalled);
		assertFalse(delegate.updateCalled);

		tested.close();
	}

	/**
	 * Directory resource behaves as if it was empty.
	 */
	@Test
	void testDirectoryResources() throws Exception {
		FileSystemResource resource = new FileSystemResource("target/data");
		resource.getFile().mkdirs();
		assertTrue(resource.getFile().isDirectory());
		tested.setResources(new Resource[] { resource });
		itemReader.setStrict(false);
		tested.open(new ExecutionContext());

		assertNull(tested.read());

		tested.close();
	}

	@Test
	void testMiddleResourceThrowsException() throws Exception {

		Resource badResource = new AbstractResource() {

			@Override
			public InputStream getInputStream() throws IOException {
				throw new RuntimeException();
			}

			@Override
			public String getDescription() {
				return null;
			}
		};

		tested.setResources(new Resource[] { r1, badResource, r3, r4, r5 });

		tested.open(ctx);

		assertEquals("1", tested.read());
		assertEquals("2", tested.read());
		assertEquals("3", tested.read());
		assertThrows(ItemStreamException.class, tested::read);
	}

	@Test
	void testFirstResourceThrowsExceptionOnRead() {

		Resource badResource = new AbstractResource() {

			@Override
			public InputStream getInputStream() throws IOException {
				throw new RuntimeException();
			}

			@Override
			public String getDescription() {
				return null;
			}
		};

		tested.setResources(new Resource[] { badResource, r2, r3, r4, r5 });

		tested.open(ctx);

		assertThrows(ItemStreamException.class, tested::read);
	}

	@Test
	void testBadIOInput() throws Exception {

		Resource badResource = new AbstractResource() {

			@Override
			public boolean exists() {
				// Looks good ...
				return true;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				// ... but fails during read
				throw new RuntimeException();
			}

			@Override
			public String getDescription() {
				return null;
			}
		};

		tested.setResources(new Resource[] { badResource, r2, r3, r4, r5 });

		tested.open(ctx);

		assertThrows(ItemStreamException.class, tested::read);

		// Now check the next read gets the next resource
		assertEquals("4", tested.read());

	}

	/**
	 * No resources to read should result in error in strict mode.
	 */
	@Test
	void testStrictModeEnabled() {
		tested.setResources(new Resource[] {});
		tested.setStrict(true);

		assertThrows(IllegalStateException.class, () -> tested.open(ctx));
	}

	/**
	 * No resources to read is OK when strict=false.
	 */
	@Test
	void testStrictModeDisabled() {
		tested.setResources(new Resource[] {});
		tested.setStrict(false);

		tested.open(ctx);
		assertTrue(true, "empty input doesn't cause an error");
	}

	/**
	 * E.g. when using the reader in the processing phase reading might not have been
	 * attempted at all before the job crashed (BATCH-1798).
	 */
	@Test
	void testRestartAfterFailureWithoutRead() throws Exception {

		// save reader state without calling read
		tested.open(ctx);
		tested.update(ctx);
		tested.close();

		// restart should work OK
		tested.open(ctx);
		assertEquals("1", tested.read());
	}

	private static class ItemStreamReaderImpl implements ResourceAwareItemReaderItemStream<String> {

		private boolean openCalled = false;

		private boolean updateCalled = false;

		private boolean closeCalled = false;

		@Nullable
		@Override
		public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
			return null;
		}

		@Override
		public void open(ExecutionContext executionContext) throws ItemStreamException {
			openCalled = true;
		}

		@Override
		public void update(ExecutionContext executionContext) throws ItemStreamException {
			updateCalled = true;
		}

		@Override
		public void close() throws ItemStreamException {
			closeCalled = true;
		}

		@Override
		public void setResource(Resource resource) {
		}

	}

}

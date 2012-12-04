package org.springframework.batch.item.data;

import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.repository.CrudRepository;

@SuppressWarnings("rawtypes")
public class RepositoryItemWriterTest {

	private CrudRepository repository;

	private RepositoryItemWriter writer;

	@Before
	public void setUp() throws Exception {
		repository = createStrictMock(CrudRepository.class);
		writer = new RepositoryItemWriter();
		writer.setRepository(repository);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer.afterPropertiesSet();

		writer.setRepository(null);

		try {
			writer.afterPropertiesSet();
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testWriteNoItems() throws Exception {
		replay(repository);
		writer.write(null);

		writer.write(new ArrayList());

		try {
			writer.write(new ArrayList(){{add(new Object());}});
		} catch (AssertionError e) {
			assertTrue(e.getMessage().contains("CrudRepository.save("));
		}
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testWriteItems() throws Exception {
		List<Object> items = new ArrayList<Object>() {{
			add(new Object());
		}};

		expect(repository.save(items)).andReturn(null);

		replay(repository);

		writer.write(items);

		verify(repository);
	}
}

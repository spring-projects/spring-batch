package org.springframework.batch.item.data;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.repository.CrudRepository;

@SuppressWarnings("rawtypes")
public class RepositoryItemWriterTests {

	@Mock
	private CrudRepository repository;

	private RepositoryItemWriter writer;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		writer = new RepositoryItemWriter();
		writer.setMethodName("save");
		writer.setRepository(repository);
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer.afterPropertiesSet();

		writer.setRepository(null);

		try {
			writer.afterPropertiesSet();
			fail();
		} catch (IllegalStateException e) {
		}
	}

	@Test
	public void testWriteNoItems() throws Exception {
		writer.write(null);

		writer.write(new ArrayList());

		verifyZeroInteractions(repository);
	}

	@Test
	@SuppressWarnings({"serial", "unchecked"})
	public void testWriteItems() throws Exception {
		List<Object> items = new ArrayList<Object>() {{
			add("foo");
		}};

		writer.write(items);

		verify(repository).save("foo");
	}
}

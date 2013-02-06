package org.springframework.batch.item.database;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;

/**
 * Tests for {@link HibernateCursorItemReader} using standard hibernate {@link Session}.
 *
 * @author Robert Kasanicky
 * @author Will Schipp
 */
public class HibernateCursorItemReaderStatefulIntegrationTests extends AbstractHibernateCursorItemReaderIntegrationTests {

	@Override
	protected boolean isUseStatelessSession() {
		return false;
	}

	//Ensure close is called on the stateful session correctly.
	@Test
	public void testStatefulClose(){

		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = mock(Session.class);
		Query scrollableResults = mock(Query.class);
		HibernateCursorItemReader<Foo> itemReader = new HibernateCursorItemReader<Foo>();
		itemReader.setSessionFactory(sessionFactory);
		itemReader.setQueryString("testQuery");
		itemReader.setUseStatelessSession(false);

		when(sessionFactory.openSession()).thenReturn(session);
		when(session.createQuery("testQuery")).thenReturn(scrollableResults);
		when(scrollableResults.setFetchSize(0)).thenReturn(scrollableResults);
		when(session.close()).thenReturn(null);

		itemReader.open(new ExecutionContext());
		itemReader.close();
	}

}

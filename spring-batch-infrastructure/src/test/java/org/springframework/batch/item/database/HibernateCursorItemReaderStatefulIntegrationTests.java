package org.springframework.batch.item.database;

import org.easymock.MockControl;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.sample.Foo;

/**
 * Tests for {@link HibernateCursorItemReader} using standard hibernate {@link Session}.
 * 
 * @author Robert Kasanicky
 */
public class HibernateCursorItemReaderStatefulIntegrationTests extends HibernateCursorItemReaderIntegrationTests {

	protected boolean isUseStatelessSession() {
		return false;
	}
	
	//Ensure close is called on the stateful session correctly.
	public void testStatfulClose(){
		
		MockControl<SessionFactory> sessionFactoryControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sessionFactory = sessionFactoryControl.getMock();
		MockControl<Session> sessionControl = MockControl.createControl(Session.class);
		Session session = sessionControl.getMock();
		MockControl<Query> resultsControl = MockControl.createNiceControl(Query.class);
		Query scrollableResults = resultsControl.getMock();
		HibernateCursorItemReader<Foo> itemReader = new HibernateCursorItemReader<Foo>();
		itemReader.setSessionFactory(sessionFactory);
		itemReader.setQueryString("testQuery");
		itemReader.setUseStatelessSession(false);
		
		sessionFactory.openSession();
		sessionFactoryControl.setReturnValue(session);
		session.createQuery("testQuery");
		sessionControl.setReturnValue(scrollableResults);
		scrollableResults.setFetchSize(0);
		resultsControl.setReturnValue(scrollableResults);
		session.close();
		sessionControl.setReturnValue(null);
		sessionFactoryControl.replay();
		sessionControl.replay();
		resultsControl.replay();
		itemReader.open(new ExecutionContext());
		itemReader.close(new ExecutionContext());
		sessionFactoryControl.verify();
		sessionControl.verify();
	}
	
}

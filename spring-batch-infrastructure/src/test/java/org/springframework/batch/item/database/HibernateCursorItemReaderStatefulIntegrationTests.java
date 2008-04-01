package org.springframework.batch.item.database;

import org.easymock.MockControl;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.springframework.batch.item.ExecutionContext;

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
		
		MockControl sessionFactoryControl = MockControl.createControl(SessionFactory.class);
		SessionFactory sessionFactory = (SessionFactory) sessionFactoryControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		MockControl resultsControl = MockControl.createControl(Query.class);
		Query scrollableResults = (Query) resultsControl.getMock();
		HibernateCursorItemReader itemReader = new HibernateCursorItemReader();
		itemReader.setSessionFactory(sessionFactory);
		itemReader.setQueryString("testQuery");
		itemReader.setUseStatelessSession(false);
		
		sessionFactory.openSession();
		sessionFactoryControl.setReturnValue(session);
		session.createQuery("testQuery");
		sessionControl.setReturnValue(scrollableResults);
		session.close();
		sessionControl.setReturnValue(null);
		sessionFactoryControl.replay();
		sessionControl.replay();
		itemReader.open(new ExecutionContext());
		itemReader.close(new ExecutionContext());
		sessionFactoryControl.verify();
		sessionControl.verify();
	}
	
}

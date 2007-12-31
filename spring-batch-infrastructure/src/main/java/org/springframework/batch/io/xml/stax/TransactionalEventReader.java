package org.springframework.batch.io.xml.stax;

import javax.xml.stream.XMLEventReader;

/**
 * XMLEventReader with transactional capabilities (ability to rollback to last commit point).
 * 
 * @author Robert Kasanicky
 */
public interface TransactionalEventReader extends XMLEventReader{

	/**
	 * Callback on transaction rollback.
	 */
	public void onRollback();

	/**
	 * Callback on transaction commit.
	 */
	public void onCommit();

}

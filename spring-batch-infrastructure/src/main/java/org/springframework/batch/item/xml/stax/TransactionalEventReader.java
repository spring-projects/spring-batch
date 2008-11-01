package org.springframework.batch.item.xml.stax;

import javax.xml.stream.XMLEventReader;

/**
 * XMLEventReader with transactional capabilities (ability to rollback to last commit point).
 * 
 * @deprecated no longer used, to be removed in 2.0
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

package org.springframework.batch.io.xml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.util.Assert;

/**
 * <p>XmlInputSource2 is {@link InputSource} implementation which processes XML input file 
 * and maps XML elements to objects. It uses {@link org.springframework.oxm.Unmarshaller} 
 * for parsing XML and for OXM mapping. This allows to plug-in various OXM frameworks. 
 * Spring-ws' OXM package provides implementations for Castor, JAXB, JiBX, 
 * XmlBeans and XStream.</p>
 * 
 * <p>{@link org.springframework.oxm.Unmarshaller} always processes whole XML
 * input at once, but we need to process only one record per one module
 * iteration. Therefore XmlInputSource2 uses {@link UnmarshallerAdapter} which cuts XML 
 * into smaller pieces (each piece contains a single record) and calls the unmarshaller 
 * to process only this single piece (record).</p> 
 * 
 * <p><b>XmlInputSource2 configuration</b></p>
 * <p>Mandatory bean properties: 
 * <ul>
 * <li><i>resource</i> - resource pointing to XML input file. Currently, only 
 * {@link FileSystemResource} is supported.</li>
 * <li><i>unmarshaller</i> - any implementation of the {@link Unmarshaller}</li>
 * <li><i>recordElementName</i> - name of the XML element which represents single record</li>
 * </ul>
 * </p>
 * <p>Optional bean properties (if not set, default value is used):
 * <ul>
 * <li><i>encoding</i> - input file encoding (default value is UTF-8)</li>
 * <li><i>useSaxParser</i> - TRUE forces unmarshaller to use SAX parser instead StAX parser. 
 * This can be used for performance optimization. Performance of SAX and StAX can vary
 * depending on OXM framework used and XML record size. Default value is FALSE</li>
 * </ul>
 * </p>
 * 
 * <p>Limitations:
 * <ul>
 * <li>processing of nested records is not supported:</li>
 * <pre>			&lt;folder&gt;
 * 				&lt;folder&gt;&lt;/folder&gt; &lt;- this is not supported
 * 				&lt;file&gt;&lt;/file&gt;
 * 			&lt;/folder&gt; 
 * 			&lt;folder&gt;
 * 				&lt;file&gt;&lt;/file&gt;
 * 			&lt;/folder&gt;</pre>
 * <li>References (idref) in XML are not supported</li>
 * <li>Validation against XSD schema is not implemented</li>
 * </ul>
 * </p>
 * 
 * @see org.springframework.oxm.Unmarshaller
 * @see org.springframework.batch.io.xml.UnmarshallerAdapter
 * @author Peter Zozom
 */
public class XmlInputSource2 implements InputSource, Skippable, Restartable, StatisticsProvider, InitializingBean,
		DisposableBean {

	//logger
	private static final Log log = LogFactory.getLog(XmlInputSource2.class);

	//default encoding
	private static final String DEFAULT_ENCODING = "UTF-8";

	//restart data property name
	private static final String RESTART_DATA_NAME = "staxinputsource.position";

	//read statistics property name
	public static final String READ_STATISTICS_NAME = "Read";

	//file system resource
	private Resource resource;

	//xml unmarshaller (Castor, JAXB, JiBX, XmlBeans or XStream)
	private Unmarshaller unmarshaller;

	//unmarshaller adapter  
	private UnmarshallerAdapter unmarshallerAdapter;

	//file channel associated with Resource 
	private FileChannel fc;

	//encoding to be used while reading from the resource
	private String encoding = DEFAULT_ENCODING;

	//name of the element which represents record
	private String recordElementName;

	//force to use SAX parser. By default StAX parser is used.
	private boolean useSaxParser = false;

	//signalizes that input source has been initialized
	private boolean initialized = false;

	//transaction synchronization object
	private TransactionSynchronization synchronization = new XmlInputSource2TransactionSychronization();

	//current count of processed records
	private long currentRecordCount = 0;

	//file channel position at last commit point
	private long lastCommitPointPosition = 0;

	//count of processed records at last commit point
	private long lastCommitPointRecordCount = 0;

	//list of skipped record numbers
	private List skipRecords = new ArrayList();

	//statistics
	private Properties statistics = new Properties();

	/**
	 * Set the encoding to be used while reading from the Resource
	 * @param encoding the encoding to be used
	 */
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	/**
	 * Set the resource to be read from. Currently is only {@link FileSystemResource} supported.
	 * @param resource the Resource to be read from
	 */
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	/**
	 * Set the {@link Unmarshaller} implementation to be used for Object XML mapping (e.g. JAXB, JiBX, XmlBeans...).
	 * @see org.springframework.oxm.Unmarshaller 
	 * @param unmarshaller the OXM unmarshaller
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	/**
	 * Set the name of the element which represents the record.
	 * @param elementName the element name
	 */
	public void setRecordElementName(String elementName) {
		this.recordElementName = elementName;
	}

	/**
	 * Force to use SAX parser instead of StAX parser. 
	 * @param useSaxParser if true SAX parser will be used, else StAX parser will be used
	 */
	public void setUseSaxParser(boolean useSaxParser) {
		this.useSaxParser = useSaxParser;
	}

	/**
	 * Verifies bean configuration. Resource, Unmarshaller and record element name are mandatory.
	 * @throws Exception
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resource);
		Assert.state(resource.exists(), "Input resource does not exist: [" + resource + "]");
		Assert.notNull(unmarshaller);
		Assert.hasLength(recordElementName);
	}

	/**
	 * @throws Exception
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	/**
	 * Registers the input source for transaction synchronization.
	 */
	private void registerSynchronization() {
		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);
	}

	/**
	 * Opens the input source.
	 * @see org.springframework.batch.item.ResourceLifecycle#open()
	 */
	public void open() {

		registerSynchronization();

		try {
			InputStream is = resource.getInputStream();

			if (is instanceof FileInputStream) {
				fc = ((FileInputStream) is).getChannel();
			}
			else {
				throw new IllegalArgumentException("Only file input stream is supported");
			}

			this.unmarshallerAdapter = new UnmarshallerAdapter(unmarshaller, recordElementName, fc, encoding,
					useSaxParser);

		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new DataAccessResourceFailureException("Unable to get input stream", ioe);
		}

		initialized = true;
	}

	/**
	 * Closes the input source.
	 * @see org.springframework.batch.item.ResourceLifecycle#close()
	 */
	public void close() {

		initialized = false;

		try {
			fc.close();
		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new DataAccessResourceFailureException("Unable to close input Source", ioe);
		}
	}

	/**
	 * Read object from the input. 
	 * @return
	 * @see org.springframework.batch.io.InputSource#read()
	 */
	public Object read() {
		if (!initialized) {
			open();
		}

		Object o;

		do {
			currentRecordCount++;

			try {
				o = unmarshallerAdapter.unmarshal();
			}
			catch (IOException ioe) {
				log.error(ioe);
				throw new DataAccessResourceFailureException("Unable to close XML Input Source", ioe);
			}
		} while (skipRecords.contains(new Long(currentRecordCount)));

		return o;
	}

	/**
	 * Mark current record to be skipped.
	 * @see org.springframework.batch.io.Skippable#skip()
	 */
	public void skip() {
		skipRecords.add(new Long(currentRecordCount));
	}

	/**
	 * @return
	 * @see org.springframework.batch.restart.Restartable#getRestartData()
	 */
	public RestartData getRestartData() {
		Properties restartData = new Properties();

		restartData.setProperty(RESTART_DATA_NAME, String.valueOf(getPosition()));

		return new GenericRestartData(restartData);
	}

	/**
	 * Get current file position.
	 * @return file position
	 */
	private long getPosition() {

		long pos = 0;

		try {
			pos = fc.position();
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to get file position", ioe);
		}

		return pos;
	}

	/**
	 * @param data
	 * @see org.springframework.batch.restart.Restartable#restoreFrom(org.springframework.batch.restart.RestartData)
	 */
	public void restoreFrom(RestartData data) {
		if (data == null || data.getProperties() == null || data.getProperties().getProperty(RESTART_DATA_NAME) == null) {
			return;
		}

		if (!initialized) {
			open();
		}

		long startAtPosition = Long.parseLong(data.getProperties().getProperty(RESTART_DATA_NAME));
		setPosition(startAtPosition);
		this.unmarshallerAdapter = new UnmarshallerAdapter(unmarshaller, recordElementName, fc, encoding, useSaxParser);
	}

	/**
	 * @param startAtPosition
	 */
	private void setPosition(long position) {

		try {
			fc.position(position);
		}
		catch (IOException ioe) {
			throw new DataAccessResourceFailureException("Unable to set file position", ioe);
		}
	}

	/**
	 * @return
	 * @see org.springframework.batch.statistics.StatisticsProvider#getStatistics()
	 */
	public Properties getStatistics() {
		statistics.setProperty(READ_STATISTICS_NAME, String.valueOf(currentRecordCount));
		return statistics;
	}
	
	

	/**
	 * Encapsulates transaction events for the XmlInputSource2.
	 */
	private class XmlInputSource2TransactionSychronization extends TransactionSynchronizationAdapter {

		/**
		 * @param status
		 * @see org.springframework.transaction.support.TransactionSynchronizationAdapter#afterCompletion(int)
		 */
		public void afterCompletion(int status) {
			if (status == TransactionSynchronization.STATUS_COMMITTED) {
				transactionComitted();
			}
			else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
				transactionRolledback();
			}
		}

		private void transactionComitted() {
			lastCommitPointPosition = getPosition();
			lastCommitPointRecordCount = currentRecordCount;
			skipRecords = new ArrayList();
		}

		private void transactionRolledback() {
			currentRecordCount = lastCommitPointRecordCount;
			setPosition(lastCommitPointPosition);
			unmarshallerAdapter = new UnmarshallerAdapter(unmarshaller, recordElementName, fc, encoding, useSaxParser);
		}
	}


	//package visibility method necessary to simulate transaction events in tests
	TransactionSynchronization getSynchronization() {
		return synchronization;
	}

}

package org.springframework.batch.io.xml;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StaxSource;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Unmarshaller adapter allows to use {@link org.springframework.oxm.Unmarshaller} for
 * iterative XML record processing.
 * 
 * {@link org.springframework.oxm.Unmarshaller} always processes whole XML
 * input at once, but we need to process only one record per one module
 * iteration (or per one call of the read() method).
 *  
 * Solution is to cut XML in smaller pieces (each piece contains single record) and call
 * the unmarshaller to process only this single piece (record).
 * 
 */
class UnmarshallerAdapter {

	private static final Log log = LogFactory.getLog(UnmarshallerAdapter.class);

	private FileChannel fc;

	private String encoding;

	//original unmarshaller - processes only pieces of XML passed from wrapper
	private Unmarshaller u;

	//regex matcher - used to cut XML piece holding one record
	private final Matcher matcher;

	private SourceFactory factory;

	/* ***** Constructor ***** */

	/**
	 * @param originalUnmarshaller unmarshaller to be wrapped
	 */
	public UnmarshallerAdapter(Unmarshaller originalUnmarshaller, String recordElementName, FileChannel fc,
			String encoding, boolean useSaxParser) {
		this.u = originalUnmarshaller;
		this.fc = fc;
		this.encoding = encoding;
		this.matcher = getMatcher(recordElementName);
		this.factory = getSourceFactory(useSaxParser);
	}

	/**
	 * Unmarshal next record.
	 * @see org.springframework.oxm.Unmarshaller#unmarshal(javax.xml.transform.Source)
	 */
	public Object unmarshal() throws XmlMappingException, IOException {
	
		Object result = null;

		String record = readNextRecord();
		if (record != null) {
			result = u.unmarshal(factory.getSource(record));
		}
		return result;
	}
	

	/**
	 * Read next piece of XML.
	 * @return xml string holding one record
	 */
	private String readNextRecord() {

		String result = null;

		if (matcher.find()) {
			result = matcher.group();
		}
		if (result != null) {
			try {
				fc.position(matcher.end());
			} catch (IOException e) {
				throw new IllegalStateException("Error while adjusting filechannel position");
			}
		}

		return result;
	}

	
	/* ***** Private helper methods ***** */

	/**
	 * Get regex matcher, which cuts XML to pieces 
	 * @param elemName name of the element that represents one record
	 * @return the matcher
	 */
	private Matcher getMatcher(String elemName) {

		CharSequence cs = null;

		try {
			// Create a read-only CharBuffer on the file
			ByteBuffer bbuf = fc.map(FileChannel.MapMode.READ_ONLY, fc.position(), fc.size() - fc.position());
			cs = Charset.forName(encoding).newDecoder().decode(bbuf);
		}
		catch (IOException ioe) {
			log.error(ioe);
			throw new DataAccessResourceFailureException("Unable to get input stream", ioe);
		}

		String prefix = (elemName.indexOf(':') < 0) ? "(?:[^:]*?:)?" : "";
		String re = "<" + prefix + elemName + "[^/>]*?/>|<" + prefix + elemName + "[\\s>][\\S\\s]*?</" + prefix
				+ elemName + ">";

		return Pattern.compile(re, Pattern.MULTILINE).matcher(cs);
	}

	/**
	 * Get SourceFactory which will create SAX or StAX source (based on useSaxParser flag)   
	 * @param useSaxParser
	 */
	private SourceFactory getSourceFactory(boolean useSaxParser) {

		SourceFactory sf;

		if (useSaxParser) {
			sf = new SourceFactory() {
				public Source getSource(String xml) {

					Source source = null;

					Reader r = new StringReader(xml);
					try {
						XMLReader reader;
						try {
							reader = XMLReaderFactory.createXMLReader();
						}
						catch (SAXException se) {
							reader = XMLReaderFactory.createXMLReader("org.apache.crimson.parser.XMLReaderImpl");
						}
						source = new SAXSource(reader, new org.xml.sax.InputSource(r));
					}
					catch (SAXException se) {
						log.error(se);
						throw new DataAccessResourceFailureException("Unable to get XML reader", se);
					}

					return source;
				}
			};
		}
		else {
			sf = new SourceFactory() {
				public Source getSource(String xml) {
					XMLInputFactory xsf = XMLInputFactory.newInstance();

					Source source = null;

					try {
						Reader r = new StringReader(xml);
						source = new StaxSource(xsf.createXMLStreamReader(r));
					}
					catch (XMLStreamException xse) {
						log.error(xse);
						throw new DataAccessResourceFailureException("Unable to get XML reader", xse);
					}
					return source;
				}
			};
		}

		return sf;
	}

	private interface SourceFactory {
		public Source getSource(String xml);
	}
}

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.file.AbstractMultiResourceItemWriterTests;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.xml.transform.StaxResult;

/**
 * Tests for {@link MultiResourceItemWriter} delegating to
 * {@link StaxEventItemWriter}.
 */
public class MultiResourceItemWriterXmlTests extends AbstractMultiResourceItemWriterTests {

	final private String xmlDocStart = "<root>";

	final private String xmlDocEnd = "</root>";

	@Override
	@Before
	public void setUp() throws Exception {
		delegate = new StaxEventItemWriter<String>() {
			{
				setMarshaller(new SimpleMarshaller());
			}
		};
		super.setUp();
	}

	/**
	 * Writes object's toString representation as tag.
	 */
	private static class SimpleMarshaller implements Marshaller {
		public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
			Assert.isInstanceOf(StaxResult.class, result);

			StaxResult staxResult = (StaxResult) result;
			try {
				XMLEventFactory factory = XMLEventFactory.newInstance();
				XMLEventWriter writer = staxResult.getXMLEventWriter();
				writer.add(factory.createStartDocument("UTF-8"));
				writer.add(factory.createStartElement("prefix", "namespace", graph.toString()));
				writer.add(factory.createEndElement("prefix", "namespace", graph.toString()));
				writer.add(factory.createEndDocument());
			}
			catch (XMLStreamException e) {
				throw new RuntimeException("Exception while writing to output file", e);
			}
		}

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}
	}

	@Override
	protected String readFile(File f) throws Exception {
		String content = super.readFile(f);
		//skip the <?xml ... ?> header to avoid platform issues with single vs. double quotes
		return content.substring(content.indexOf("?>") + 2);
	}

	@Test
	public void multiResourceWritingWithRestart() throws Exception {

		tested.write(Arrays.asList("1", "2", "3"));

		File part1 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(1));
		assertTrue(part1.exists());

		tested.write(Arrays.asList("4"));
		File part2 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(2));
		assertTrue(part2.exists());

		tested.update(executionContext);
		tested.close(executionContext);

		assertEquals(xmlDocStart + "<prefix:4></prefix:4>" + xmlDocEnd, readFile(part2));
		assertEquals(xmlDocStart + "<prefix:1></prefix:1><prefix:2></prefix:2><prefix:3></prefix:3>" + xmlDocEnd,
				readFile(part1));

		tested.open(executionContext);

		tested.write(Arrays.asList("5"));

		tested.write(Arrays.asList("6", "7", "8", "9"));
		File part3 = new File(file.getAbsolutePath() + suffixCreator.getSuffix(3));
		assertTrue(part3.exists());

		tested.close(executionContext);

		assertEquals(xmlDocStart + "<prefix:4></prefix:4><prefix:5></prefix:5>" + xmlDocEnd, readFile(part2));
		assertEquals(xmlDocStart
				+ "<prefix:6></prefix:6><prefix:7></prefix:7><prefix:8></prefix:8><prefix:9></prefix:9>" + xmlDocEnd,
				readFile(part3));
	}

}

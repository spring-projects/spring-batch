package org.springframework.batch.item.file.mapping;

import java.util.Map;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.springframework.batch.item.file.LineMapper;

/**
 * Interpret a line as a Json object and parse it up to a Map. The line should be a standard Json object, starting with
 * "{" and ending with "}" and composed of <code>name:value</code> pairs separated by commas. Whitespace is ignored,
 * e.g.
 * 
 * <pre>
 * { "foo" : "bar", "value" : 123 }
 * </pre>
 * 
 * The values can also be Json objects (which are converted to maps):
 * 
 * <pre>
 * { "foo": "bar", "map": { "one": 1, "two": 2}}
 * </pre>
 * 
 * @author Dave Syer
 * 
 */
public class JsonLineMapper implements LineMapper<Map<String, Object>> {

	private MappingJsonFactory factory = new MappingJsonFactory();

	/**
	 * Interpret the line as a Json object and create a Map from it.
	 * 
	 * @see LineMapper#mapLine(String, int)
	 */
	public Map<String, Object> mapLine(String line, int lineNumber) throws Exception {
		Map<String, Object> result;
		JsonParser parser = factory.createJsonParser(line);
		@SuppressWarnings("unchecked")
		Map<String, Object> token = parser.readValueAs(Map.class);
		result = token;
		return result;
	}

}

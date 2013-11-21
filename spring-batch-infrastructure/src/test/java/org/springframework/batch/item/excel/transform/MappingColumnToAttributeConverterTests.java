package org.springframework.batch.item.excel.transform;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MappingColumnToAttributeConverterTests {

    private final MappingColumnToAttributeConverter converter = new MappingColumnToAttributeConverter();

    @Before
    public void setup() {
        final Map<String, String> mappings = new HashMap<String, String>();
        mappings.put("foo", "bar1");
        mappings.put("baz", "bar2");
        mappings.put("with spaces", "noSpaces");
        this.converter.setMappings(mappings);
    }

    @Test
    public void convertColumnToAttribtue() {
        assertEquals("bar1", this.converter.toAttribute("foo"));
        assertEquals("noSpaces", this.converter.toAttribute("with spaces"));
        assertEquals("not existing", this.converter.toAttribute("not existing"));
        assertNull(this.converter.toAttribute(null));
    }

    @Test
    public void convertAttributeToColumn() {
        assertEquals("baz", this.converter.toColumn("bar2"));
        assertEquals("with spaces", this.converter.toColumn("noSpaces"));
        assertEquals("not existing", this.converter.toColumn("not existing"));
        assertNull(this.converter.toAttribute(null));
    }

}

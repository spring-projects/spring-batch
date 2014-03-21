package org.springframework.batch.item.excel.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PassThroughColumnToAttributeConverterTests {

    private final PassThroughColumnToAttributeConverter converter = new PassThroughColumnToAttributeConverter();

    @Test
    public void columnNameShouldRemainTheSame() {
        final String column = "column";
        assertEquals(column, this.converter.toAttribute(column));
        assertNull(this.converter.toAttribute(null));
    }

    @Test
    public void attributeNameShouldRemainTheSame() {
        final String attribute = "attribute";
        assertEquals(attribute, this.converter.toColumn(attribute));
        assertNull(this.converter.toColumn(null));
    }

}

package org.springframework.batch.item.excel.mapping;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Tests for {@link PassThroughRowMapper}.
 * 
 * @author Marten Deinum
 *
 */
public class PassThroughRowMapperTests {

    private final PassThroughRowMapper rowMapper = new PassThroughRowMapper();

    @Test
    public void mapRowShouldReturnSameValues() throws Exception {
        final String[] row = new String[] { "foo", "bar", "baz" };

        assertArrayEquals(row, this.rowMapper.mapRow(null, row, 0));
    }

    @Test
    public void mapRowShouldReturnNull() throws Exception {
        assertNull(this.rowMapper.mapRow(null, null, 0));
    }

}

package org.springframework.batch.item.excel.jxl;

import org.springframework.batch.item.excel.AbstractExcelItemReader;
import org.springframework.batch.item.excel.AbstractExcelItemReaderTests;

/**
 * Test
 */
public class JxlItemReaderTest extends AbstractExcelItemReaderTests {

    @Override
    protected AbstractExcelItemReader createExcelItemReader() {
        return new JxlItemReader();
    }

}

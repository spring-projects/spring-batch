package org.springframework.batch.item.excel.poi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.excel.AbstractExcelItemReader;
import org.springframework.batch.item.excel.AbstractExcelItemReaderTests;

public class PoiItemReaderXlsTests extends AbstractExcelItemReaderTests {

    private final Log logger = LogFactory.getLog(this.getClass());

    private PoiItemReader itemReader;

    @Override
    protected AbstractExcelItemReader createExcelItemReader() {
        return new PoiItemReader();
    }

}

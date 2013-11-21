package org.springframework.batch.item.excel.poi;

import org.springframework.batch.item.excel.AbstractExcelItemReader;
import org.springframework.batch.item.excel.AbstractExcelItemReaderTests;
import org.springframework.core.io.ClassPathResource;

public class PoiItemReaderXlsxTests extends AbstractExcelItemReaderTests {

    @Override
    protected void configureItemReader(AbstractExcelItemReader itemReader) {
        itemReader.setResource(new ClassPathResource("org/springframework/batch/item/excel/player.xlsx"));
    }

    @Override
    protected AbstractExcelItemReader createExcelItemReader() {
        return new PoiItemReader();
    }
}

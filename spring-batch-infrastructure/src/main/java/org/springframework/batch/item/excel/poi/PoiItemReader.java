package org.springframework.batch.item.excel.poi;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.batch.item.excel.AbstractExcelItemReader;
import org.springframework.batch.item.excel.Sheet;
import org.springframework.core.io.Resource;

/**
 * {@link org.springframework.batch.item.ItemReader} implementation which uses apache POI to read an Excel
 * file. It will read the file sheet for sheet and row for row. It is based on
 * the {@link org.springframework.batch.item.file.FlatFileItemReader}
 *
 * @author Marten Deinum
 *
 * @param <T> the type
 */
public class PoiItemReader<T> extends AbstractExcelItemReader<T> {

    private Workbook workbook;

    @Override
    protected Sheet getSheet(final int sheet) {
        return new PoiSheet(this.workbook.getSheetAt(sheet));
    }

    @Override
    protected int getNumberOfSheets() {
        return this.workbook.getNumberOfSheets();
    }

    @Override
    protected void openExcelFile(final Resource resource) throws Exception {
        this.workbook = WorkbookFactory.create(resource.getInputStream());
    }

}

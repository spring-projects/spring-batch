package org.springframework.batch.item.excel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.excel.mapping.PassThroughRowMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import static org.junit.Assert.assertEquals;

/**
 * Base class for testing Excel based item readers.
 *
 * @author Marten Deinum
 */
public abstract class AbstractExcelItemReaderTests  {

    private final Log logger = LogFactory.getLog(this.getClass());

    protected AbstractExcelItemReader itemReader;

    @Before
    public void setup() throws Exception {
        this.itemReader = createExcelItemReader();
        this.itemReader.setLinesToSkip(1); //First line is column names
        this.itemReader.setResource(new ClassPathResource("org/springframework/batch/item/excel/player.xls"));
        this.itemReader.setRowMapper(new PassThroughRowMapper());
        this.itemReader.setSkippedRowsCallback(new RowCallbackHandler() {

            public void handleRow(final Sheet sheet, final String[] row) {
                logger.info("Skipping: " + StringUtils.arrayToCommaDelimitedString(row));
            }
        });
        configureItemReader(this.itemReader);
        this.itemReader.afterPropertiesSet();
        this.itemReader.open(new ExecutionContext());
    }

    protected void configureItemReader(AbstractExcelItemReader itemReader) {
    }

    @After
    public void after() throws Exception {
        this.itemReader.close();
    }

    @Test
    public void readExcelFile() throws Exception {
        assertEquals(3, this.itemReader.getNumberOfSheets());
        String[] row = null;
        do {
            row = (String[]) this.itemReader.read();
            this.logger.debug("Read: "+ StringUtils.arrayToCommaDelimitedString(row));
        } while (row != null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRequiredProperties() throws Exception {
        final AbstractExcelItemReader reader = createExcelItemReader();
        reader.afterPropertiesSet();
    }

    protected abstract AbstractExcelItemReader createExcelItemReader();

}

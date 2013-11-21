package org.springframework.batch.item.excel.jxl;

import jxl.Cell;
import jxl.Workbook;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Tests for {@link org.springframework.batch.item.excel.jxl.JxlUtils}.
 * 
 * @author Marten Deinum
 *
 */
public class JxlUtilsTests {

    private final Cell cell1 = Mockito.mock(Cell.class);
    private final Cell cell2 = Mockito.mock(Cell.class);
    private final Cell cell3 = Mockito.mock(Cell.class);
    private final Cell cell4 = Mockito.mock(Cell.class);

    private final Workbook workbook = Mockito.mock(Workbook.class);

    @Before
    public void setup() {
        Mockito.when(this.cell1.getContents()).thenReturn("foo");
        Mockito.when(this.cell2.getContents()).thenReturn(" ");
        Mockito.when(this.cell3.getContents()).thenReturn("");
        Mockito.when(this.cell4.getContents()).thenReturn(null);
    }

    /**
     * Test the {@link org.springframework.batch.item.excel.jxl.JxlUtils#isEmpty(Cell)} method.
     */
    @Test
    public void checkIfCellsAreEmpty() {
        Assert.assertFalse("Cell1 should not be empty", JxlUtils.isEmpty(this.cell1));
        Assert.assertTrue("Cell2 should be empty", JxlUtils.isEmpty(this.cell2));
        Assert.assertTrue("Cell3 should be empty", JxlUtils.isEmpty(this.cell3));
        Assert.assertTrue("Cell4 should be empty", JxlUtils.isEmpty(this.cell4));
        Assert.assertTrue("[null] should be empty", JxlUtils.isEmpty((Cell) null));
    }

    /**
     * Test the {@link JxlUtils#isEmpty(Cell[])} method.
     */
    @Test
    public void checkIfRowIsEmpty() {
        Assert.assertTrue("[null] should be empty", JxlUtils.isEmpty((Cell[]) null));
        Assert.assertTrue("[null] should be empty", JxlUtils.isEmpty(new Cell[0]));
        Assert.assertFalse("Cell[]1 should not be empty",
                JxlUtils.isEmpty(new Cell[] { this.cell1, this.cell2, this.cell3 }));
        Assert.assertTrue("Cell[]2 should be empty", JxlUtils.isEmpty(new Cell[] { this.cell2, this.cell3, null }));
    }

    /**
     * Test the {@link JxlUtils#hasSheets(Workbook)} method.
     */
    @Test
    public void checkIfWorkbookHasSheets() {
        Assert.assertFalse("[null] doesn't have sheets.", JxlUtils.hasSheets(null));
        Mockito.when(this.workbook.getNumberOfSheets()).thenReturn(5);
        Assert.assertTrue("Workbook should have sheets.", JxlUtils.hasSheets(this.workbook));
        Mockito.when(this.workbook.getNumberOfSheets()).thenReturn(0);
        Assert.assertFalse("Workbook shouldn't have sheets.", JxlUtils.hasSheets(this.workbook));

    }

}

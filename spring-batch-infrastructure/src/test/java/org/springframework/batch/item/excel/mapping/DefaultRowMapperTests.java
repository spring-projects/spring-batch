package org.springframework.batch.item.excel.mapping;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.item.excel.Sheet;
import org.springframework.batch.item.excel.transform.RowTokenizer;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

import static org.mockito.Matchers.any;

/**
 * Tests for {@link DefaultRowMapper}.
 * @author Marten Deinum
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultRowMapperTests {

    @Mock
    private FieldSetMapper fieldSetMapper;

    @Mock
    private RowTokenizer rowTokenizer;

    @Test(expected = IllegalArgumentException.class)
    public void nullRowTokenizerShouldLeadToException() throws Exception {
        final DefaultRowMapper mapper = new DefaultRowMapper();
        mapper.setRowTokenizer(null);
        mapper.setFieldSetMapper(this.fieldSetMapper);
        mapper.afterPropertiesSet();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullFieldSetMapperShouldLeadToException() throws Exception {
        final DefaultRowMapper mapper = new DefaultRowMapper();
        mapper.setRowTokenizer(this.rowTokenizer);
        mapper.setFieldSetMapper(null);
        mapper.afterPropertiesSet();
    }

    @Test
    public void foo() throws Exception {
        final DefaultRowMapper mapper = new DefaultRowMapper();
        mapper.setRowTokenizer(this.rowTokenizer);
        mapper.setFieldSetMapper(this.fieldSetMapper);
        final FieldSet fs = Mockito.mock(FieldSet.class);
        final Object result = new Object();
        Mockito.when(this.rowTokenizer.tokenize(any(Sheet.class), any(String[].class))).thenReturn(fs);
        Mockito.when(this.fieldSetMapper.mapFieldSet(fs)).thenReturn(result);
        Assert.assertEquals(result, mapper.mapRow(null, null, 0));
        Mockito.verify(this.rowTokenizer, Mockito.times(1)).tokenize(any(Sheet.class), any(String[].class));
        Mockito.verify(this.fieldSetMapper, Mockito.times(1)).mapFieldSet(fs);
    }

}

package top.nomelin.iot.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.enums.IotDataType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class NumUtilTest {
    @Test
    void testConvertValueToInt() {
        assertEquals(123, NumUtil.convertValue(123, IotDataType.INT));
        assertEquals(123, NumUtil.convertValue(123.0, IotDataType.INT));
        assertEquals(123, NumUtil.convertValue("123", IotDataType.INT));
        assertThrows(SystemException.class, () -> NumUtil.convertValue("abc", IotDataType.INT));
        assertThrows(SystemException.class, () -> NumUtil.convertValue(123.45, IotDataType.INT));
    }

    @Test
    void testConvertValueToLong() {
        assertEquals(123L, NumUtil.convertValue(123, IotDataType.LONG));
        assertEquals(123L, NumUtil.convertValue(123.0, IotDataType.LONG));
        assertEquals(123L, NumUtil.convertValue("123", IotDataType.LONG));
        assertThrows(SystemException.class, () -> NumUtil.convertValue("abc", IotDataType.LONG));
    }

    @Test
    void testConvertValueToFloat() {
        assertEquals(123.45f, NumUtil.convertValue(123.45, IotDataType.FLOAT));
        assertEquals(123.0f, NumUtil.convertValue(123, IotDataType.FLOAT));
        assertEquals(123.45f, NumUtil.convertValue("123.45", IotDataType.FLOAT));
        assertEquals(123.45679f, NumUtil.convertValue("123.456789012345678", IotDataType.FLOAT));
        assertThrows(SystemException.class, () -> NumUtil.convertValue("abc", IotDataType.FLOAT));
    }

    @Test
    void testConvertValueToDouble() {
        assertEquals(123.45, NumUtil.convertValue(123.45, IotDataType.DOUBLE));
        assertEquals(123.0, NumUtil.convertValue(123, IotDataType.DOUBLE));
        assertEquals(123.45, NumUtil.convertValue("123.45", IotDataType.DOUBLE));
        assertThrows(SystemException.class, () -> NumUtil.convertValue("abc", IotDataType.DOUBLE));
    }

    @Test
    void testConvertValueToString() {
        assertEquals("123", NumUtil.convertValue(123, IotDataType.STRING));
        assertEquals("123.45", NumUtil.convertValue(123.45, IotDataType.STRING));
        assertEquals("test", NumUtil.convertValue("test", IotDataType.STRING));
    }

    @Test
    void testConvertValueWithNull() {
        assertThrows(SystemException.class, () -> NumUtil.convertValue(null, IotDataType.INT));
    }

    @Test
    void testConvertValueWithUnsupportedType() {
        assertThrows(SystemException.class, () -> NumUtil.convertValue(123, null));
    }
}
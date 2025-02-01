package top.nomelin.iot.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "1, 1",
            "10, 10",
            "999, 100",
            "1000, 1000",
            "500, 100",
            "0, 1",
            "-5, 1"
    })
    void adjustStorageGranularityTest(int input, int expected) {
        assertEquals(expected, util.adjustStorageGranularity(input));
    }

    @ParameterizedTest
    @CsvSource({
            // 格式: timestamp, granularity, expected
            "1672531200123, 1, 1672531200123",          // 2023-01-01T00:00:00.123+08:00
            "1672531200123, 1000, 1672531200000",       // 对齐到秒
            "1672531260456, 60000, 1672531260000",      // 对齐到分钟
            "1672531260456, 3600000, 1672531200000",    // 对齐到小时
            "1672531260456, 86400000, 1672488000000"    // 对齐到天
    })
    void alignToEast8ZoneTest(long timestamp, int granularity, long expected) {
        assertEquals(expected, util.alignToEast8Zone(timestamp, granularity));
    }

    @ParameterizedTest
    @CsvSource({
            "1234, 1000, 1000",
            "123456, 60000, 120000",
            "3000, 1000, 3000",
            "-500, 1000, -1000",
            "1672531260456, 3600000, 1672531200000"
    })
    void alignToStorageWindowTest(long timestamp, int granularity, long expected) {
        assertEquals(expected, util.alignToStorageWindow(timestamp, granularity));
    }

    @Test
    void testInvalidGranularity() {
        assertThrows(IllegalArgumentException.class, () -> {
            util.alignToEast8Zone(1672531260456L, 500);
        });
    }

    @Test
    void testMillisecondPrecision() {
        Instant now = Instant.now();
        assertEquals(now.toEpochMilli(),
                util.alignToEast8Zone(now.toEpochMilli(), 1));
    }
}
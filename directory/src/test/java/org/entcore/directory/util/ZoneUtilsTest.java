package org.entcore.directory.util;

import org.entcore.common.utils.DateUtils;
import org.junit.Test;

import java.time.ZoneId;

import static org.junit.Assert.assertEquals;

public class ZoneUtilsTest {

    @Test
    public void parseOrUtc_returnsGivenZone() {
        assertEquals(ZoneId.of("America/Bogota"), ZoneUtils.parseOrUtc("America/Bogota"));
    }

    @Test
    public void parseOrUtc_fallsBackToUtcWhenMissing() {
        assertEquals(DateUtils.UTC_ZONE, ZoneUtils.parseOrUtc(null));
        assertEquals(DateUtils.UTC_ZONE, ZoneUtils.parseOrUtc(""));
    }

    @Test
    public void parseOrUtc_fallsBackToUtcWhenInvalid() {
        assertEquals(DateUtils.UTC_ZONE, ZoneUtils.parseOrUtc("Not/AZone"));
        assertEquals(DateUtils.UTC_ZONE, ZoneUtils.parseOrUtc("+99:00"));
    }
}

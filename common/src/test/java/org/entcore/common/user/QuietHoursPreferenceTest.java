package org.entcore.common.user;

import org.entcore.common.user.dto.QuietHoursPreference;
import org.entcore.common.user.dto.QuietHoursPreference.ManagedBy;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuietHoursPreferenceTest {

    private static int[][] validSchedule() {
        int[][] s = new int[7][];
        s[0] = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 22, 23};
        s[1] = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 22, 23};
        s[2] = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 22, 23};
        s[3] = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 22, 23};
        s[4] = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 20, 21, 22, 23};
        s[5] = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
        s[6] = new int[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
        return s;
    }

    @Test
    public void testValidate_NullSchedule_Valid() {
        QuietHoursPreference pref = new QuietHoursPreference();
        assertTrue(pref.validate());
    }

    @Test
    public void testValidate_ValidSchedule() {
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setSchedule(validSchedule());
        assertTrue(pref.validate());
    }

    @Test
    public void testValidate_WrongNumberOfDays_Invalid() {
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setSchedule(new int[5][]);
        assertFalse(pref.validate());
    }

    @Test
    public void testValidate_HourOutOfRange_Invalid() {
        QuietHoursPreference pref = new QuietHoursPreference();
        int[][] s = validSchedule();
        s[0] = new int[]{0, 1, 25}; // 25 is invalid
        pref.setSchedule(s);
        assertFalse(pref.validate());
    }

    @Test
    public void testValidate_NegativeHour_Invalid() {
        QuietHoursPreference pref = new QuietHoursPreference();
        int[][] s = validSchedule();
        s[0] = new int[]{-1, 0, 1};
        pref.setSchedule(s);
        assertFalse(pref.validate());
    }

    @Test
    public void testValidate_DuplicateHour_Invalid() {
        QuietHoursPreference pref = new QuietHoursPreference();
        int[][] s = validSchedule();
        s[0] = new int[]{0, 1, 1}; // duplicate 1
        pref.setSchedule(s);
        assertFalse(pref.validate());
    }

    @Test
    public void testValidate_NullDayArray_Invalid() {
        QuietHoursPreference pref = new QuietHoursPreference();
        int[][] s = validSchedule();
        s[3] = null; // null day
        pref.setSchedule(s);
        assertFalse(pref.validate());
    }

    @Test
    public void testValidate_EmptyDayArray_Valid() {
        // Empty array for a day is valid (no quiet hours that day)
        QuietHoursPreference pref = new QuietHoursPreference();
        int[][] s = validSchedule();
        s[0] = new int[]{}; // no quiet hours Monday
        pref.setSchedule(s);
        assertTrue(pref.validate());
    }

    // --- enabled / managedBy defaults ---

    @Test
    public void testDefaults_EnabledIsFalse() {
        // primitive boolean -> default false
        QuietHoursPreference pref = new QuietHoursPreference();
        assertFalse(pref.isEnabled());
    }

    @Test
    public void testDefaults_ManagedByIsNull() {
        // enum reference -> default null
        QuietHoursPreference pref = new QuietHoursPreference();
        assertNull(pref.getManagedBy());
    }

    @Test
    public void testSetEnabled_True() {
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setEnabled(true);
        assertTrue(pref.isEnabled());
    }

    @Test
    public void testSetManagedBy_Structure() {
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setManagedBy(ManagedBy.STRUCTURE);
        assertEquals(ManagedBy.STRUCTURE, pref.getManagedBy());
    }

    @Test
    public void testSetManagedBy_User() {
        QuietHoursPreference pref = new QuietHoursPreference();
        pref.setManagedBy(ManagedBy.USER);
        assertEquals(ManagedBy.USER, pref.getManagedBy());
    }
}

package org.entcore.common.validation;

import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.entcore.common.validation.PhoneValidation.PhoneValidationResult;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class PhoneValidationTest {

    // ========== Valid Mobile Numbers ==========

    @Test
    public void testValidFrenchMobileWithPrefix(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+33612345678");
        context.assertTrue(result.isValid(), "French mobile with +33 should be valid");
        context.assertEquals("+33612345678", result.getNormalizedNumber());
        context.assertEquals(PhoneNumberType.MOBILE, result.getNumberType());
    }

    @Test
    public void testValidFrenchMobileWithoutPrefix(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0612345678");
        context.assertTrue(result.isValid(), "French mobile without prefix should be valid");
        context.assertEquals("+33612345678", result.getNormalizedNumber());
        context.assertEquals(PhoneNumberType.MOBILE, result.getNumberType());
    }

    @Test
    public void testValidFrenchMobileWith0033Prefix(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0033612345678");
        context.assertTrue(result.isValid(), "French mobile with 0033 should be valid");
        context.assertEquals("+33612345678", result.getNormalizedNumber());
    }

    @Test
    public void testValidFrenchMobileWithSpaces(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+33 6 12 34 56 78");
        context.assertTrue(result.isValid(), "French mobile with spaces should be valid");
        context.assertEquals("+33612345678", result.getNormalizedNumber());
    }

    @Test
    public void testValidFrenchMobileWithDashes(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("06-12-34-56-78");
        context.assertTrue(result.isValid(), "French mobile with dashes should be valid");
        context.assertEquals("+33612345678", result.getNormalizedNumber());
    }

    @Test
    public void testValidFrenchMobile07(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0712345678");
        context.assertTrue(result.isValid(), "French mobile 07 should be valid");
        context.assertEquals("+33712345678", result.getNormalizedNumber());
    }

    // ========== International Numbers ==========

    @Test
    public void testValidUSMobile(TestContext context) {
        // US numbers are FIXED_LINE_OR_MOBILE
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+12025551234", "US");
        context.assertTrue(result.isValid(), "US mobile should be valid");
        context.assertEquals("+12025551234", result.getNormalizedNumber());
    }

    @Test
    public void testValidGermanMobile(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+4915123456789", "DE");
        context.assertTrue(result.isValid(), "German mobile should be valid");
        context.assertEquals("+4915123456789", result.getNormalizedNumber());
        context.assertEquals(PhoneNumberType.MOBILE, result.getNumberType());
    }

    @Test
    public void testValidUKMobile(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+447911123456", "GB");
        context.assertTrue(result.isValid(), "UK mobile should be valid");
        context.assertEquals("+447911123456", result.getNormalizedNumber());
        context.assertEquals(PhoneNumberType.MOBILE, result.getNumberType());
    }

    @Test
    public void testValidSpanishMobile(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+34612345678", "ES");
        context.assertTrue(result.isValid(), "Spanish mobile should be valid");
        context.assertEquals("+34612345678", result.getNormalizedNumber());
    }

    // ========== Empty/Null Numbers ==========

    @Test
    public void testNullNumber(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber(null);
        context.assertFalse(result.isValid(), "Null should be invalid");
        context.assertEquals(PhoneValidation.ERROR_EMPTY, result.getErrorCode());
    }

    @Test
    public void testEmptyString(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("");
        context.assertFalse(result.isValid(), "Empty string should be invalid");
        context.assertEquals(PhoneValidation.ERROR_EMPTY, result.getErrorCode());
    }

    @Test
    public void testWhitespaceOnly(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("   ");
        context.assertFalse(result.isValid(), "Whitespace only should be invalid");
        context.assertEquals(PhoneValidation.ERROR_EMPTY, result.getErrorCode());
    }

    // ========== Invalid Format ==========

    @Test
    public void testInvalidFormatNotANumber(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("not-a-number");
        context.assertFalse(result.isValid(), "Non-number should be invalid");
        context.assertEquals(PhoneValidation.ERROR_INVALID_FORMAT, result.getErrorCode());
    }

    @Test
    public void testInvalidFormatTooShort(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("123");
        context.assertFalse(result.isValid(), "Too short number should be invalid");
    }

    // ========== Invalid Numbers ==========

    @Test
    public void testInvalidFrenchNumber(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+33999999999");
        context.assertFalse(result.isValid(), "Invalid French number should be rejected");
        context.assertEquals(PhoneValidation.ERROR_INVALID_NUMBER, result.getErrorCode());
    }

    // ========== Fixed Line Rejected ==========

    @Test
    public void testFrenchFixedLineRejected(TestContext context) {
        // French fixed line number (01...)
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("+33123456789");
        context.assertFalse(result.isValid(), "French fixed line should be rejected");
        context.assertEquals(PhoneValidation.ERROR_NOT_MOBILE, result.getErrorCode());
    }

    @Test
    public void testFrenchFixedLine02Rejected(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0234567890");
        context.assertFalse(result.isValid(), "French fixed line 02 should be rejected");
        context.assertEquals(PhoneValidation.ERROR_NOT_MOBILE, result.getErrorCode());
    }

    // ========== Premium Rate Blocked ==========

    @Test
    public void testFrenchPremiumRate0890Blocked(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0890123456");
        context.assertFalse(result.isValid(), "French premium 0890 should be blocked");
        context.assertEquals(PhoneValidation.ERROR_PREMIUM_BLOCKED, result.getErrorCode());
    }

    @Test
    public void testFrenchPremiumRate0891Blocked(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0891123456");
        context.assertFalse(result.isValid(), "French premium 0891 should be blocked");
        context.assertEquals(PhoneValidation.ERROR_PREMIUM_BLOCKED, result.getErrorCode());
    }

    @Test
    public void testFrenchPremiumRate0892Blocked(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0892123456");
        context.assertFalse(result.isValid(), "French premium 0892 should be blocked");
        context.assertEquals(PhoneValidation.ERROR_PREMIUM_BLOCKED, result.getErrorCode());
    }

    @Test
    public void testFrenchPremiumRate0897Blocked(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0897123456");
        context.assertFalse(result.isValid(), "French premium 0897 should be blocked");
        context.assertEquals(PhoneValidation.ERROR_PREMIUM_BLOCKED, result.getErrorCode());
    }

    @Test
    public void testFrenchPremiumRate0899Blocked(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0899123456");
        context.assertFalse(result.isValid(), "French premium 0899 should be blocked");
        context.assertEquals(PhoneValidation.ERROR_PREMIUM_BLOCKED, result.getErrorCode());
    }

    // ========== Toll Free Blocked ==========

    @Test
    public void testFrenchTollFree0800Blocked(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0800123456");
        context.assertFalse(result.isValid(), "French toll-free 0800 should be blocked");
        context.assertEquals(PhoneValidation.ERROR_PREMIUM_BLOCKED, result.getErrorCode());
    }

    // ========== toE164 Method ==========

    @Test
    public void testToE164ValidNumber(TestContext context) {
        String result = PhoneValidation.toE164("0612345678", "FR");
        context.assertEquals("+33612345678", result);
    }

    @Test
    public void testToE164NullNumber(TestContext context) {
        String result = PhoneValidation.toE164(null, "FR");
        context.assertNull(result);
    }

    @Test
    public void testToE164InvalidNumber(TestContext context) {
        String result = PhoneValidation.toE164("invalid", "FR");
        context.assertNull(result);
    }

    // ========== Default Region ==========

    @Test
    public void testDefaultRegionFR(TestContext context) {
        // Without specifying region, FR should be default
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0612345678");
        context.assertTrue(result.isValid());
        context.assertEquals("+33612345678", result.getNormalizedNumber());
    }

    @Test
    public void testNullDefaultRegionUsesFR(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0612345678", null);
        context.assertTrue(result.isValid());
        context.assertEquals("+33612345678", result.getNormalizedNumber());
    }

    @Test
    public void testEmptyDefaultRegionUsesFR(TestContext context) {
        PhoneValidationResult result = PhoneValidation.validateMobileNumber("0612345678", "");
        context.assertTrue(result.isValid());
        context.assertEquals("+33612345678", result.getNormalizedNumber());
    }
}

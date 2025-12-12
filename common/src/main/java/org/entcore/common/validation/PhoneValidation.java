/* Copyright © "Open Digital Education", 2024
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.
 *
 */

package org.entcore.common.validation;

import org.entcore.common.utils.StringUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberType;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

/**
 * Utility class for validating phone numbers using Google's libphonenumber.
 * Ensures phone numbers are mobile-compatible and not premium-rate.
 */
public class PhoneValidation {

    private static final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    public static final String DEFAULT_REGION = "FR";

    // Error codes (matching i18n keys)
    public static final String ERROR_EMPTY = "phone.validation.error.empty";
    public static final String ERROR_INVALID_FORMAT = "phone.validation.error.invalid.format";
    public static final String ERROR_INVALID_NUMBER = "phone.validation.error.invalid.number";
    public static final String ERROR_NOT_MOBILE = "phone.validation.error.not.mobile";
    public static final String ERROR_PREMIUM_BLOCKED = "phone.validation.error.premium.blocked";

    /**
     * Result class encapsulating validation outcome.
     */
    public static class PhoneValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String normalizedNumber;
        private final PhoneNumberType numberType;

        private PhoneValidationResult(boolean valid, String errorCode, String normalizedNumber, PhoneNumberType numberType) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.normalizedNumber = normalizedNumber;
            this.numberType = numberType;
        }

        public static PhoneValidationResult success(String normalizedNumber, PhoneNumberType type) {
            return new PhoneValidationResult(true, null, normalizedNumber, type);
        }

        public static PhoneValidationResult failure(String errorCode) {
            return new PhoneValidationResult(false, errorCode, null, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getNormalizedNumber() {
            return normalizedNumber;
        }

        public PhoneNumberType getNumberType() {
            return numberType;
        }
    }

    /**
     * Validates a phone number for mobile SMS sending with default region FR.
     *
     * @param phoneNumber The phone number to validate
     * @return PhoneValidationResult with validation outcome
     */
    public static PhoneValidationResult validateMobileNumber(String phoneNumber) {
        return validateMobileNumber(phoneNumber, DEFAULT_REGION);
    }

    /**
     * Validates a phone number for mobile SMS sending.
     *
     * Checks:
     * 1. Number is not empty
     * 2. Number can be parsed
     * 3. Number is valid according to libphonenumber
     * 4. Number type is MOBILE or FIXED_LINE_OR_MOBILE
     * 5. Number is not a premium-rate or blocked type
     *
     * @param phoneNumber   The phone number to validate (with or without prefix)
     * @param defaultRegion ISO 3166-1 alpha-2 region code if no country prefix
     * @return PhoneValidationResult with validation outcome
     */
    public static PhoneValidationResult validateMobileNumber(String phoneNumber, String defaultRegion) {
        // Check empty
        if (StringUtils.isEmpty(phoneNumber)) {
            return PhoneValidationResult.failure(ERROR_EMPTY);
        }

        String cleanNumber = phoneNumber.trim();
        String region = StringUtils.isEmpty(defaultRegion) ? DEFAULT_REGION : defaultRegion;

        PhoneNumber parsedNumber;
        try {
            parsedNumber = phoneUtil.parse(cleanNumber, region);
        } catch (NumberParseException e) {
            return PhoneValidationResult.failure(ERROR_INVALID_FORMAT);
        }

        // Check if valid number
        if (!phoneUtil.isValidNumber(parsedNumber)) {
            return PhoneValidationResult.failure(ERROR_INVALID_NUMBER);
        }

        // Get number type
        PhoneNumberType numberType = phoneUtil.getNumberType(parsedNumber);

        // Check for premium-rate / blocked types first (more specific error)
        if (isPremiumRateType(numberType)) {
            return PhoneValidationResult.failure(ERROR_PREMIUM_BLOCKED);
        }

        // Check if mobile or fixed-line-or-mobile
        if (!isAllowedMobileType(numberType)) {
            return PhoneValidationResult.failure(ERROR_NOT_MOBILE);
        }

        // Valid - return normalized E.164 format
        String normalized = phoneUtil.format(parsedNumber, PhoneNumberFormat.E164);
        return PhoneValidationResult.success(normalized, numberType);
    }

    /**
     * Checks if the number type is acceptable for mobile SMS.
     *
     * @param type The phone number type
     * @return true if the type is allowed for SMS
     */
    private static boolean isAllowedMobileType(PhoneNumberType type) {
        return type == PhoneNumberType.MOBILE ||
               type == PhoneNumberType.FIXED_LINE_OR_MOBILE;
    }

    /**
     * Checks if the number type is a blocked premium-rate type.
     *
     * @param type The phone number type
     * @return true if the type is a premium-rate or blocked type
     */
    private static boolean isPremiumRateType(PhoneNumberType type) {
        return type == PhoneNumberType.PREMIUM_RATE ||
               type == PhoneNumberType.SHARED_COST ||
               type == PhoneNumberType.TOLL_FREE ||
               type == PhoneNumberType.UAN ||
               type == PhoneNumberType.VOICEMAIL;
    }

    /**
     * Converts a phone number to E.164 format.
     * Returns null if the number cannot be parsed or is invalid.
     *
     * @param phoneNumber   The phone number to convert
     * @param defaultRegion ISO 3166-1 alpha-2 region code if no country prefix
     * @return The phone number in E.164 format, or null if invalid
     */
    public static String toE164(String phoneNumber, String defaultRegion) {
        if (StringUtils.isEmpty(phoneNumber)) {
            return null;
        }

        try {
            String region = StringUtils.isEmpty(defaultRegion) ? DEFAULT_REGION : defaultRegion;
            PhoneNumber parsed = phoneUtil.parse(phoneNumber.trim(), region);

            if (phoneUtil.isValidNumber(parsed)) {
                return phoneUtil.format(parsed, PhoneNumberFormat.E164);
            }
        } catch (NumberParseException e) {
            // Return null on parse error
        }

        return null;
    }
}

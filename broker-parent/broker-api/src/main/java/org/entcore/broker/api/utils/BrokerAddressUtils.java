package org.entcore.broker.api.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for manipulating broker addresses and placeholders
 */
public class BrokerAddressUtils {

    /**
     * Convert an array of AddressParameter objects to a parameter map
     * 
     * @param addressParameters Array of address parameters
     * @return Map of parameter names to values
     */
    public static Map<String, String> getParametersMap(AddressParameter[] addressParameters) {
        final Map<String, String> params;
        if(addressParameters == null) {
            params = Collections.emptyMap();
        } else {
            params = new HashMap<>();
            for (AddressParameter addressParameter : addressParameters) {
                params.put(addressParameter.getName(), addressParameter.getValue());
            }
        }
        return params;
    }

    /**
     * Replace placeholders in a template string with values from a map
     * Placeholders are in the format {key} and will be replaced with the 
     * corresponding value from the map
     * 
     * @param template The template string with placeholders
     * @param params Map of parameter names to values
     * @return The template with placeholders replaced by values
     */
    public static String replacePlaceholders(String template, Map<String, String> params) {
        if (template == null || params == null || params.isEmpty()) {
            return template;
        }
        
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (key != null && value != null) {
                result = result.replace("{" + key + "}", value);
            }
        }
        return result;
    }
}
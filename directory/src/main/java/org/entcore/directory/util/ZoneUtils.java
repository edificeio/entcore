package org.entcore.directory.util;

import org.entcore.common.utils.DateUtils;
import org.entcore.common.utils.StringUtils;

import java.time.DateTimeException;
import java.time.ZoneId;

/**
 * Résolution des fuseaux horaires transmis par les clients.
 */
public final class ZoneUtils {

    private ZoneUtils() {
    }

    /**
     * Convertit un identifiant de fuseau IANA (ex. {@code Europe/Madrid}) en {@link ZoneId}.
     *
     * @param zoneId l'identifiant transmis par le client, éventuellement absent
     * @return le fuseau correspondant, ou UTC si l'identifiant est absent ou invalide
     */
    public static ZoneId parseOrUtc(String zoneId) {
        if (StringUtils.isEmpty(zoneId)) {
            return DateUtils.UTC_ZONE;
        }
        try {
            return ZoneId.of(zoneId);
        } catch (DateTimeException e) {
            return DateUtils.UTC_ZONE;
        }
    }
}

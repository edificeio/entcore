package org.entcore.directory.util;

import org.entcore.common.utils.StringUtils;
import org.entcore.directory.dto.LinkDTO;

/**
 * Validation des charges utiles de l'API des liens utilisateur.
 */
public final class UserLinkValidator {

    /** Longueur maximale du nom d'un lien, contrainte par son affichage sur la page d'accueil. */
    public static final int NAME_MAX_LENGTH = 80;

    /** Clé d'erreur renvoyée quand le nom dépasse {@link #NAME_MAX_LENGTH}. */
    public static final String NAME_TOO_LONG = "directory.user.link.name.too.long";

    /** Clé d'erreur renvoyée quand l'url est absente ou vide. */
    public static final String URL_REQUIRED = "directory.user.link.url.required";

    private UserLinkValidator() {
    }

    /**
     * Valide la charge utile d'une création de lien.
     *
     * @param link la charge utile à valider
     * @return la clé d'erreur i18n si la charge utile est invalide, null sinon
     */
    public static String validatePayload(LinkDTO link) {
        if (StringUtils.isEmpty(link.getUrl())) {
            return URL_REQUIRED;
        }
        if (link.getName() != null && link.getName().length() > NAME_MAX_LENGTH) {
            return NAME_TOO_LONG;
        }
        return null;
    }
}

package edu.one.core.sync.aaf;

/**
 *
 * @author bperez
 */
public class AafConstantes {
    // Filtres pour récupérer les fichiers d'un certain type dans un répertoire
	public static final String[] AAF_FILTERS =
			{".*_EtabEducNat.*",".*_PersRelEleve.*",".*_Eleve.*",".*_PersEducNat.*"};

    // Types d'opération
    public static final String ADD_TAG = "addRequest";
    public static final String UPDATE_TAG = "modifyRequest";
    public static final String DELETE_TAG = "deleteRequest";

    // Balises
    public static final String ATTRIBUTES_TAG = "attributes";
    public static final String ATTR_TAG = "attr";
    public static final String VALUE_TAG = "value";
    public static final String IDENTIFIER_TAG = "identifier";
    public static final String ID_TAG = "id";
	public static final String FIC_ALIM_TAG = "ficAlimMENESR";
	public static final String OPERATIONAL_ATTRIBUTES_TAG = "operationalAttributes";
    public static final int ATTRIBUTE_NAME_INDEX = 0;

	// Attributs pour relations
	public static final String CLASSES_ATTR = "ENTPersonClasses";
	public static final String GROUPES_ATTR = "ENTPersonGroupes";
	public static final String ECOLE_ATTR = "ENTPersonStructRattach";
	public static final String PARENTS_ATTR = "ENTEleveAutoriteParentale";
	public static final int PARENT_ID_INDEX = 0;

	// Attributs pour création groupe
	public static final String GROUPE_ECOLE_ATTR = "ENTGroupeEcoleProprietaire";
	public static final String GROUPE_TYPE_ATTR = "ENTGroupeType";
	public static final String GROUPE_NOM_ATTR = "ENTGroupeNom";
	public static final int GROUPE_ECOLE_INDEX = 0;
	public static final int GROUPE_TYPE_INDEX = 1;
	public static final int GROUPE_NOM_INDEX = 2;

	// Attributs pour Wordpress
	public static final String STRUCTURE_NOM_ATTR = "ENTStructureNomCourant";
	public static final String PERSONNE_NOM_ATTR = "ENTPersonNom";
	public static final String PERSONNE_PRENOM_ATTR = "ENTPersonPrenom";

	// Attributs calculés
	public static final String PERSONNE_ID_ATTR = "ENTPersonIdentifiant";
	public static final String PERSONNE_LOGIN_ATTR = "ENTPersonLogin";
	public static final String PERSONNE_PASSWORD_ATTR = "ENTPersonMotDePasse";
	public static final String PERSONNE_PROFIL_ATTR = "ENTPersonProfils";
	public static final String PERSONNE_NOM_AFFICHAGE_ATTR = "ENTPersonNomAffichage";
	public static final String PERSONNE_SEXE_ATTR = "ENTPersonSexe";
	public static final String STRUCTURE_ID_ATTR = "ENTStructureIdentifiant";
	public static final String GROUPE_ID_ATTR = "ENTGroupeIdentifiant";

	public static final String PERSONNE_CIVILITE_ATTR = "ENTPersonCivilite";

    // Séparateurs
    public static final String MULTIVALUE_SEPARATOR = "||";
    public static final String AAF_SEPARATOR = "\\$";

	// Types de relation
	public static final String REL_APPARTIENT = "APPARTIENT";
	public static final String REL_PARENT = "EN RELATION AVEC";

	// alphabet utilisé pour les mots de passe.
	public static String[] alphabet = new String[28];
	static {
		alphabet[0] = "a";
		alphabet[1] = "b";
		alphabet[2] = "c";
		alphabet[3] = "d";
		alphabet[4] = "e";
		alphabet[5] = "f";
		alphabet[6] = "g";
		alphabet[7] = "h";
		alphabet[8] = "j";
		alphabet[9] = "k";
		alphabet[10] = "m";
		alphabet[11] = "n";
		alphabet[12] = "p";
		alphabet[13] = "r";
		alphabet[14] = "s";
		alphabet[15] = "t";
		alphabet[16] = "v";
		alphabet[17] = "w";
		alphabet[18] = "x";
		alphabet[19] = "y";
		alphabet[20] = "z";
		alphabet[21] = "3";
		alphabet[22] = "4";
		alphabet[23] = "5";
		alphabet[24] = "6";
		alphabet[25] = "7";
		alphabet[26] = "8";
		alphabet[27] = "9";
	}
}
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

    // Séparateurs
//    public static final String ATTR_SEPARATOR = "||";
//    public static final String VALUE_SEPARATOR = "==";
    public static final String MULTIVALUE_SEPARATOR = "||";
//    public static final String REQUEST_SEPARATOR = "**";
    public static final String AAF_SEPARATOR = "\\$";
	
	// Types de relation
	public static final String REL_APPARTIENT = "APPARTIENT";
	public static final String REL_PARENT = "EN RELATION AVEC";
}
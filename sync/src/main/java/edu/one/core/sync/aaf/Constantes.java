package edu.one.core.sync.aaf;

/**
 *
 * @author bperez
 */
public class Constantes {
    // Filtres pour récupérer les fichiers d'un certain type dans un répertoire
	public static final String[] AAF_FILTERS = 
			{".*_EtabEducNat_.*",".*_PersRelEleve_.*",".*_Eleve_.*",".*_PersEducNat_.*"};
   
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
}

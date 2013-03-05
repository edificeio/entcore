/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.constants;

/**
 *
 * @author bperez
 */
public class SyncConstants {
    // Filters (type of files)
    public static final String STUDENT_FILTER = "_Eleve_";
    public static final String RELATIVES_FILTER = "_PersRelEleve_";
    public static final String STAFF_FILTER = "_PersEducNat_";
    public static final String SCHOOL_FILTER = "_EtabEducNat_";
   
    // Types of processing
    public static final String ADD_TAG = "addRequest";
    public static final String UPDATE_TAG = "modifyRequest";
    public static final String DELETE_TAG = "deleteRequest";

    // Tag names
    public static final String ATTRIBUTES_TAG = "attributes";
    public static final String MODIFICATIONS_TAG = "modifications";
    public static final String ATTRIBUTE_NAME_TAG = "name";
    public static final String VALUE_TAG = "value";
    public static final String ID_TAG = "identifier";
    public static final String ID_VALUE_TAG = "id";

    // Values
    public static final String TRUE_VALUE = "O";
    public static final String FALSE_VALUE = "N";
    public static final String SEPARATOR_VALUE = "$";
    
}

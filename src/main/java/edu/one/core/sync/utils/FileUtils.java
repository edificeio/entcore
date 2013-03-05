/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.one.core.sync.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author bperez
 */
public class FileUtils {
    /**
    * get files list from a given folder
    * @param folderPathStr : folder path to browse
    * @return folder files list
    */
    public static String [] getFiles(String folderPathStr) throws Exception {
	   String [] filesArray = null;
	   File file = new File(folderPathStr);
	   if (file.isDirectory()) {
		  filesArray = file.list();
	   } else {
		  // TODO : log + exception
	   }
	   return filesArray;        
    }
    
    /**
    * filter a file list on file names
    * @param filesArray : files to filter
    * @param filterStr : filter to apply on files name
    * @param folderPathStr : folder path
    * @return list of files that correspond to given filter
    */
    public static List < File > getFilesByType(String [] filesArray, String filterStr, String folderPathStr) {
	   List < File > filteredFilesList = new ArrayList <  >();
	   for (String fileNameStr : filesArray) {
		  if (fileNameStr != null && fileNameStr.contains(filterStr)) {
			 filteredFilesList.add(new File(folderPathStr + fileNameStr));
		  }
	   }
	   return filteredFilesList;
    }
    
    /**
    * Open a file as a JDOM xml document
    * @param file : source file
    * @return xml document
    */
    public static Document getXmlDocument(File file) throws Exception {
	   try {
		  SAXBuilder sxb = new SAXBuilder();
		  return sxb.build(file);
	   } catch (IOException | JDOMException exception) {
		  // TODO : log + exception
		  throw exception;
	   }
    }
}

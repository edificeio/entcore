/*
 * Copyright © "Open Digital Education", 2014
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

 */

package org.entcore.common.processor;

import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import org.entcore.common.controller.RightsController;
import org.entcore.common.http.filter.IgnoreCsrf;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.http.filter.Trace;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.*;
import java.util.*;

@SupportedAnnotationTypes({"fr.wseduc.security.SecuredAction", "fr.wseduc.bus.BusAddress",
		"fr.wseduc.rs.Get", "fr.wseduc.rs.Post", "fr.wseduc.rs.Delete", "fr.wseduc.rs.Put",
		"fr.wseduc.security.ResourceFilter", "fr.wseduc.rs.ApiDoc", "fr.wseduc.rs.ApiPrefixDoc",
		"org.entcore.common.http.filter.ResourceFilter", "org.entcore.common.http.filter.IgnoreCsrf",
        "org.entcore.common.http.filter.Trace"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ControllerAnnotationProcessor extends fr.wseduc.processor.ControllerAnnotationProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		super.process(annotations, roundEnv);
		resourceFilter(roundEnv);
		ignoreCsrf(roundEnv);
		trace(roundEnv);
		return false;
	}

	private void trace(RoundEnvironment roundEnv) {
	    final Map<String, Set<String>> filtersMap = new HashMap<>();
	    Set<String> filters = new TreeSet<>();
	    filtersMap.put("Trace", filters);
	    for (Element element : roundEnv.getElementsAnnotatedWith(Trace.class)) {
	        Trace annotation = element.getAnnotation(Trace.class);
	        TypeElement clazz = (TypeElement) element.getEnclosingElement();
	        if (annotation == null || !isMethod(element) || clazz == null) {
	            continue;
            }
	        filters.add("{ \"method\" : \"" + clazz.getQualifiedName().toString() + "|" + element.getSimpleName().toString() +
                    "\", \"value\" : \"" + annotation.value() +
                    "\", \"body\" : " + annotation.body() + " }");
        }

        if (filters.size() > 0) {
            writeFile("", "", filtersMap);
        }
    }

	private void ignoreCsrf(RoundEnvironment roundEnv) {
		final Map<String,Set<String>> filtersMap = new HashMap<>();
		Set<String> filters = new TreeSet<>();
		filtersMap.put("IgnoreCsrf", filters);
		for (Element element : roundEnv.getElementsAnnotatedWith(IgnoreCsrf.class)) {
			IgnoreCsrf annotation = element.getAnnotation(IgnoreCsrf.class);
			TypeElement clazz = (TypeElement) element.getEnclosingElement();
			if(annotation == null || !isMethod(element) || clazz == null) {
				continue;
			}
			filters.add("{ \"method\" : \"" + clazz.getQualifiedName().toString() + "|" +
					element.getSimpleName().toString() + "\", \"ignore\" :  " + annotation.value() + " }");
		}
		if (filters.size() > 0) {
			writeFile("", "", filtersMap);
		}
	}

	private void resourceFilter(RoundEnvironment roundEnv) {
		final Map<String,Set<String>> filtersMap = new HashMap<>();
		Set<String> filters = new TreeSet<>();
		filtersMap.put("Filters", filters);
		Set<String> filtersClasses = new TreeSet<>();
		for (Element element : roundEnv.getElementsAnnotatedWith(ResourceFilter.class)) {
			ResourceFilter annotation = element.getAnnotation(ResourceFilter.class);
			TypeElement clazz = (TypeElement) element.getEnclosingElement();
			if(annotation == null || !isMethod(element) || clazz == null) {
				continue;
			}
			TypeMirror v = null;
			try {
				annotation.value();
			} catch(MirroredTypeException e){
				v = e.getTypeMirror();
			}
			String value = String.format("%s", v);
			filters.add("{ \"method\" : \"" + clazz.getQualifiedName().toString() + "|" +
					element.getSimpleName().toString() + "\", \"filter\" : \"" + value + "\" }");
			filtersClasses.add(value);
		}
		if (filters.size() > 0) {
			writeFile("", "", filtersMap);
			writeMetaInfServices(ResourcesProvider.class.getName(), filtersClasses);
		}
	}

	private void writeMetaInfServices(String service, Set<String> servicesImpl) {
		Filer filer = processingEnv.getFiler();
		try {
			FileObject f = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + service);
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(f.openOutputStream(), "UTF-8"));
			for (String value : servicesImpl) {
				pw.println(value);
			}
			pw.close();
		} catch (IOException e) {
			error("Failed to write META-INF/services/" + service);
		}
	}

	@Override
	protected void checkRights(SecuredAction annotation, TypeElement clazz) {
		super.checkRights(annotation, clazz);
		if (ActionType.RESOURCE.equals(annotation.type())) {
			if (annotation.value().isEmpty()) {
				return;
			}
			String sharingType = annotation.value().substring(annotation.value().lastIndexOf('.') + 1);
			if (!RightsController.allowedSharingRights.contains(sharingType)) {
				throw new RuntimeException("Invalid sharing type : " + annotation.value());
			}
		}
	}

}

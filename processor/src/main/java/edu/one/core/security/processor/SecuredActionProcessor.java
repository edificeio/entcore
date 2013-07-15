package edu.one.core.security.processor;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import edu.one.core.security.SecuredAction;

@SupportedAnnotationTypes("edu.one.core.security.SecuredAction")
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class SecuredActionProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (roundEnv.processingOver()) {
			return false;
		}

		final Map<String,Set<String>> actions = new HashMap<String, Set<String>>();

		for (Element element : roundEnv.getElementsAnnotatedWith(SecuredAction.class)) {
			SecuredAction annotation = element.getAnnotation(SecuredAction.class);
			TypeElement clazz = (TypeElement) element.getEnclosingElement();
			if(annotation == null || !isMethod(element) || clazz == null) {
				continue;
			}

			// TODO check right method type else compile error ?
			Set<String> controllerActions = actions.get(clazz.getQualifiedName());
			if (controllerActions == null) {
				controllerActions = new TreeSet<>();
				actions.put(clazz.getQualifiedName().toString(), controllerActions);
			}
			controllerActions.add("{ \"name\" : \"" +  element.getSimpleName().toString() +
					"\", \"displayName\" : \"" + annotation.value() + "\"}");
		}

		Filer filer = processingEnv.getFiler();
		for (Map.Entry<String,Set<String>> e : actions.entrySet()) {
			try {
				String controller = e.getKey();
				FileObject f = filer.getResource(StandardLocation.CLASS_OUTPUT, "",
						"SecuredAction-" + controller + ".json");
				BufferedReader r = new BufferedReader(new InputStreamReader(f.openInputStream(), "UTF-8"));
				String line;
				while((line = r.readLine()) != null) {
					e.getValue().add(line);
				}
				r.close();
			} catch (FileNotFoundException x) {
				// doesn't exist
			} catch (IOException ex) {
				error("Failed to load existing secured actions : " + ex);
			}
		}

		for (Map.Entry<String,Set<String>> e : actions.entrySet()) {
			try {
				String path = "SecuredAction-" + e.getKey() + ".json";
				processingEnv.getMessager().printMessage(Kind.NOTE,"Writing "+ path);
				FileObject f = filer.createResource(StandardLocation.CLASS_OUTPUT, "", path);
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(f.openOutputStream(), "UTF-8"));
				for (String value : e.getValue()) {
					pw.println(value);
				}
				pw.close();
			} catch (IOException ex) {
				error("Failed to write secured actions : " + ex);
			}
		}
		return false;
	}

	private boolean isMethod(Element element) {
		return ((element != null) && ElementKind.METHOD.equals(element.getKind()));
	}

	private void error(String message) {
		processingEnv.getMessager().printMessage(Kind.ERROR, message);
	}

}

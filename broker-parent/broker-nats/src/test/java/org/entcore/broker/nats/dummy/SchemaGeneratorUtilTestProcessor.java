package org.entcore.broker.nats.dummy;

import org.entcore.broker.nats.utils.SchemaGeneratorUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

public class SchemaGeneratorUtilTestProcessor extends AbstractProcessor {
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getRootElements()) {
      if (element instanceof TypeElement) {
        TypeMirror typeMirror = element.asType();
        SchemaGeneratorUtil util = new SchemaGeneratorUtil();
        util.generateJsonSchemaFromTypeMirror(typeMirror);
      }
    }
    return false;
  }
}
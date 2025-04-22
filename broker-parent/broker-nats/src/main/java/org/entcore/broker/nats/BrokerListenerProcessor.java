package org.entcore.broker.nats;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.entcore.broker.api.BrokerListener;
import org.entcore.broker.nats.model.NATSContract;
import org.entcore.broker.nats.model.NATSEndpoint;
import org.entcore.broker.nats.utils.SchemaGeneratorUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SupportedAnnotationTypes("org.entcore.broker.api.BrokerListener")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class BrokerListenerProcessor extends AbstractProcessor {

  private SchemaGeneratorUtil schemaGeneratorUtil;
  private Filer filer;
  private Messager messager;
  private ObjectMapper objectMapper;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.schemaGeneratorUtil = new SchemaGeneratorUtil();
    this.filer = processingEnv.getFiler();
    this.messager = processingEnv.getMessager();

    // Initialize JSON utils
    this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    messager.printMessage(Diagnostic.Kind.NOTE, "Processing @BrokerListener annotations...");
    if (roundEnv.processingOver()) {
      return false;
    }

    NATSContract contract = new NATSContract();
    contract.setServiceName(getServiceName());
    contract.setVersion("1.0.0"); // Could be read from a project property

    // Process all methods annotated with @BrokerListener
    for (Element element : roundEnv.getElementsAnnotatedWith(BrokerListener.class)) {
      if (element instanceof ExecutableElement) {
        ExecutableElement method = (ExecutableElement) element;
        BrokerListener annotation = method.getAnnotation(BrokerListener.class);

        // Create an endpoint for this method
        NATSEndpoint endpoint = processMethod(method, annotation);
        contract.getEndpoints().add(endpoint);
      }
    }

    // Generate and write the nats.json file
    try {
      writeContractFile(contract);
    } catch (IOException e) {
      e.printStackTrace();
      messager.printMessage(Diagnostic.Kind.ERROR,
        "Error writing nats.json: " + e.getMessage());
    }

    return true;
  }

  private NATSEndpoint processMethod(ExecutableElement method, BrokerListener annotation) {
    NATSEndpoint endpoint = new NATSEndpoint();

    endpoint.setSubject(annotation.subject());
    endpoint.setDescription(annotation.description());
    endpoint.setProxy(annotation.proxy());

    // Get method information
    endpoint.setMethodName(method.getSimpleName().toString());
    endpoint.setClassName(((TypeElement) method.getEnclosingElement()).getQualifiedName().toString());

    final List<? extends VariableElement> parameters = method.getParameters();
    // Get request type information (first parameter)
    if (parameters.size() == 1) {
      TypeMirror requestType = method.getParameters().get(0).asType();
      endpoint.setRequestType(requestType.toString());
      try {
        endpoint.setRequestSchema(schemaGeneratorUtil.generateJsonSchemaFromTypeMirror(requestType));
      } catch (Exception e) {
        e.printStackTrace();
        messager.printMessage(Diagnostic.Kind.WARNING,
          "Could not generate schema for request type: " + e.getMessage(), method);
      }
    } else {
      messager
        .printMessage(Diagnostic.Kind.ERROR, "Could not generate schema for request type: method " +  method + " should have 1 and only 1 parameter");
      throw new RuntimeException("Should have only 1 parameter but got " + parameters.size() + " : " + parameters);
    }

    // Get response type information
    TypeMirror responseType = getTargetType(method.getReturnType());
    final String responseTypeClass = responseType.toString();
    if(! "void".equalsIgnoreCase(responseTypeClass)) {
      endpoint.setResponseType(responseType.toString());
      try {
        endpoint.setResponseSchema(schemaGeneratorUtil.generateJsonSchemaFromTypeMirror(responseType));
      } catch (Exception e) {
        messager.printMessage(Diagnostic.Kind.WARNING,
          "Could not generate schema for response type: " + e.getMessage(), method);
      }
    }

    return endpoint;
  }

  private TypeMirror getTargetType(TypeMirror returnType) {
    final String typeMirrorString = returnType.toString();
    if(returnType instanceof DeclaredType && typeMirrorString.startsWith("io.vertx.core.Future")) {
      List<? extends TypeMirror> typeArguments = ((DeclaredType) returnType).getTypeArguments();
      if (typeArguments != null && !typeArguments.isEmpty()) {
        return typeArguments.get(0);
      }
      return returnType;
    } else {
      return returnType;
    }
  }


  private void writeContractFile(NATSContract contract) throws IOException {
    FileObject file = filer.createResource(
      StandardLocation.CLASS_OUTPUT, "", "META-INF/nats.json");

    try (Writer writer = file.openWriter()) {
      objectMapper.writeValue(writer, contract);
    }
  }

  private String getServiceName() {
    // Get the artifactId from processor options
    final Map<String, String> options = processingEnv.getOptions();
    final String artifactId = options.get("artifactId");
    return artifactId != null ? artifactId : "ent-nats-service"; // Fallback to default
  }
}
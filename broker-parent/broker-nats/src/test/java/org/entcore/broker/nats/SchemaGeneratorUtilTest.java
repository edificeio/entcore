package org.entcore.broker.nats;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.entcore.broker.nats.dummy.SchemaGeneratorUtilTestProcessor;
import org.junit.Test;

import javax.tools.JavaFileObject;

public class SchemaGeneratorUtilTest {
  @Test
  public void testSchemaGenerationWithCompileTesting() {
    JavaFileObject file = JavaFileObjects.forSourceString(
      "org.entcore.broker.nats.dummy.AllTypeClass",
      "package org.entcore.broker.nats.dummy; public class AllTypeClass { int x; }"
    );

    Compiler.javac()
      .withProcessors(new SchemaGeneratorUtilTestProcessor())
      .compile(file);
  }
}

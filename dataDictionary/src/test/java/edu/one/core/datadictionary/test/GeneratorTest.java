package edu.one.core.datadictionary.test;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.datadictionary.dictionary.Dictionary;
import edu.one.core.datadictionary.generation.ChargeEnseignementGenerator;
import edu.one.core.datadictionary.generation.DisplayNameGenerator;
import edu.one.core.datadictionary.generation.Generator;
import edu.one.core.datadictionary.generation.IdGenerator;
import edu.one.core.datadictionary.generation.LoginGenerator;
import edu.one.core.datadictionary.generation.PasswordGenerator;
import edu.one.core.datadictionary.generation.SexGenerator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.testtools.TestVerticle;
import static org.vertx.testtools.VertxAssert.*;

public class GeneratorTest extends TestVerticle {

	Dictionary d;
	Logger logger = LoggerFactory.getLogger(GeneratorTest.class);

	@Test
	public void simpleGeneration() throws Exception {
		assertEquals(6, new PasswordGenerator().generate().length());
		assertEquals(23, new IdGenerator().generate().length());
		assertEquals("H", new SexGenerator().generate("M"));
		assertEquals("O", new ChargeEnseignementGenerator().generate("ENSEIGNANT"));
		assertEquals("bob.eponge", new LoginGenerator().generate("Bob", "Éponge"));
		assertEquals("Bob Éponge", new DisplayNameGenerator().generate("Bob", "Éponge"));
		testComplete();
	}

	@Test
	public void batchGeneration() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");
		Map<String,List<String>> attrs = new HashMap<>();

		attrs.put("ENTPersonCivilite", Arrays.asList("M"));
		attrs.put("ENTPersonSexe", null);
		attrs.put("ENTPersonPrenom", Arrays.asList("Bob"));
		attrs.put("ENTPersonNom", Arrays.asList("L'Éponge"));
		attrs.put("ENTPersonNomAffichage", null);
		attrs.put("ENTPersonProfils", Arrays.asList("ENSEIGNANT"));
		attrs.put("ENTPersonChargeEnseignement", null);
		attrs.put("ENTPersonIdentifiant", null);
		attrs = d.generateField(attrs);
		
		assertEquals("H", attrs.get("ENTPersonSexe").get(0));
		assertEquals("Bob L'Éponge", attrs.get("ENTPersonNomAffichage").get(0));
		assertEquals("O", attrs.get("ENTPersonChargeEnseignement").get(0));
		testComplete();
	}
}
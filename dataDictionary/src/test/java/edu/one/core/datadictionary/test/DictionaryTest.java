package edu.one.core.datadictionary.test;

import edu.one.core.datadictionary.dictionary.DefaultDictionary;
import edu.one.core.datadictionary.dictionary.Dictionary;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;
import org.vertx.testtools.TestVerticle;
import static org.vertx.testtools.VertxAssert.*;

public class DictionaryTest extends TestVerticle {

	Dictionary d;
	Logger logger = LoggerFactory.getLogger(DictionaryTest.class);

	@Test
	public void constructDictionary() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");
		assertEquals("ENTPersonCodePostal", d.getField("ENTPersonCodePostal").getName());
		testComplete();
	}

	@Test
	public void simpleValidate() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");
		assertEquals(true, d.validateField("ENTPersonCodePostal", strToList("75018")));
		assertEquals(false, d.validateField("ENTPersonCodePostal", strToList("12AAA")));
		assertEquals(true, d.validateField("ENTPersonTelPerso", strToList("0100000000")));
		assertEquals(false, d.validateField("ENTPersonTelPerso", strToList("33")));
		testComplete();
	}

	@Test
	public void multipleValidate() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");

		Map<String,List<String>> map = new HashMap<>();
		map.put("ENTPersonCodePostal", strToList("75018"));
		map.put("ENTPersonTelPerso", strToList("0100000000"));
		assertArrayEquals(new Boolean[]{true, true}, d.validateFields(map).values().toArray());

		// Same keys. So it's overwrite values
		map.put("ENTPersonCodePostal", strToList("12AAA"));
		map.put("ENTPersonTelPerso", strToList("33"));
		assertArrayEquals(new Boolean[]{false, false}, d.validateFields(map).values().toArray());

		testComplete();
	}
	
	// TODO : mettre cette fonction dans infra (utilisée aussi dans sync)
	public static List<String> strToList(String chaine) {
		String[] array = {chaine};
		return Arrays.asList(array);
	}
}
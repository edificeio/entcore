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

public class DictionaryValidationTest extends TestVerticle {

	Dictionary d;
	Logger logger = LoggerFactory.getLogger(DictionaryValidationTest.class);

	@Test
	public void constructDictionary() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");
		assertEquals("ENTPersonCodePostal", d.getField("ENTPersonCodePostal").getId());
		testComplete();
	}

	@Test
	public void simpleValue() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");
		assertEquals(true, d.validateField("ENTPersonCodePostal", "75018"));
		assertEquals(false, d.validateField("ENTPersonCodePostal", "12AAA"));
		assertEquals(true, d.validateField("ENTPersonTelPerso", "0100000000"));
		assertEquals(false, d.validateField("ENTPersonTelPerso", "33"));
		testComplete();
	}

	@Test
	public void multipleFieldSimpleValue() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");

		Map<String,String> map = new HashMap<>();
		map.put("ENTPersonCodePostal", "75018");
		map.put("ENTPersonTelPerso", "0100000000");
		assertArrayEquals(new Boolean[]{true, true}, d.validateFields(map).values().toArray());

		// Same keys. So it's overwrite values
		map.put("ENTPersonCodePostal", "12AAA");
		map.put("ENTPersonTelPerso", "33");
		assertArrayEquals(new Boolean[]{false, false}, d.validateFields(map).values().toArray());

		testComplete();
	}

	@Test
	public void multipleValue() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");
		assertEquals(true, d.validateField("ENTPersonTelPerso", Arrays.asList(new String[]{"0100000000", "0100000002"})));
		assertEquals(false, d.validateField("ENTPersonTelPerso", Arrays.asList(new String[]{"a", "0100000002"})));
		testComplete();
	}

	@Test
	public void multipleFieldMultipleValue() throws Exception {
		d = new DefaultDictionary(vertx, container, "./src/main/resources/aaf-dictionary.json");
		Map<String,List<String>> map = new HashMap<>();
		map.put("ENTPersonCodePostal", Arrays.asList(new String[]{"34000", "75000"}));
		map.put("ENTPersonTelPerso", Arrays.asList(new String[]{"0100000000", "0100000002"}));
		assertArrayEquals(new Boolean[]{true, true}, d.validateFieldsList(map).values().toArray());

		// Same keys. So it's overwrite values
		map.put("ENTPersonCodePostal", Arrays.asList(new String[]{"34000", "75aaa"}));
		map.put("ENTPersonTelPerso", Arrays.asList(new String[]{"0100000000", "z"}));
		assertArrayEquals(new Boolean[]{false, false}, d.validateFieldsList(map).values().toArray());

		testComplete();
	}
}
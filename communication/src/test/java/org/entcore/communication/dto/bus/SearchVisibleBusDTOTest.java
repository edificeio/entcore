package org.entcore.communication.dto.bus;

import io.vertx.core.json.JsonObject;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SearchVisibleBusDTOTest {

	@Test
	public void testReadsFlagsWithBusKeys() {
		// keys as sent by UserUtils.findVisibles / findVisibleUsers
		final SearchVisibleBusDTO dto = new SearchVisibleBusDTO(new JsonObject()
				.put("action", "visibleUsers")
				.put("itself", true)
				.put("mygroup", true));
		assertTrue(dto.isItself());
		assertTrue(dto.isMygroup());
	}

	@Test
	public void testFlagsDefaultToFalse() {
		final SearchVisibleBusDTO dto = new SearchVisibleBusDTO(new JsonObject().put("action", "visibleUsers"));
		assertFalse(dto.isItself());
		assertFalse(dto.isMygroup());
	}
}

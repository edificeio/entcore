package org.entcore.feeder.utils;

import io.vertx.core.json.JsonArray;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AAFUploadDirectoriesTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private Path root;

	@Before
	public void setUp() throws IOException {
		root = folder.newFolder("import").toPath();
	}

	private Path uploaded(final String name) throws IOException {
		final Path directory = Files.createDirectory(root.resolve(name));
		Files.write(directory.resolve("ENT_Eleve_0001.xml"), "<xml/>".getBytes(StandardCharsets.UTF_8));
		Files.write(directory.resolve(AAFUploadDirectories.UPLOAD_MARKER), new byte[0]);
		return directory;
	}

	private Path academy(final String name) throws IOException {
		final Path directory = Files.createDirectory(root.resolve(name));
		Files.write(directory.resolve("ENT_Eleve_0001.xml"), "<xml/>".getBytes(StandardCharsets.UTF_8));
		return directory;
	}

	private void listDirectories(final String... names) throws IOException {
		Files.write(root.resolve(AAFUploadDirectories.IMPORT_DIRECTORIES_JSON),
				new JsonArray(Arrays.asList(names)).encode().getBytes(StandardCharsets.UTF_8));
	}

	private List<Object> listedDirectories() throws IOException {
		return new JsonArray(new String(Files.readAllBytes(root.resolve(AAFUploadDirectories.IMPORT_DIRECTORIES_JSON)),
				StandardCharsets.UTF_8)).getList();
	}

	private void assertRefused(final int expectedStatus, final String subPath) throws IOException {
		try {
			AAFUploadDirectories.clean(root, subPath);
			fail("Cleaning '" + subPath + "' should have been refused");
		} catch (AAFUploadDirectories.RefusedException e) {
			assertEquals("status for '" + subPath + "'", expectedStatus, e.getStatusCode());
		}
	}

	@Test
	public void cleanRemovesTheUploadedDirectoryAndItsListing() throws Exception {
		final Path kept = uploaded("keep");
		final Path removed = uploaded("remove");
		listDirectories("keep", "remove");
		assertEquals(Collections.singletonList("remove"), AAFUploadDirectories.clean(root, "remove"));
		assertFalse(Files.exists(removed));
		assertTrue(Files.exists(kept));
		assertEquals(Collections.singletonList("keep"), listedDirectories());
	}

	@Test
	public void cleanAllRemovesOnlyUploadedDirectories() throws Exception {
		uploaded("test");
		uploaded("test-big");
		final Path fromAcademy = academy("ACADEMY");
		listDirectories("test", "test-big", "ACADEMY");
		final List<String> removed = AAFUploadDirectories.clean(root, null);
		assertEquals(Arrays.asList("test", "test-big"), removed.stream().sorted().collect(java.util.stream.Collectors.toList()));
		assertTrue("the directory of an academy must never be removed", Files.exists(fromAcademy));
		assertEquals(Collections.singletonList("ACADEMY"), listedDirectories());
		assertTrue(Files.exists(root));
	}

	@Test
	public void cleanIsIdempotent() throws Exception {
		assertTrue(AAFUploadDirectories.clean(root, "absent").isEmpty());
		assertTrue(AAFUploadDirectories.clean(root, null).isEmpty());
	}

	@Test
	public void cleanRefusesADirectoryWhichWasNotUploaded() throws Exception {
		final Path fromAcademy = academy("ACADEMY");
		listDirectories("ACADEMY");
		assertRefused(403, "ACADEMY");
		assertTrue(Files.exists(fromAcademy));
		assertEquals(Collections.singletonList("ACADEMY"), listedDirectories());
	}

	@Test
	public void cleanRefusesUnacceptableSubPaths() throws Exception {
		uploaded("test");
		final StringBuffer sbuffer = new StringBuffer();
		for(int i = 0; i < 101; i++) {
			sbuffer.append('x');
		}
		final String xs = sbuffer.toString();
		for (String subPath : new String[]{"", ".", "..", "../import", "../../etc", "test/sub", "/test", "/tmp", "~", "te st",
				"test;rm", "test\0", "a/../b", "%2e%2e", "C:\\test", xs}) {
			assertRefused(400, subPath);
		}
		assertTrue(Files.exists(root.resolve("test")));
	}

	@Test
	public void cleanNeverFollowsASymbolicLink() throws Exception {
		final Path outside = folder.newFolder("outside").toPath();
		Files.write(outside.resolve("precious.txt"), "data".getBytes(StandardCharsets.UTF_8));
		Files.write(outside.resolve(AAFUploadDirectories.UPLOAD_MARKER), new byte[0]);
		Files.createSymbolicLink(root.resolve("link"), outside);
		assertRefused(403, "link");
		assertTrue(AAFUploadDirectories.clean(root, null).isEmpty());
		assertTrue(Files.exists(outside.resolve("precious.txt")));
	}

	@Test
	public void cleanRemovesALinkInsideAnUploadedDirectoryWithoutFollowingIt() throws Exception {
		final Path outside = folder.newFolder("outside").toPath();
		Files.write(outside.resolve("precious.txt"), "data".getBytes(StandardCharsets.UTF_8));
		final Path directory = uploaded("test");
		Files.createSymbolicLink(directory.resolve("inner"), outside);
		assertEquals(Collections.singletonList("test"), AAFUploadDirectories.clean(root, "test"));
		assertFalse(Files.exists(directory));
		assertTrue(Files.exists(outside.resolve("precious.txt")));
	}

	@Test
	public void cleanDoesNothingWhenTheRootDoesNotExist() throws Exception {
		assertTrue(AAFUploadDirectories.clean(root.resolve("missing"), "test").isEmpty());
	}

	@Test
	public void subPathAcceptance() {
		assertTrue(AAFUploadDirectories.isCleanableSubPath("it-ENABLING-1377_1234"));
		assertFalse(AAFUploadDirectories.isCleanableSubPath(null));
		assertFalse(AAFUploadDirectories.isCleanableSubPath("a.b"));
	}
}

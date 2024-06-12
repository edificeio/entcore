package org.entcore.common.notification;

import static org.entcore.common.notification.NotificationUtils.jsonContentToPreview;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import static org.entcore.common.notification.NotificationUtils.camelCaseToKebabCaseKeys;

public class NotificationUtilsTest {
    @Test
    public void testKebabCase() {
        assertNull(camelCaseToKebabCaseKeys(null));
        assertEquals(0, camelCaseToKebabCaseKeys(new JsonObject()).size());
        final JsonObject kebab = camelCaseToKebabCaseKeys(new JsonObject()
          .put("patatiPatata", 0)
          .put("KEY", 4)
          .put("toto", 5)
          .put("videoResolutionFrames", 1)
          .put("already-kebab", 2));
        assertEquals(0, (int)kebab.getInteger("patati-patata"));
        assertEquals(4, (int)kebab.getInteger("KEY"));
        assertEquals(5, (int)kebab.getInteger("toto"));
        assertEquals(1, (int)kebab.getInteger("video-resolution-frames"));
        assertEquals(2, (int)kebab.getInteger("already-kebab"));
    }

    @Test
    public void testJsonContentToPreviewFullPost() {
        final JsonObject preview = jsonContentToPreview(FULL_CONTENT);
        assertEquals("Coucou voici un paragraphe.Et un deuxièmeTitreCol 1Col 2Row 1 et un petit texteContenuContenu 2Row 2Contenu 3Contenu 4ImageEt avec un peu de modifica…", preview.getString("text"));
        final JsonArray images = preview.getJsonArray("images");
        assertEquals(1, images.size());
        assertEquals("/workspace/document/ebfba26d-f195-42d3-be4c-d15f0f1b838b", images.getString(0));

        final JsonArray medias = preview.getJsonArray("medias");
        assertEquals(6, medias.size());
        // Image
        assertMediaProps(medias.getJsonObject(0), "image", "/workspace/document/ebfba26d-f195-42d3-be4c-d15f0f1b838b");
        assertMediaProps(medias.getJsonObject(1), "iframe", "https://www.youtube.com/embed/iENAm60rSbA?si=f5BfWYQ9OLdlHuqu");
        assertMediaProps(medias.getJsonObject(2), "audio", "/workspace/document/05a07fda-0efb-4d62-a951-f7c898d872a6");
        assertMediaProps(medias.getJsonObject(3), "attachment", "/workspace/document/45132d8a-51dd-4e45-a2de-2ef2adf73e74");
        assertMediaProps(medias.getJsonObject(4), "attachment", "/workspace/document/ba9d25b8-d7d7-4ca3-a850-0fb808641d9a");
        assertMediaProps(medias.getJsonObject(5), "video", "/workspace/document/8a1d14b4-2943-4680-83e6-d74f380a57b8",
          "document-id", "8a1d14b4-2943-4680-83e6-d74f380a57b8", "width", "350", "height", "197", "document-is-captation", "true",
          "video-resolution", "350x197");
    }

    private static void assertMediaProps(final JsonObject media, final String type, final String source, final String... extraProps) {
        assertEquals("wrong type for media : " + media.encodePrettily(), type, media.getString("type"));
        assertEquals("wrong source for media : " + media.encodePrettily(), source, media.getString("src"));
        if(extraProps != null) {
            for(int i = 0; i < extraProps.length - 1; i++) {
                final String key = extraProps[i++];
                final String expectedValue = extraProps[i];
                assertEquals("wrong " + key + " for media :" + media.encodePrettily(), expectedValue, media.getString(key));
            }
        }
    }

    private static final JsonObject FULL_CONTENT = new JsonObject("{\n" +
      "\t\"jsonContent\": {\n" +
      "\t\t\"type\": \"doc\",\n" +
      "\t\t\"content\": [\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"text\": \"Coucou voici un paragraphe.\"\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"text\": \"Et un deuxième\"\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"table\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"template\": null\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"tableRow\",\n" +
      "\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableHeader\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Titre\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableHeader\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Col 1\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableHeader\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t271\n" +
      "\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Col 2\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t},\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"tableRow\",\n" +
      "\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableCell\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"backgroundColor\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"data-text-align\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Row 1 et un petit texte\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableCell\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"backgroundColor\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"data-text-align\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Contenu\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableCell\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t271\n" +
      "\t\t\t\t\t\t\t\t\t],\n" +
      "\t\t\t\t\t\t\t\t\t\"backgroundColor\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"data-text-align\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Contenu 2\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t},\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"tableRow\",\n" +
      "\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableCell\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"backgroundColor\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"data-text-align\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Row 2\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableCell\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"backgroundColor\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"data-text-align\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Contenu 3\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"tableCell\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"colspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"rowspan\": 1,\n" +
      "\t\t\t\t\t\t\t\t\t\"colwidth\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t271\n" +
      "\t\t\t\t\t\t\t\t\t],\n" +
      "\t\t\t\t\t\t\t\t\t\"backgroundColor\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"data-text-align\": null\n" +
      "\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\t\t\t\t\t\t\"text\": \"Contenu 4\"\n" +
      "\t\t\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t]\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"text\": \"Image\"\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"custom-image\",\n" +
      "\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\"textAlign\": \"left\",\n" +
      "\t\t\t\t\t\t\t\"src\": \"/workspace/document/ebfba26d-f195-42d3-be4c-d15f0f1b838b\",\n" +
      "\t\t\t\t\t\t\t\"alt\": null,\n" +
      "\t\t\t\t\t\t\t\"title\": null,\n" +
      "\t\t\t\t\t\t\t\"size\": \"medium\",\n" +
      "\t\t\t\t\t\t\t\"width\": \"350\",\n" +
      "\t\t\t\t\t\t\t\"height\": null,\n" +
      "\t\t\t\t\t\t\t\"style\": null\n" +
      "\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"text\": \"Et avec un \"\n" +
      "\t\t\t\t\t},\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"marks\": [\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"textStyle\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"color\": \"rgb(255, 58, 85)\",\n" +
      "\t\t\t\t\t\t\t\t\t\"fontSize\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"lineHeight\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"fontFamily\": null\n" +
      "\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t],\n" +
      "\t\t\t\t\t\t\"text\": \"peu de modific\"\n" +
      "\t\t\t\t\t},\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"text\": \"ation de \"\n" +
      "\t\t\t\t\t},\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"marks\": [\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"bold\"\n" +
      "\t\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"italic\"\n" +
      "\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t],\n" +
      "\t\t\t\t\t\t\"text\": \"style.\"\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"marks\": [\n" +
      "\t\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\t\"type\": \"hyperlink\",\n" +
      "\t\t\t\t\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\t\t\t\t\"href\": \"https://www.youtube.com/watch?v=iENAm60rSbA\",\n" +
      "\t\t\t\t\t\t\t\t\t\"target\": \"_blank\",\n" +
      "\t\t\t\t\t\t\t\t\t\"rel\": \"noopener noreferrer nofollow\",\n" +
      "\t\t\t\t\t\t\t\t\t\"class\": null,\n" +
      "\t\t\t\t\t\t\t\t\t\"title\": \"\"\n" +
      "\t\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t\t],\n" +
      "\t\t\t\t\t\t\"text\": \"DJ et ça c'est un texte à rallonge\"\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"iframe\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"src\": \"https://www.youtube.com/embed/iENAm60rSbA?si=f5BfWYQ9OLdlHuqu\",\n" +
      "\t\t\t\t\t\"frameborder\": 0,\n" +
      "\t\t\t\t\t\"allowfullscreen\": true,\n" +
      "\t\t\t\t\t\"width\": \"560\",\n" +
      "\t\t\t\t\t\"height\": \"315\",\n" +
      "\t\t\t\t\t\"style\": null\n" +
      "\t\t\t\t}\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t}\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"audio\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"src\": \"/workspace/document/05a07fda-0efb-4d62-a951-f7c898d872a6\",\n" +
      "\t\t\t\t\t\"documentId\": \"05a07fda-0efb-4d62-a951-f7c898d872a6\"\n" +
      "\t\t\t\t}\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t},\n" +
      "\t\t\t\t\"content\": [\n" +
      "\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\"type\": \"text\",\n" +
      "\t\t\t\t\t\t\"text\": \".\"\n" +
      "\t\t\t\t\t}\n" +
      "\t\t\t\t]\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"attachments\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"links\": [\n" +
      "\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\"href\": \"/workspace/document/45132d8a-51dd-4e45-a2de-2ef2adf73e74\",\n" +
      "\t\t\t\t\t\t\t\"name\": \"images.jpeg\\n            \",\n" +
      "\t\t\t\t\t\t\t\"documentId\": \"45132d8a-51dd-4e45-a2de-2ef2adf73e74\",\n" +
      "\t\t\t\t\t\t\t\"dataContentType\": null\n" +
      "\t\t\t\t\t\t},\n" +
      "\t\t\t\t\t\t{\n" +
      "\t\t\t\t\t\t\t\"href\": \"/workspace/document/ba9d25b8-d7d7-4ca3-a850-0fb808641d9a\",\n" +
      "\t\t\t\t\t\t\t\"name\": \"file (13).png\\n            \",\n" +
      "\t\t\t\t\t\t\t\"documentId\": \"ba9d25b8-d7d7-4ca3-a850-0fb808641d9a\",\n" +
      "\t\t\t\t\t\t\t\"dataContentType\": null\n" +
      "\t\t\t\t\t\t}\n" +
      "\t\t\t\t\t]\n" +
      "\t\t\t\t}\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"paragraph\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\"\n" +
      "\t\t\t\t}\n" +
      "\t\t\t},\n" +
      "\t\t\t{\n" +
      "\t\t\t\t\"type\": \"video\",\n" +
      "\t\t\t\t\"attrs\": {\n" +
      "\t\t\t\t\t\"textAlign\": \"left\",\n" +
      "\t\t\t\t\t\"src\": \"/workspace/document/8a1d14b4-2943-4680-83e6-d74f380a57b8\",\n" +
      "\t\t\t\t\t\"controls\": \"true\",\n" +
      "\t\t\t\t\t\"documentId\": \"8a1d14b4-2943-4680-83e6-d74f380a57b8\",\n" +
      "\t\t\t\t\t\"isCaptation\": \"true\",\n" +
      "\t\t\t\t\t\"videoResolution\": \"350x197\",\n" +
      "\t\t\t\t\t\"width\": \"350\",\n" +
      "\t\t\t\t\t\"height\": \"197\"\n" +
      "\t\t\t\t}\n" +
      "\t\t\t}\n" +
      "\t\t]\n" +
      "\t}\n" +
      "}").getJsonObject("jsonContent");
}

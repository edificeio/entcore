package org.entcore.common.events.impl;

import io.vertx.core.json.JsonObject;
import org.entcore.common.editor.ContentTransformerEventRecorder;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ContentTransformerEventRecorderTest {


  @Test
  public void testComputeCounters() {
    final JsonObject content = new JsonObject(data).getJsonObject("jsonContent");
    final Map<String, Integer> occurrences = ContentTransformerEventRecorder.computeMultimediaOccurrences(content);
    assertEquals(5, (int)occurrences.get("nb_images"));
    assertEquals(4, (int)occurrences.get("nb_videos"));
    assertEquals(4, (int)occurrences.get("nb_sounds"));
    assertEquals(1, (int)occurrences.get("nb_embedded"));
    assertEquals(4, (int)occurrences.get("nb_attachments"));
    assertEquals(1, (int)occurrences.get("nb_external_links"));
    assertEquals(2, (int)occurrences.get("nb_internal_links"));
    assertEquals(1, (int)occurrences.get("nb_formulae"));
  }

  @Test
  public void testComputeCounters2() {
    final JsonObject content = new JsonObject(data2).getJsonObject("jsonContent");
    final Map<String, Integer> occurrences = ContentTransformerEventRecorder.computeMultimediaOccurrences(content);
    assertEquals(1, (int)occurrences.get("nb_external_links"));
    assertEquals(3, (int)occurrences.get("nb_internal_links"));
  }

  public static final String data2 = "{\n" +
    "  \"jsonContent\": {\n" +
    "    \"type\": \"doc\",\n" +
    "    \"content\": [\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"text\",\n" +
    "            \"text\": \"3 Liens internes\"\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"linker\",\n" +
    "            \"attrs\": {\n" +
    "              \"href\": \"/blog/id/5d42bb33-79ad-442d-994e-e31496280a42/post/c66a8faa-8d3a-4299-9edc-602485f18230\",\n" +
    "              \"class\": null,\n" +
    "              \"target\": null,\n" +
    "              \"title\": \"Projet en cours [Blog public + favori]\",\n" +
    "              \"data-id\": \"5d42bb33-79ad-442d-994e-e31496280a42#c66a8faa-8d3a-4299-9edc-602485f18230\",\n" +
    "              \"data-app-prefix\": \"blog\"\n" +
    "            },\n" +
    "            \"content\": [\n" +
    "              {\n" +
    "                \"type\": \"text\",\n" +
    "                \"text\": \"Projet en cours [Blog public + favori]\"\n" +
    "              }\n" +
    "            ]\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"linker\",\n" +
    "            \"attrs\": {\n" +
    "              \"href\": \"/blog/id/1cb7a7dc-1072-45d5-933c-00e28fffacde/post/15d29ce5-61fa-4456-b1c1-cbd060f39c9b\",\n" +
    "              \"class\": null,\n" +
    "              \"target\": null,\n" +
    "              \"title\": \"Android S22 chrome Toolbar non scrollable ! [Test de créa de blog 2]\",\n" +
    "              \"data-id\": \"1cb7a7dc-1072-45d5-933c-00e28fffacde#15d29ce5-61fa-4456-b1c1-cbd060f39c9b\",\n" +
    "              \"data-app-prefix\": \"blog\"\n" +
    "            },\n" +
    "            \"content\": [\n" +
    "              {\n" +
    "                \"type\": \"text\",\n" +
    "                \"text\": \"Android S22 chrome Toolbar non scrollable ! [Test de créa de blog 2]\"\n" +
    "              }\n" +
    "            ]\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"linker\",\n" +
    "            \"attrs\": {\n" +
    "              \"href\": \"/blog/id/1cb7a7dc-1072-45d5-933c-00e28fffacde/post/a70d3e71-d4c5-4dd5-b6f7-b2a2d99cf876\",\n" +
    "              \"class\": null,\n" +
    "              \"target\": null,\n" +
    "              \"title\": \"Responsive iOS safari  [Test de créa de blog 2]\",\n" +
    "              \"data-id\": \"1cb7a7dc-1072-45d5-933c-00e28fffacde#a70d3e71-d4c5-4dd5-b6f7-b2a2d99cf876\",\n" +
    "              \"data-app-prefix\": \"blog\"\n" +
    "            },\n" +
    "            \"content\": [\n" +
    "              {\n" +
    "                \"type\": \"text\",\n" +
    "                \"text\": \"Responsive iOS safari [Test de créa de blog 2]\"\n" +
    "              }\n" +
    "            ]\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        }\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"text\",\n" +
    "            \"text\": \"Liens externes\"\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"text\",\n" +
    "            \"marks\": [\n" +
    "              {\n" +
    "                \"type\": \"hyperlink\",\n" +
    "                \"attrs\": {\n" +
    "                  \"href\": \"https://www.youtube.com/watch?v=SLp7G8dYRlQ&list=RDUTtABhxErvw&index=10\",\n" +
    "                  \"target\": \"_blank\",\n" +
    "                  \"class\": null,\n" +
    "                  \"title\": \"\"\n" +
    "                }\n" +
    "              }\n" +
    "            ],\n" +
    "            \"text\": \"https://www.youtube.com/watch?v=SLp7G8dYRlQ&list=RDUTtABhxErvw&index=10\"\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        }\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        }\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        }\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"custom-image\",\n" +
    "            \"attrs\": {\n" +
    "              \"textAlign\": \"left\",\n" +
    "              \"src\": \"/workspace/document/b81f3d53-d021-48f8-95ed-810f8daa5ac7\",\n" +
    "              \"alt\": null,\n" +
    "              \"title\": null,\n" +
    "              \"size\": \"medium\",\n" +
    "              \"width\": \"350\",\n" +
    "              \"height\": \"NaN\",\n" +
    "              \"style\": null\n" +
    "            }\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"paragraph\",\n" +
    "        \"attrs\": {\n" +
    "          \"textAlign\": \"left\"\n" +
    "        },\n" +
    "        \"content\": [\n" +
    "          {\n" +
    "            \"type\": \"text\",\n" +
    "            \"text\": \"Yes\"\n" +
    "          }\n" +
    "        ]\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"audio\",\n" +
    "        \"attrs\": {\n" +
    "          \"src\": \"/workspace/document/1117a432-a622-4da4-9519-ec492f986c56\",\n" +
    "          \"documentId\": \"1117a432-a622-4da4-9519-ec492f986c56\"\n" +
    "        }\n" +
    "      },\n" +
    "      {\n" +
    "        \"type\": \"audio\",\n" +
    "        \"attrs\": {\n" +
    "          \"src\": \"/workspace/document/686b3f20-bce5-47a2-9425-f64c011a9889\",\n" +
    "          \"documentId\": \"686b3f20-bce5-47a2-9425-f64c011a9889\"\n" +
    "        }\n" +
    "      }\n" +
    "    ]\n" +
    "  }\n" +
    "}";

  
  public static final String data = "{" +
    "  \"jsonContent\": {" +
    "    \"type\": \"doc\"," +
    "    \"content\": [" +
    "      {" +
    "        \"type\": \"custom-image\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/982d2c2a-2b40-4f6f-bdc3-d7d9eae3aa30\"," +
    "          \"alt\": null," +
    "          \"title\": null," +
    "          \"size\": \"medium\"," +
    "          \"width\": \"80\"," +
    "          \"height\": \"NaN\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"video\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/2af1f06a-0688-47a0-8cb8-80c137d4b691\"," +
    "          \"controls\": \"true\"," +
    "          \"documentId\": \"2af1f06a-0688-47a0-8cb8-80c137d4b691\"," +
    "          \"isCaptation\": \"true\"," +
    "          \"videoResolution\": \"350x197\"," +
    "          \"width\": \"350\"," +
    "          \"height\": \"197\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"audio\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/0beb7f56-9e79-4d70-b644-eb71e8e69ca7\"," +
    "          \"documentId\": \"0beb7f56-9e79-4d70-b644-eb71e8e69ca7\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"custom-image\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/fc49393b-b18b-4e7d-b281-8e63b1f8a218\"," +
    "          \"alt\": null," +
    "          \"title\": null," +
    "          \"size\": \"medium\"," +
    "          \"width\": \"80\"," +
    "          \"height\": \"NaN\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"attachments\"," +
    "        \"attrs\": {" +
    "          \"links\": [" +
    "            {" +
    "              \"href\": \"/workspace/document/574381e1-e8d7-4262-bb5d-b941caba77db\"," +
    "              \"name\": \"undraw_Interview_re_e5jn.png\"," +
    "              \"documentId\": \"574381e1-e8d7-4262-bb5d-b941caba77db\"," +
    "              \"dataContentType\": null" +
    "            }," +
    "            {" +
    "              \"href\": \"/workspace/document/29caba75-fce3-473d-9359-de0143f78ed5\"," +
    "              \"name\": \"undraw_Sharing_articles_re_jnkp.png\"," +
    "              \"documentId\": \"29caba75-fce3-473d-9359-de0143f78ed5\"," +
    "              \"dataContentType\": null" +
    "            }" +
    "          ]" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"custom-image\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/01cfefc5-af88-41c9-8144-48f63b822754\"," +
    "          \"alt\": null," +
    "          \"title\": null," +
    "          \"size\": \"medium\"," +
    "          \"width\": \"80\"," +
    "          \"height\": \"NaN\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"custom-image\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/eb06aeff-556b-4eee-9969-defd42c86243\"," +
    "          \"alt\": null," +
    "          \"title\": null," +
    "          \"size\": \"medium\"," +
    "          \"width\": \"80\"," +
    "          \"height\": \"NaN\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"attachments\"," +
    "        \"attrs\": {" +
    "          \"links\": [" +
    "            {" +
    "              \"href\": \"/workspace/document/01cfefc5-af88-41c9-8144-48f63b822754\"," +
    "              \"name\": \"Product - earphone.jpeg\"," +
    "              \"documentId\": \"01cfefc5-af88-41c9-8144-48f63b822754\"," +
    "              \"dataContentType\": null" +
    "            }" +
    "          ]" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"attachments\"," +
    "        \"attrs\": {" +
    "          \"links\": [" +
    "            {" +
    "              \"href\": \"/workspace/document/b7099fcc-ad74-4f7a-9a01-7d19e3c1d21c\"," +
    "              \"name\": \"undraw_People_search_re_5rre.png\"," +
    "              \"documentId\": \"b7099fcc-ad74-4f7a-9a01-7d19e3c1d21c\"," +
    "              \"dataContentType\": null" +
    "            }" +
    "          ]" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"custom-image\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/ea06d933-9be1-4d01-ba67-a8b5b4228212\"," +
    "          \"alt\": null," +
    "          \"title\": null," +
    "          \"size\": \"medium\"," +
    "          \"width\": \"80\"," +
    "          \"height\": \"NaN\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"audio\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/4ffccfa5-39d3-4257-a554-7598d8ea28f5\"," +
    "          \"documentId\": \"4ffccfa5-39d3-4257-a554-7598d8ea28f5\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"audio\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/5e6ea922-fb7c-4d00-b3e7-6879221f76e6\"," +
    "          \"documentId\": \"5e6ea922-fb7c-4d00-b3e7-6879221f76e6\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"audio\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/4c8d4ddf-730c-430b-8a3e-986b7c19c21e\"," +
    "          \"documentId\": \"4c8d4ddf-730c-430b-8a3e-986b7c19c21e\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }," +
    "        \"content\": [" +
    "          {" +
    "            \"type\": \"text\"," +
    "            \"marks\": [" +
    "              {" +
    "                \"type\": \"hyperlink\"," +
    "                \"attrs\": {" +
    "                  \"href\": \"/actualites#/view/thread/71/info/237\"," +
    "                  \"target\": \"_blank\"," +
    "                  \"class\": null" +
    "                }" +
    "              }" +
    "            ]," +
    "            \"text\": \"Présentation du CDI en 2 minutes [Vie du collège]\"" +
    "          }" +
    "        ]" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }," +
    "        \"content\": [" +
    "          {" +
    "            \"type\": \"text\"," +
    "            \"marks\": [" +
    "              {" +
    "                \"type\": \"hyperlink\"," +
    "                \"attrs\": {" +
    "                  \"href\": \"/actualites#/view/thread/71/info/236\"," +
    "                  \"target\": \"_blank\"," +
    "                  \"class\": null" +
    "                }" +
    "              }" +
    "            ]," +
    "            \"text\": \"Règlement intérieur du collège [Vie du collège]\"" +
    "          }" +
    "        ]" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }," +
    "        \"content\": [" +
    "          {" +
    "            \"type\": \"text\"," +
    "            \"marks\": [" +
    "              {" +
    "                \"type\": \"hyperlink\"," +
    "                \"attrs\": {" +
    "                  \"href\": \"https://edifice-community.atlassian.net/wiki/spaces/ODE/pages/3767468033/Cadrage+technique+-+Mesure+d+audience\"," +
    "                  \"target\": \"_blank\"," +
    "                  \"class\": null," +
    "                  \"title\": \"\"" +
    "                }" +
    "              }" +
    "            ]," +
    "            \"text\": \"Lien externe\"" +
    "          }" +
    "        ]" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }," +
    "        \"content\": [" +
    "          {" +
    "            \"type\": \"text\"," +
    "            \"text\": \"$\\\\frac{-b + \\\\sqrt{b^2 - 4ac}}{2a}$\"" +
    "          }" +
    "        ]" +
    "      }," +
    "      {" +
    "        \"type\": \"iframe\"," +
    "        \"attrs\": {" +
    "          \"src\": \"https://www.youtube.com/embed/P_KOOdgKTSM?rel=0\"," +
    "          \"frameborder\": 0," +
    "          \"allowfullscreen\": true," +
    "          \"width\": \"560\"," +
    "          \"height\": \"315\"," +
    "          \"style\": null" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"paragraph\"," +
    "        \"attrs\": {" +
    "          \"textAlign\": \"left\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"video\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/400b45dd-0b83-43e5-9ab9-80319f924ea8\"," +
    "          \"controls\": \"true\"," +
    "          \"documentId\": \"400b45dd-0b83-43e5-9ab9-80319f924ea8\"," +
    "          \"isCaptation\": \"true\"," +
    "          \"videoResolution\": \"350x197\"," +
    "          \"width\": \"350\"," +
    "          \"height\": \"197\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"video\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/2a022985-e357-4b78-8a68-319954404eb7\"," +
    "          \"controls\": \"true\"," +
    "          \"documentId\": \"2a022985-e357-4b78-8a68-319954404eb7\"," +
    "          \"isCaptation\": \"true\"," +
    "          \"videoResolution\": \"350x197\"," +
    "          \"width\": \"350\"," +
    "          \"height\": \"197\"" +
    "        }" +
    "      }," +
    "      {" +
    "        \"type\": \"video\"," +
    "        \"attrs\": {" +
    "          \"src\": \"/workspace/document/b4c75fac-7d7b-4b71-b919-97667a0d5f23\"," +
    "          \"controls\": \"true\"," +
    "          \"documentId\": \"b4c75fac-7d7b-4b71-b919-97667a0d5f23\"," +
    "          \"isCaptation\": \"true\"," +
    "          \"videoResolution\": \"350x197\"," +
    "          \"width\": \"350\"," +
    "          \"height\": \"197\"" +
    "        }" +
    "      }" +
    "    ]" +
    "  }" +
    "}";
}

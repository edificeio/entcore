package org.entcore.feeder.utils;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ExternalIdValidatorTest {

    @Test
    public void testCleanKeepsValidExternalId(final TestContext context) {
        // Cas nominal : un externalId généré (sha1) n'est pas touché
        context.assertEquals("9a3f1ce773860dac7d32bd4c82e55b87ca5715f5",
                ExternalIdValidator.clean("9a3f1ce773860dac7d32bd4c82e55b87ca5715f5"));

        // Un uuid non plus : les tirets internes ne sont pas réservés
        context.assertEquals("786ed343-8865-4d34-8e01-8637be049a69",
                ExternalIdValidator.clean("786ed343-8865-4d34-8e01-8637be049a69"));

        // Caractères non réservés par le QueryParser Lucene
        context.assertEquals("ID#12.34_56;78=90@ab%cd&ef|gh<i>j'k,l",
                ExternalIdValidator.clean("ID#12.34_56;78=90@ab%cd&ef|gh<i>j'k,l"));

        // Les accents passent par l'analyzer sans être interprétés
        context.assertEquals("ELEVEéÉàçÎ", ExternalIdValidator.clean("ELEVEéÉàçÎ"));
    }

    @Test
    public void testCleanRemovesReservedChars(final TestContext context) {
        // Le cas du ticket : deux slashes suffisent à faire échouer la requête
        context.assertEquals("ABCDEFGHI", ExternalIdValidator.clean("ABC/DEF/GHI"));

        // Espaces et tabulations : l'index est un fulltext, ils découpent la valeur en tokens
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC DEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC\tDEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("  ABC   DEF  "));

        // Erreurs de syntaxe
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC\"DEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC(DEF)"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC[DEF]"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC{DEF}"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC:DEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC^DEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC!DEF"));

        // Requêtes silencieusement fausses : jokers, recherche floue, échappement
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC*DEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC?DEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC~DEF"));
        context.assertEquals("ABCDEF", ExternalIdValidator.clean("ABC\\DEF"));
    }

    @Test
    public void testCleanRemovesLeadingOperatorsOnly(final TestContext context) {
        // '-' et '+' en tête sont les opérateurs NOT et MUST
        context.assertEquals("ABC", ExternalIdValidator.clean("-ABC"));
        context.assertEquals("ABC", ExternalIdValidator.clean("+ABC"));
        context.assertEquals("ABC", ExternalIdValidator.clean("+-ABC"));

        // ailleurs ce sont des caractères de terme ordinaires, à conserver
        context.assertEquals("AB-C", ExternalIdValidator.clean("AB-C"));
        context.assertEquals("AB+C", ExternalIdValidator.clean("AB+C"));
        context.assertEquals("ABC-", ExternalIdValidator.clean("ABC-"));

        // un caractère réservé peut démasquer un opérateur en tête
        context.assertEquals("ABC", ExternalIdValidator.clean(" -ABC"));
        context.assertEquals("ABC", ExternalIdValidator.clean("/-/ABC"));
    }

    @Test
    public void testCleanHandlesEmptyValues(final TestContext context) {
        context.assertNull(ExternalIdValidator.clean(null));
        context.assertEquals("", ExternalIdValidator.clean(""));
        context.assertEquals("", ExternalIdValidator.clean("   "));
        context.assertEquals("", ExternalIdValidator.clean("///"));
    }
}

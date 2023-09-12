package org.entcore.common.utils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class SanitizerUtilsTest {
    private static Logger log = LoggerFactory.getLogger(SanitizerUtilsTest.class);


    void assertSame(final TestContext context, final String message, final String test) {
        final String cleaned = SanitizerUtils.sanitizeHtml(test);
        context.assertEquals(cleaned, test, message);
    }

    void assertCleaned(final TestContext context, final String message, final String test) {
        assertCleaned(context, message, test, "");
    }

    void assertCleaned(final TestContext context, final String message, final String test, final String cleanValue) {
        final String cleaned = SanitizerUtils.sanitizeHtml(test);
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
        assertCleaned(context, "should not keep js injection with \\0 in href", "<a href=\"java\\0script:alert('XSS')\" ></a>");
        assertCleaned(context, "should not keep js injection with \\0", "<img src=\"java\\0script:alert('XSS')\" />");
        assertCleaned(context, "should not keep js injection with meta char", "<img src=\" &#14; javascript:alert('XSS');\" />");
        assertCleaned(context, "should not keep js injection with non-alpha non-digit", "<div onload!#$%&()*~+-_.,:;?@[/|\\]^`=alert(\"XSS\")></div>", "<div></div>");
        assertCleaned(context, "should not inject using img charcode", "<img src=javascript:alert(String.fromCharCode(88,83,83))/>");
        assertCleaned(context, "should not inject using img onerror", "<img src=/ onerror=\"alert(String.fromCharCode(88,83,83))\"></img>", "<img src=\"/\" />");
        assertCleaned(context, "should not inject using img onerror encode", "<img src=x onerror=\"&#0000106&#0000097&#0000118&#0000097&#0000115&#0000099&#0000114&#0000105&#0000112&#0000116&#0000058&#0000097&#0000108&#0000101&#0000114&#0000116&#0000040&#0000039&#0000088&#0000083&#0000083&#0000039&#0000041\">", "<img src=\"x\" />");
        assertCleaned(context, "should not inject using img and html caracter", "<img src=&#106;&#97;&#118;&#97;&#115;&#99;&#114;&#105;&#112;&#116;&#58;&#97;&#108;&#101;&#114;&#116;&#40;&#39;&#88;&#83;&#83;&#39;&#41;/>");
        assertCleaned(context, "should not inject using img and hex", "<img src=&#x6A&#x61&#x76&#x61&#x73&#x63&#x72&#x69&#x70&#x74&#x3A&#x61&#x6C&#x65&#x72&#x74&#x28&#x27&#x58&#x53&#x53&#x27&#x29/>");
        assertCleaned(context, "should not inject using img and base64", "<img onload=\"eval(atob('ZG9jdW1lbnQubG9jYXRpb249Imh0dHA6Ly9saXN0ZXJuSVAvIitkb2N1bWVudC5jb29raWU='))\"/>");
        // SVG
        assertSame(context, "should keep svg elements", "<svg width=\"100\" height=\"100\" xmlns=\"http://www.w3.org/2000/svg\"><rect x=\"10\" y=\"10\" width=\"80\" height=\"80\" fill=\"blue\" stroke=\"black\" stroke-width=\"2\"></rect><circle cx=\"50\" cy=\"50\" r=\"30\" fill=\"red\" stroke=\"green\" stroke-width=\"3\"></circle><ellipse cx=\"80\" cy=\"30\" rx=\"25\" ry=\"15\" fill=\"yellow\" stroke=\"purple\" stroke-width=\"2\"></ellipse><g fill=\"none\" stroke=\"gray\" stroke-width=\"1\"></g></svg>");
        assertCleaned(context, "should clean svg script", "<svg><script></script></svg>","<svg></svg>");
        assertCleaned(context, "should clean svg event", "<svg><rect onclick=\"alert('XSS')\"></rect></svg>","<svg><rect></rect></svg>");
        // BASE64
        assertCleaned(context, "should clean base64 xss", "<img src=\"data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCI+CiAgICA8Zm9yZWlnbk9iamVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIj4KICAgICAgICA8c2NyaXB0PmFsZXJ0KGRvY3VtZW50LmRvbWFpbik7PC9zY3JpcHQ+CiAgICA8L2ZvcmVpZ25PYmplY3Q+Cjwvc3ZnPg==\">", "<img src=\"\" />");
        final String imageBase64 = "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABHNCSVQICAgIfAhkiAAAAAlwSFlzAAAApgAAAKYB3X3/OAAAABl0RVh0U29mdHdhcmUAd3d3Lmlua3NjYXBlLm9yZ5vuPBoAAANCSURBVEiJtZZPbBtFFMZ/M7ubXdtdb1xSFyeilBapySVU8h8OoFaooFSqiihIVIpQBKci6KEg9Q6H9kovIHoCIVQJJCKE1ENFjnAgcaSGC6rEnxBwA04Tx43t2FnvDAfjkNibxgHxnWb2e/u992bee7tCa00YFsffekFY&#43;nUzFtjW0LrvjRXrCDIAaPLlW0nHL0SsZtVoaF98mLrx3pdhOqLtYPHChahZcYYO7KvPFxvRl5XPp1sN3adWiD1ZAqD6XYK1b/dvE5IWryTt2udLFedwc1&#43;9kLp&#43;vbbpoDh&#43;6TklxBeAi9TL0taeWpdmZzQDry0AcO&#43;jQ12RyohqqoYoo8RDwJrU&#43;qXkjWtfi8Xxt58BdQuwQs9qC/afLwCw8tnQbqYAPsgxE1S6F3EAIXux2oQFKm0ihMsOF71dHYx&#43;f3NND68ghCu1YIoePPQN1pGRABkJ6Bus96CutRZMydTl&#43;TvuiRW1m3n0eDl0vRPcEysqdXn&#43;jsQPsrHMquGeXEaY4Yk4wxWcY5V/9scqOMOVUFthatyTy8QyqwZ&#43;kDURKoMWxNKr2EeqVKcTNOajqKoBgOE28U4tdQl5p5bwCw7BWquaZSzAPlwjlithJtp3pTImSqQRrb2Z8PHGigD4RZuNX6JYj6wj7O4TFLbCO/Mn/m8R&#43;h6rYSUb3ekokRY6f/YukArN979jcW&#43;V/S8g0eT/N3VN3kTqWbQ428m9/8k0P/1aIhF36PccEl6EhOcAUCrXKZXXWS3XKd2vc/TRBG9O5ELC17MmWubD2nKhUKZa26Ba2&#43;D3P&#43;4/MNCFwg59oWVeYhkzgN/JDR8deKBoD7Y&#43;ljEjGZ0sosXVTvbc6RHirr2reNy1OXd6pJsQ&#43;gqjk8VWFYmHrwBzW/n&#43;uMPFiRwHB2I7ih8ciHFxIkd/3Omk5tCDV1t&#43;2nNu5sxxpDFNx&#43;huNhVT3/zMDz8usXC3ddaHBj1GHj/As08fwTS7Kt1HBTmyN29vdwAw&#43;/wbwLVOJ3uAD1wi/dUH7Qei66PfyuRj4Ik9is&#43;hglfbkbfR3cnZm7chlUWLdwmprtCohX4HUtlOcQjLYCu&#43;fzGJH2QRKvP3UNz8bWk1qMxjGTOMThZ3kvgLI5AzFfo379UAAAAASUVORK5CYII&#61;\" />";
        assertSame(context, "should keep valid base64 image", imageBase64);
        assertSame(context, "should keep empty base64 image", "<img src=\"\" />");
        assertSame(context, "should keep absent src image", "<img alt=\"\" />");
        // IFRAME
        assertSame(context, "should keep allowed attributes", "<iframe height=\"1\" loading=\"lazy\" name=\"name\" src=\"/iframe.html\"></iframe>");
        assertCleaned(context, "should not keep forbidden attributes", "<iframe allow=\"*\" allowfullscreen=\"true\" allowpaymentrequest=\"true\" credentialless=\"true\" csp=\"default-src\" referrerpolicy=\"no-referrer\" sandbox=\"allow-downloads\" srcdoc=\"<html></html>\"></iframe>", "<iframe></iframe>");
    }

}

package org.entcore.common.utils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.owasp.html.ElementPolicy;
import org.owasp.html.HtmlChangeListener;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SanitizerUtils {
    private static Logger log = LoggerFactory.getLogger(SanitizerUtils.class);
    /**
     * Custom html element allowed in xss sanitizer
     */
    private static final String[] ALLOWED_CUSTOM_ELEMENTS = new String[]{"mathjax"};
    /**
     * HTML element allowed in xss sanitizer
     * https://developer.mozilla.org/en-US/docs/Web/HTML/Element
     */
    private static final String[] ALLOWED_ELEMENTS = new String[]{
            // document metadata
            //forbidden : "base", "head", "link", "meta", "style", "title", "body",
            // content sectionning
            "address", "article", "aside", "footer", "header",
            "h1", "h2", "h3", "h4", "h5", "h6", "hgroup", "main", "nav", "section", "search",
            // text content
            "blockquote", "dd", "div", "dl", "dt", "figcaption", "figure", "hr", "li", "menu", "ol", "p", "pre", "ul",
            // inline text
            "a", "abbr", "b", "bdi", "bdo", "br", "cite", "code", "data", "dfn", "em", "i", "kbd", "mark", "q", "rp", "rt", "ruby", "s", "samp", "small", "span", "strong", "sub", "sup", "time", "u", "var", "wbr",
            //image multimedia
            "area", "audio", "img", "map", "track", "video",
            // embed content
            //forbidden :  "embed", "object",
            "iframe", "picture",
            //forbidden :  "portal", "source",
            "svg",
            // math and canvas
            "math", "canvas",
            // scription
            // forbidden: "noscript", "script",
            // demarcating
            // forbidden: "del", "ins",
            // table
            "caption", "col", "colgroup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "button", "datalist", "fieldset",
            // form
            // forbiddent: "form", "input", "label", "legend", "meter", "optgroup", "option", "output", "progress", "select", "textarea",
            // interactive elements
            // forbidden: "details", "dialog", "summary",
            // webcomponents
            // forbidden: "slot","template",
            // obsolete
            // forbidden: "acronym", "big", "center", "content", "dir", "font", "frame", "frameset", "image", "marquee", "menuitem", "nobr", "noembed", "noframes", "param", "plaintext", "rb", "rtc", "shadow", "strike", "tt", "xmp"
    };
    /**
     * HTML attributes allowed in xss sanitizer
     * https://developer.mozilla.org/en-US/docs/Web/HTML/Attributes
     */
    private static final String[] ALLOW_ATTRIBUTES = new String[]{
            //forbidden "accept","accept-charset","accesskey","action",
            "align",
            // forbidden: "allow",
            "alt",
            // forbidden: "async","autocapitalize","autocomplete",
            "autoplay", "background", "bgcolor", "border",
            // forbidden "buffered","capture","charset",
            "checked", "cite", "class", "color", "cols", "colspan",
            // forbidden: "content","contenteditable","contextmenu",
            "controls", "coords",
            // forbidden: "crossorigin","csp","data","data-*","datetime","decoding","default","defer",
            "dir",
            // forbidden: "dirname",
            "disabled",
            //forbidden: "download","draggable","enctype","enterkeyhint",
            "for",
            //forbidden: "form","formaction","formenctype","formmethod","formnovalidate","formtarget","headers",
            "height",
            // forbidden: "hidden","high",
            "href", "hreflang",
            // forbidden: "http-equiv"
            "id",
            // forbidden: "integrity","intrinsicsize","inputmode","ismap","itemprop","kind",
            "label", "lang",
            // forbidden: "language",
            "loading",
            // "list","loop","low","manifest","max","maxlength","minlength","media","method","min","multiple","muted",
            "name",
            //"novalidate","open","optimum","pattern","ping","placeholder","playsinline","poster","preload",
            "readonly",
            // forbidden "referrerpolicy","rel","required","reversed",
            "role", "rows", "rowspan",
            // forbidden "sandbox","scope","scoped",
            "selected",
            // forbidden "shape","size","sizes","slot","span",
            "spellcheck", "src",
            // forbidden "srcdoc","srclang","srcset","start","step",
            "style", "summary", "tabindex", "target", "title", "translate", "type",
            // forbidden: "usemap",
            "value", "width", "wrap"
    };
    /**
     * HTML attributes allowed in xss sanitizer
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute
     */
    private static final String[] ALLOW_SVG_ATTRIBUTES = new String[]{
            "accent-height", "accumulate", "additive", "alignment-baseline", "alphabetic", "amplitude", "arabic-form", "ascent",
            "attributeName", "attributeType", "azimuth", "baseFrequency", "baseline-shift", "baseProfile", "bbox", "begin", "bias",
            "by", "calcMode", "cap-height", "class", "clip", "clipPathUnits", "clip-path", "clip-rule", "color", "color-interpolation",
            "color-interpolation-filters", "color-profile", "color-rendering", "contentScriptType", "contentStyleType", "crossorigin",
            "cursor", "cx", "cy", "d", "decelerate", "descent", "diffuseConstant", "direction", "display", "divisor", "dominant-baseline",
            "dur", "dx", "dy", "edgeMode", "elevation", "enable-background", "end", "exponent", "fill", "fill-opacity", "fill-rule", "filter",
            "filterRes", "filterUnits", "flood-color", "flood-opacity", "font-family", "font-size", "font-size-adjust", "font-stretch",
            "font-style", "font-variant", "font-weight", "format", "from", "fr", "fx", "fy", "g1", "g2", "glyph-name", "glyph-orientation-horizontal",
            "glyph-orientation-vertical", "glyphRef", "gradientTransform", "gradientUnits", "hanging", "height", "href", "hreflang",
            "horiz-adv-x", "horiz-origin-x", "id", "ideographic", "image-rendering", "in", "in2", "intercept", "k", "k1", "k2", "k3", "k4",
            "kernelMatrix", "kernelUnitLength", "kerning", "keyPoints", "keySplines", "keyTimes", "lang", "lengthAdjust", "letter-spacing",
            "lighting-color", "limitingConeAngle", "local", "marker-end", "marker-mid", "marker-start", "markerHeight", "markerUnits", "markerWidth",
            "mask", "maskContentUnits", "maskUnits", "mathematical", "max", "media", "method", "min", "mode", "name", "numOctaves",
            "offset", "opacity", "operator", "order", "orient", "orientation", "origin", "overflow", "overline-position", "overline-thickness",
            "panose-1", "paint-order", "path", "pathLength", "patternContentUnits", "patternTransform", "patternUnits",/*forbidden "ping",*/
            "pointer-events", "points", "pointsAtX", "pointsAtY", "pointsAtZ", "preserveAlpha", "preserveAspectRatio", "primitiveUnits", "r", "radius",
            // forbidden "referrerPolicy",
            "refX", "refY", "rel", "rendering-intent", "repeatCount", "repeatDur",
            // forbidden "requiredExtensions","requiredFeatures",
            "restart", "result", "rotate", "rx", "ry", "scale", "seed", "shape-rendering", "slope", "spacing", "specularConstant",
            "specularExponent", "speed", "spreadMethod", "startOffset", "stdDeviation", "stemh", "stemv", "stitchTiles", "stop-color",
            "stop-opacity", "strikethrough-position", "strikethrough-thickness", "string", "stroke", "stroke-dasharray", "stroke-dashoffset",
            "stroke-linecap", "stroke-linejoin", "stroke-miterlimit", "stroke-opacity", "stroke-width", "style", "surfaceScale", "systemLanguage",
            "tabindex", "tableValues", "target", "targetX", "targetY", "text-anchor", "text-decoration", "text-rendering", "textLength", "to", "transform",
            "transform-origin", "type", "u1", "u2", "underline-position", "underline-thickness", "unicode", "unicode-bidi", "unicode-range", "units-per-em",
            "v-alphabetic", "v-hanging", "v-ideographic", "v-mathematical", "values", "vector-effect", "version", "vert-adv-y", "vert-origin-x", "vert-origin-y",
            "viewBox", "viewTarget", "visibility", "width", "widths", "word-spacing", "writing-mode", "x", "x-height", "x1", "x2", "xChannelSelector",
            // forbidden deprecated "xlink:actuate","xlink:arcrole","xlink:href","xlink:role","xlink:show","xlink:title","xlink:type","xml:base","xml:lang","xml:space",
            "xmlns", "y", "y1", "y2", "yChannelSelector", "z", "zoomAndPan"
            // forbidden: on* attributes
    };
    /**
     * HTML attributes allowed in xss sanitizer
     * https://developer.mozilla.org/en-US/docs/Web/SVG/Element
     */
    private static final String[] ALLOW_SVG_ELEMENTS = new String[]{
            "a", "animate", "animateMotion", "animateTransform", "circle", "clipPath",
            "defs", "desc", "ellipse", "feBlend", "feColorMatrix", "feComponentTransfer", "feComposite", "feConvolveMatrix",
            "feDiffuseLighting", "feDisplacementMap", "feDistantLight", "feDropShadow", "feFlood", "feFuncA", "feFuncB", "feFuncG",
            "feFuncR", "feGaussianBlur", "feImage", "feMerge", "feMergeNode", "feMorphology", "feOffset", "fePointLight", "feSpecularLighting",
            "feSpotLight", "feTile", "feTurbulence", "filter", "foreignObject", "g", "hatch", "hatchpath", "image", "line",
            "linearGradient", "marker", "mask", "metadata", "mpath", "path", "pattern", "polygon", "polyline", "radialGradient",
            "rect",
            // forbidden "script",
            "set", "stop", "style", "svg", "switch", "symbol", "text", "textPath", "title", "tspan", "use", "view"
            // forbidden deprecated: "cursor","font","font-face","font-face-format","font-face-name","font-face-src","font-face-uri","glyph","glyphRef","hkern","missing-glyph","tref","vkern"
    };
    static final PolicyFactory policy = new HtmlPolicyBuilder().allowStandardUrlProtocols()
            .allowElements(ALLOWED_CUSTOM_ELEMENTS)
            .allowElements(ALLOWED_ELEMENTS)
            .allowAttributes(ALLOW_ATTRIBUTES).globally()
            .allowElements(ALLOW_SVG_ELEMENTS)
            .allowAttributes(ALLOW_SVG_ATTRIBUTES).onElements(ALLOW_SVG_ELEMENTS)
            // allow data image
            .allowUrlProtocols("data")
            .allowElements(new Base64SanitizePolicy(), "img", "image", "iframe")
            .toFactory();

    /**
     *
     * @param input raw html string
     * @return html string sanitized from any xss injection
     */
    public static String sanitizeHtml(final String input) {
        final String cleanHtml = policy.sanitize(input);
        return cleanHtml;
    }

    /**
     * This ElementPolicy is applied to img which contains base64:data as "src" value
     * It checks whether the encoded base64 contains any encoded value and remove it if so
     */
    private static class Base64SanitizePolicy implements ElementPolicy {

        @Nullable
        @Override
        public String apply(String elementName, List<String> attrs) {
            try{
                // check whether src value exists
                final int index = attrs.indexOf("src");
                final int nextIndex = index + 1;
                if(index != -1 && 0 <= nextIndex &&  nextIndex < attrs.size()){
                    final String value = attrs.get(nextIndex);
                    // check whether image contains any base64 as src value
                    if(value != null && value.contains("base64,")){
                        // parse base64 and check whether it contains xss injections
                        final String base64Image = value.split(",")[1];
                        final String decodedImage = new String(Base64.getDecoder().decode(base64Image));
                        final Base64HtmlChangeListener listener = new Base64HtmlChangeListener();
                        // trigger sanitizer
                        policy.sanitize(decodedImage, listener, null);
                        // if sanitizer has detected forbidden element or attributes => remove src value
                        if(listener.countIssues > 0){
                            // remove data attribute
                            attrs.set(nextIndex, "");
                        }
                    }
                }
                // keep img element in dom
                return elementName;
            }catch(Exception e){
                // if base64 could not be parsed => remove img element from dom
                log.error("Invalid img base64 decode: ", e);
                return null;
            }
        }
    }

    private static class Base64HtmlChangeListener implements  HtmlChangeListener<Void>{
        int countIssues = 0;
        @Override
        public void discardedTag(@Nullable Void context, String elementName) {
            countIssues++;
        }
        @Override
        public void discardedAttributes(@Nullable Void context, String tagName, String... attributeNames) {
            countIssues++;
        }
    }
}

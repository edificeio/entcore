'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var core = require('@tiptap/core');

const emDash = (override) => core.textInputRule({
    find: /--$/,
    replace: override !== null && override !== void 0 ? override : '—',
});
const ellipsis = (override) => core.textInputRule({
    find: /\.\.\.$/,
    replace: override !== null && override !== void 0 ? override : '…',
});
const openDoubleQuote = (override) => core.textInputRule({
    find: /(?:^|[\s{[(<'"\u2018\u201C])(")$/,
    replace: override !== null && override !== void 0 ? override : '“',
});
const closeDoubleQuote = (override) => core.textInputRule({
    find: /"$/,
    replace: override !== null && override !== void 0 ? override : '”',
});
const openSingleQuote = (override) => core.textInputRule({
    find: /(?:^|[\s{[(<'"\u2018\u201C])(')$/,
    replace: override !== null && override !== void 0 ? override : '‘',
});
const closeSingleQuote = (override) => core.textInputRule({
    find: /'$/,
    replace: override !== null && override !== void 0 ? override : '’',
});
const leftArrow = (override) => core.textInputRule({
    find: /<-$/,
    replace: override !== null && override !== void 0 ? override : '←',
});
const rightArrow = (override) => core.textInputRule({
    find: /->$/,
    replace: override !== null && override !== void 0 ? override : '→',
});
const copyright = (override) => core.textInputRule({
    find: /\(c\)$/,
    replace: override !== null && override !== void 0 ? override : '©',
});
const trademark = (override) => core.textInputRule({
    find: /\(tm\)$/,
    replace: override !== null && override !== void 0 ? override : '™',
});
const servicemark = (override) => core.textInputRule({
    find: /\(sm\)$/,
    replace: override !== null && override !== void 0 ? override : '℠',
});
const registeredTrademark = (override) => core.textInputRule({
    find: /\(r\)$/,
    replace: override !== null && override !== void 0 ? override : '®',
});
const oneHalf = (override) => core.textInputRule({
    find: /(?:^|\s)(1\/2)\s$/,
    replace: override !== null && override !== void 0 ? override : '½',
});
const plusMinus = (override) => core.textInputRule({
    find: /\+\/-$/,
    replace: override !== null && override !== void 0 ? override : '±',
});
const notEqual = (override) => core.textInputRule({
    find: /!=$/,
    replace: override !== null && override !== void 0 ? override : '≠',
});
const laquo = (override) => core.textInputRule({
    find: /<<$/,
    replace: override !== null && override !== void 0 ? override : '«',
});
const raquo = (override) => core.textInputRule({
    find: />>$/,
    replace: override !== null && override !== void 0 ? override : '»',
});
const multiplication = (override) => core.textInputRule({
    find: /\d+\s?([*x])\s?\d+$/,
    replace: override !== null && override !== void 0 ? override : '×',
});
const superscriptTwo = (override) => core.textInputRule({
    find: /\^2$/,
    replace: override !== null && override !== void 0 ? override : '²',
});
const superscriptThree = (override) => core.textInputRule({
    find: /\^3$/,
    replace: override !== null && override !== void 0 ? override : '³',
});
const oneQuarter = (override) => core.textInputRule({
    find: /(?:^|\s)(1\/4)\s$/,
    replace: override !== null && override !== void 0 ? override : '¼',
});
const threeQuarters = (override) => core.textInputRule({
    find: /(?:^|\s)(3\/4)\s$/,
    replace: override !== null && override !== void 0 ? override : '¾',
});
/**
 * This extension allows you to add typography replacements for specific characters.
 * @see https://www.tiptap.dev/api/extensions/typography
 */
const Typography = core.Extension.create({
    name: 'typography',
    addOptions() {
        return {
            closeDoubleQuote: '”',
            closeSingleQuote: '’',
            copyright: '©',
            ellipsis: '…',
            emDash: '—',
            laquo: '«',
            leftArrow: '←',
            multiplication: '×',
            notEqual: '≠',
            oneHalf: '½',
            oneQuarter: '¼',
            openDoubleQuote: '“',
            openSingleQuote: '‘',
            plusMinus: '±',
            raquo: '»',
            registeredTrademark: '®',
            rightArrow: '→',
            servicemark: '℠',
            superscriptThree: '³',
            superscriptTwo: '²',
            threeQuarters: '¾',
            trademark: '™',
        };
    },
    addInputRules() {
        const rules = [];
        if (this.options.emDash !== false) {
            rules.push(emDash(this.options.emDash));
        }
        if (this.options.ellipsis !== false) {
            rules.push(ellipsis(this.options.ellipsis));
        }
        if (this.options.openDoubleQuote !== false) {
            rules.push(openDoubleQuote(this.options.openDoubleQuote));
        }
        if (this.options.closeDoubleQuote !== false) {
            rules.push(closeDoubleQuote(this.options.closeDoubleQuote));
        }
        if (this.options.openSingleQuote !== false) {
            rules.push(openSingleQuote(this.options.openSingleQuote));
        }
        if (this.options.closeSingleQuote !== false) {
            rules.push(closeSingleQuote(this.options.closeSingleQuote));
        }
        if (this.options.leftArrow !== false) {
            rules.push(leftArrow(this.options.leftArrow));
        }
        if (this.options.rightArrow !== false) {
            rules.push(rightArrow(this.options.rightArrow));
        }
        if (this.options.copyright !== false) {
            rules.push(copyright(this.options.copyright));
        }
        if (this.options.trademark !== false) {
            rules.push(trademark(this.options.trademark));
        }
        if (this.options.servicemark !== false) {
            rules.push(servicemark(this.options.servicemark));
        }
        if (this.options.registeredTrademark !== false) {
            rules.push(registeredTrademark(this.options.registeredTrademark));
        }
        if (this.options.oneHalf !== false) {
            rules.push(oneHalf(this.options.oneHalf));
        }
        if (this.options.plusMinus !== false) {
            rules.push(plusMinus(this.options.plusMinus));
        }
        if (this.options.notEqual !== false) {
            rules.push(notEqual(this.options.notEqual));
        }
        if (this.options.laquo !== false) {
            rules.push(laquo(this.options.laquo));
        }
        if (this.options.raquo !== false) {
            rules.push(raquo(this.options.raquo));
        }
        if (this.options.multiplication !== false) {
            rules.push(multiplication(this.options.multiplication));
        }
        if (this.options.superscriptTwo !== false) {
            rules.push(superscriptTwo(this.options.superscriptTwo));
        }
        if (this.options.superscriptThree !== false) {
            rules.push(superscriptThree(this.options.superscriptThree));
        }
        if (this.options.oneQuarter !== false) {
            rules.push(oneQuarter(this.options.oneQuarter));
        }
        if (this.options.threeQuarters !== false) {
            rules.push(threeQuarters(this.options.threeQuarters));
        }
        return rules;
    },
});

exports.Typography = Typography;
exports.closeDoubleQuote = closeDoubleQuote;
exports.closeSingleQuote = closeSingleQuote;
exports.copyright = copyright;
exports.default = Typography;
exports.ellipsis = ellipsis;
exports.emDash = emDash;
exports.laquo = laquo;
exports.leftArrow = leftArrow;
exports.multiplication = multiplication;
exports.notEqual = notEqual;
exports.oneHalf = oneHalf;
exports.oneQuarter = oneQuarter;
exports.openDoubleQuote = openDoubleQuote;
exports.openSingleQuote = openSingleQuote;
exports.plusMinus = plusMinus;
exports.raquo = raquo;
exports.registeredTrademark = registeredTrademark;
exports.rightArrow = rightArrow;
exports.servicemark = servicemark;
exports.superscriptThree = superscriptThree;
exports.superscriptTwo = superscriptTwo;
exports.threeQuarters = threeQuarters;
exports.trademark = trademark;
//# sourceMappingURL=index.cjs.map

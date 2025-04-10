import axeCore from 'axe-core';

declare let React: any;
declare let ReactDOM: any;
/**
 * Log axe violations to console.
 * @param {AxeResults} results
 */
declare function logToConsole(results: axeCore.AxeResults): void;
/**
 * To support paramater of type runOnly
 */
interface ReactSpec extends axeCore.Spec {
    runOnly?: string[];
    disableDeduplicate?: boolean;
}
/**
 * Run axe against all changes made in a React app.
 * @parma {React} _React React instance
 * @param {ReactDOM} _ReactDOM ReactDOM instance
 * @param {Number} _timeout debounce timeout in milliseconds
 * @parma {Spec} conf React axe.configure Spec object
 * @param {ElementContext} _context axe ElementContent object
 * @param {Function} _logger Logger implementation
 */
declare function reactAxe(_React: typeof React, _ReactDOM: typeof ReactDOM, _timeout: number, _conf?: ReactSpec, _context?: axeCore.ElementContext, _logger?: (results: axeCore.AxeResults) => void): Promise<void>;

export { reactAxe as default, logToConsole };

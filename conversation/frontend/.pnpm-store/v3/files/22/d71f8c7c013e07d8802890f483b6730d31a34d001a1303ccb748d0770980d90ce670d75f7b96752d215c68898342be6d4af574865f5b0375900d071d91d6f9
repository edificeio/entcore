// index.ts
import axeCore from "axe-core";
import * as rIC from "requestidlecallback";

// after.ts
var restoreFunctions = [];
function after(host, name, cb) {
  const originalFn = host[name];
  let restoreFn;
  if (originalFn) {
    host[name] = function(...args) {
      originalFn.apply(this, args);
      cb(host);
    };
    restoreFn = function() {
      host[name] = originalFn;
    };
  } else {
    host[name] = function() {
      cb(host);
    };
    restoreFn = function() {
      delete host[name];
    };
  }
  restoreFunctions.push(restoreFn);
}
after.restorePatchedMethods = function() {
  restoreFunctions.forEach((restoreFn) => restoreFn());
  restoreFunctions = [];
};

// cache.ts
var _cache = {};
var cache = {
  set(key, value) {
    _cache[key] = value;
  },
  get(key) {
    return _cache[key];
  },
  clear() {
    Object.keys(_cache).forEach((key) => {
      delete _cache[key];
    });
  }
};
var cache_default = cache;

// index.ts
var requestIdleCallback = rIC.request;
var cancelIdleCallback = rIC.cancel;
var React;
var ReactDOM;
var logger;
var lightTheme = {
  serious: "#d93251",
  minor: "#d24700",
  text: "black"
};
var darkTheme = {
  serious: "#ffb3b3",
  minor: "#ffd500",
  text: "white"
};
var theme = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches ? darkTheme : lightTheme;
var boldCourier = "font-weight:bold;font-family:Courier;";
var critical = `color:${theme.serious};font-weight:bold;`;
var serious = `color:${theme.serious};font-weight:normal;`;
var moderate = `color:${theme.minor};font-weight:bold;`;
var minor = `color:${theme.minor};font-weight:normal;`;
var defaultReset = `font-color:${theme.text};font-weight:normal;`;
var idleId;
var timeout;
var context;
var conf;
var _createElement;
var components = {};
var nodes = [document.documentElement];
function debounce(func, wait, immediate) {
  let _timeout;
  return function(...args) {
    const later = () => {
      _timeout = null;
      if (!immediate) func.apply(this, args);
    };
    const callNow = immediate && !_timeout;
    clearTimeout(_timeout);
    _timeout = setTimeout(later, wait);
    if (callNow) func.apply(this, args);
  };
}
function getPath(node) {
  const path = [node];
  while (node && node.nodeName.toLowerCase() !== "html") {
    path.push(node.parentNode);
    node = node.parentNode;
  }
  if (!node || !node.parentNode) {
    return null;
  }
  return path.reverse();
}
function getCommonParent(nodes2) {
  let path;
  let nextPath;
  if (nodes2.length === 1) {
    return nodes2.pop();
  }
  while (!path && nodes2.length) {
    path = getPath(nodes2.pop());
  }
  while (nodes2.length) {
    nextPath = getPath(nodes2.pop());
    if (nextPath) {
      path = path.filter((node, index) => {
        return nextPath.length > index && nextPath[index] === node;
      });
    }
  }
  return path ? path[path.length - 1] : document;
}
function logElement(node, logFn) {
  const el = document.querySelector(node.target.toString());
  if (!el) {
    logFn("Selector: %c%s", boldCourier, node.target.toString());
  } else {
    logFn("Element: %o", el);
  }
}
function logHtml(node) {
  console.log("HTML: %c%s", boldCourier, node.html);
}
function logFailureMessage(node, key) {
  const message = axeCore._audit.data.failureSummaries[key].failureMessage(
    node[key].map((check) => check.message || "")
  );
  console.error(message);
}
function failureSummary(node, key) {
  if (node[key].length > 0) {
    logElement(node, console.groupCollapsed);
    logHtml(node);
    logFailureMessage(node, key);
    let relatedNodes = [];
    node[key].forEach((check) => {
      relatedNodes = relatedNodes.concat(check.relatedNodes);
    });
    if (relatedNodes.length > 0) {
      console.groupCollapsed("Related nodes");
      relatedNodes.forEach((relatedNode) => {
        logElement(relatedNode, console.log);
        logHtml(relatedNode);
      });
      console.groupEnd();
    }
    console.groupEnd();
  }
}
function checkAndReport(node, timeout2) {
  const disableDeduplicate = conf["disableDeduplicate"];
  if (idleId) {
    cancelIdleCallback(idleId);
    idleId = void 0;
  }
  return new Promise((resolve, reject) => {
    nodes.push(node);
    idleId = requestIdleCallback(
      () => {
        let n = context;
        if (n === void 0) {
          n = getCommonParent(nodes.filter((node2) => node2.isConnected));
          if (n.nodeName.toLowerCase() === "html") {
            n = document;
          }
        }
        axeCore.configure({ allowedOrigins: ["<unsafe_all_origins>"] });
        axeCore.run(
          n,
          { reporter: "v2" },
          function(error, results) {
            if (error) {
              return reject(error);
            }
            results.violations = results.violations.filter((result) => {
              result.nodes = result.nodes.filter((node2) => {
                const key = node2.target.toString() + result.id;
                const retVal = !cache_default.get(key);
                cache_default.set(key, key);
                return disableDeduplicate || retVal;
              });
              return !!result.nodes.length;
            });
            if (results.violations.length) {
              logger(results);
            }
            resolve();
          }
        );
      },
      {
        timeout: timeout2
      }
    );
  });
}
function checkNode(component) {
  let node;
  try {
    node = ReactDOM.findDOMNode(component);
  } catch (e) {
    console.group("%caxe error: could not check node", critical);
    console.group("%cComponent", serious);
    console.error(component);
    console.groupEnd();
    console.group("%cError", serious);
    console.error(e);
    console.groupEnd();
    console.groupEnd();
  }
  if (node) {
    checkAndReport(node, timeout);
  }
}
function componentAfterRender(component) {
  const debounceCheckNode = debounce(checkNode, timeout, true);
  after(component, "componentDidMount", debounceCheckNode);
  after(component, "componentDidUpdate", debounceCheckNode);
}
function addComponent(component) {
  const reactInstance = component._reactInternalInstance || {};
  const reactInstanceDebugID = reactInstance._debugID;
  const reactFiberInstance = component._reactInternalFiber || {};
  const reactFiberInstanceDebugID = reactFiberInstance._debugID;
  const reactInternals = component._reactInternals || {};
  const reactInternalsDebugID = reactInternals._debugID;
  if (reactInstanceDebugID && !components[reactInstanceDebugID]) {
    components[reactInstanceDebugID] = component;
    componentAfterRender(component);
  } else if (reactFiberInstanceDebugID && !components[reactFiberInstanceDebugID]) {
    components[reactFiberInstanceDebugID] = component;
    componentAfterRender(component);
  } else if (reactInternalsDebugID && !components[reactInternalsDebugID]) {
    components[reactInternalsDebugID] = component;
    componentAfterRender(component);
  }
}
function logToConsole(results) {
  console.group("%cNew axe issues", serious);
  results.violations.forEach((result) => {
    let fmt;
    switch (result.impact) {
      case "critical":
        fmt = critical;
        break;
      case "serious":
        fmt = serious;
        break;
      case "moderate":
        fmt = moderate;
        break;
      case "minor":
        fmt = minor;
        break;
      default:
        fmt = minor;
        break;
    }
    console.groupCollapsed(
      "%c%s: %c%s %s",
      fmt,
      result.impact,
      defaultReset,
      result.help,
      result.helpUrl
    );
    result.nodes.forEach((node) => {
      failureSummary(node, "any");
      failureSummary(node, "none");
    });
    console.groupEnd();
  });
  console.groupEnd();
}
function reactAxe(_React, _ReactDOM, _timeout, _conf = {}, _context, _logger) {
  React = _React;
  ReactDOM = _ReactDOM;
  timeout = _timeout;
  context = _context;
  conf = _conf;
  logger = _logger || logToConsole;
  const runOnly = conf["runOnly"];
  if (runOnly) {
    conf["rules"] = axeCore.getRules(runOnly).map((rule) => ({ ...rule, id: rule.ruleId, enabled: true }));
    conf["disableOtherRules"] = true;
  }
  if (Object.keys(conf).length > 0) {
    axeCore.configure(conf);
  }
  axeCore.configure({ allowedOrigins: ["<unsafe_all_origins>"] });
  if (!_createElement) {
    _createElement = React.createElement;
    React.createElement = function(...args) {
      const reactEl = _createElement.apply(this, args);
      if (reactEl._owner && reactEl._owner._instance) {
        addComponent(reactEl._owner._instance);
      } else if (reactEl._owner && reactEl._owner.stateNode) {
        addComponent(reactEl._owner.stateNode);
      }
      return reactEl;
    };
  }
  return checkAndReport(document.body, timeout);
}
export {
  reactAxe as default,
  logToConsole
};

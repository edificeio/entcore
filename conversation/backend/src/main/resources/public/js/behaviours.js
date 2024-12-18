/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId])
/******/ 			return installedModules[moduleId].exports;
/******/
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			exports: {},
/******/ 			id: moduleId,
/******/ 			loaded: false
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.loaded = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(0);
/******/ })
/************************************************************************/
/******/ ([
/* 0 */
/***/ (function(module, exports, __webpack_require__) {

	"use strict";
	Object.defineProperty(exports, "__esModule", { value: true });
	var entcore_1 = __webpack_require__(1);
	entcore_1.Behaviours.register('conversation', {
	    rights: {
	        workflow: {
	            draft: 'org.entcore.conversation.controllers.ConversationController|createDraft',
	            read: 'org.entcore.conversation.controllers.ConversationController|view'
	        }
	    },
	    sniplets: {
	        ml: {
	            title: 'sniplet.ml.title',
	            description: 'sniplet.ml.description',
	            controller: {
	                init: function () {
	                    this.message = {};
	                },
	                initSource: function () {
	                    this.setSnipletSource({});
	                },
	                send: function () {
	                    this.message.to = entcore_1._.map(this.snipletResource.shared, function (shared) { return shared.userId || shared.groupId; });
	                    this.message.to.push(this.snipletResource.owner.userId);
	                    (0, entcore_1.http)().postJson('/conversation/send', this.message).done(function () {
	                        entcore_1.notify.info('ml.sent');
	                    }).e401(function () { });
	                    this.message = {};
	                }
	            }
	        }
	    }
	});


/***/ }),
/* 1 */
/***/ (function(module, exports) {

	module.exports = entcore;

/***/ })
/******/ ]);
//# sourceMappingURL=behaviours.js.map
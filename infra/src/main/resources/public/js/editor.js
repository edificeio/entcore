/// <reference path="./jquery-1.10.2.min.js" />
/// <reference path="./ui.js" />

if (!String.prototype.startsWith) {
    String.prototype.startsWith = function (str) {
        if (this.indexOf(str) !== -1 && this.split(str)[0] === '') {
            return true;
        }
        return false;
    };
}

window.RTE = (function () {
    var textNodes = ['SPAN', 'A', 'STRONG', 'EM', 'B', 'I'];
    var formatNodes = ['P', 'H1', 'H2', 'H3', 'H4', 'H5', 'H6'];

    function rgb(r, g, b) {
        return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
    }
    var rgba = rgb;
    var transparent = 'rgba(255, 255, 255, 0)';

	return {
		Instance: function(data){
			var that = this;
			this.states = [];
			this.stateIndex = 0;
			this.editZone = this.element.find('[contenteditable]');
			this.selection = new RTE.Selection({
				instance: this,
				editZone: this.editZone
			});

			this.focus = function(){
				var sel = window.getSelection();
				sel.removeAllRanges();
                if (this.selection.range) {
                    sel.addRange(this.selection.range);
                } else {
                    this.editZone.focus();
                }
			};

			this.execCommand = function(commandId, useUi, value){
				this.addState(this.editZone.html());
				document.execCommand(commandId, useUi, value);

				this.trigger('contentupdated');
			};

		    var mousePosition = {};
			this.editZone.on('mousemove', function(e){
				mousePosition = {
					left: e.pageX,
					top: e.pageY
				}
			});

			var contextualMenu = this.element.children('contextual-menu');
			contextualMenu.on('contextmenu', function(e){
				e.preventDefault();
				return false;
			});
			this.bindContextualMenu = function(scope, selector, items){
				this.editZone.on('contextmenu longclick', selector, function(e, position){
				    e.preventDefault();

                    if (position) {
                        mousePosition = position;
                    }

				    contextualMenu.children('ul').html('');
				    items.forEach(function (item) {
				        var node = $('<li></li>');
				        node.on('click', function (event) {
				            item.action(e);
				            scope.$apply();
				        });
				        node.html(lang.translate(item.label));
				        contextualMenu.children('ul').append(node)
				    });

				    contextualMenu.addClass('show');
				    e.preventDefault();
				    contextualMenu.offset({
				        top: mousePosition.top,
				        left: mousePosition.left
				    });

				    contextualMenu.children('li').on('click', function () {
				        contextualMenu.removeClass('show');
				    });

					return false;
				});
			};

			$('body').on('click', function(e){
				contextualMenu.removeClass('show');
			});

            $('body').on('touchstart', this.editZone, function (e) {
                $('body').on('touchend touchleave', this.editZone, function(f){
                    if($(e.target).parents('lightbox').parents('editor').length > 0 || !that.selection.changed()){
                        return;
                    }
                    that.trigger('selectionchange', {
                        selection: that.selection
                    });
                });
            });

			$('body').on('mouseup', function(e){
				if($(e.target).parents('lightbox').parents('editor').length > 0 || !that.selection.changed()){
					return;
				}
				that.trigger('selectionchange', {
					selection: that.selection
				});
			});

		    data.element.on('keyup', function(e){
				that.trigger('contentupdated');
				if(!that.selection.changed()){
					return;
				}
				that.trigger('selectionchange', {
					selection: that.selection
				});
				that.scope.$apply();
			});

		    this.applyState = function () {
		        this.editZone.html(
					this.compile(this.states[this.stateIndex - 1].html)(this.scope)
				);
				
				if (this.states[this.stateIndex - 1].range) {
				    var sel = window.getSelection();
				    sel.removeAllRanges();
				    sel.addRange(this.states[this.stateIndex - 1].range);
				}
			};

			this.undo = function(){
				if(this.stateIndex === 0){
					return;
				}
				this.stateIndex --;
				this.applyState();
			};

			this.redo = function(){
				if(this.stateIndex === this.states.length){
					return;
				}
				this.stateIndex ++;
				this.applyState();
			};

			this.addState = function(state){
			    if (this.states[this.stateIndex - 1] && state === this.states[this.stateIndex - 1].html) {
					return;
				}
				if(this.stateIndex === this.states.length){
					this.states.push({ html: state, range: this.selection.range});
					this.stateIndex ++;
				}
				else{
					this.states = this.states.slice(0, this.stateIndex);
					this.addState({ html: state, range: this.selection.range });
				}
			};

			this.toolbar = new RTE.Toolbar(this);
		},
		Selection: function(){
			var that = this;
			this.selectedElements = [];

			function getSelectedElements(){
				var selection = getSelection();
				if(!selection.rangeCount){
					return;
				}
				var range = selection.getRangeAt(0);
				var selector = [];
				if (that.editZone[0] === range.startContainer && that.editZone[0] === range.endContainer) {
				    that.editZone.children().each(function (index, child) {
				        selector.push(child);
				    });
				    return selector;
				}
				if (!(that.editZone.find(range.startContainer.parentNode).length && range.startContainer.parentNode !== that.editZone[0]) ||
                    !that.editZone.find(range.endContainer.parentNode).length && range.endContainer.parentNode !== that.editZone[0]) {
					return;
				}
				
				if(range.startContainer === range.endContainer){
					if(range.startContainer.childNodes.length){
						for(var i = range.startOffset; i <= range.endOffset; i++){
							selector.push(range.startContainer.childNodes[i]);
						}
					}
					else{
						if(range.startContainer !== that.editZone[0] && range.startOffset !== range.endOffset){
							selector.push(range.startContainer);
						}
						else{
							return [];
						}
					}
				}
				else {
                    if (range.startOffset < range.startContainer.textContent.length) {
                        selector.push(range.startContainer);
                    }
					
					that.editZone.find('*').each(function (index, item) {
					    if (((range.intersectsNode && range.intersectsNode(item)) || (!range.intersectsNode && $(range.commonAncestorContainer).find(item).length > 0))
                            && item !== range.startContainer.parentNode
                            && item !== range.endContainer.parentNode
                            && item !== range.endContainer
                            && !$(item).find(range.startContainer.parentNode).length
                            && !$(item).find(range.endContainer.parentNode).length) {
							selector.push(item);
						}
					});

					if (range.endContainer !== that.editZone[0] && range.endOffset > 0 && $(range.endContainer).find(range.startContainer).length === 0) {
						selector.push(range.endContainer);
					}
				}

				return selector;
			}

			this.changed = function(){
				var sel = getSelection();
				if(sel.rangeCount === 0){
					return;
				}
				var range = sel.getRangeAt(0);

				var same = this.range && this.range.startContainer === range.startContainer && this.range.startOffset === range.startOffset
						&& this.range.endContainer === range.endContainer && range.endOffset === this.range.endOffset;
			    same = same || (this.editZone.find(range.startContainer).length === 0 && this.editZone[0] !== range.startContainer);
				var selectedElements = getSelectedElements();

				if(!same && selectedElements){
					this.selectedElements = selectedElements || this.selectedElements;
				}
				if (!same && this.editZone.is(':focus')) {
				    this.range = range;
				}
				return !same;
			};

			this.selectedElements = getSelectedElements() || this.selectedElements;

			this.moveCaret = function(element, offset){
				if(!offset){
					offset = 0;
				}

				var range = document.createRange();
				range.setStart(element.firstChild || element, offset);
				this.range = range;

				var sel = getSelection();
				this.selectedElements = [];
				sel.removeAllRanges();
				sel.addRange(range);
			};

			this.selectNode = function(element, start, end){
				var range = document.createRange();
				var sel = getSelection();

				if(!(element.textContent) && !(element.nodeName && element.nodeName === 'IMG')){
					return;
				}
				if(!start){
					start = 0;
				}
				if (!end && element.textContent) {
				    end = (element.firstChild || element).textContent.length;
				}

				if (element.nodeType === 1) {
                    range.selectNode(element);
                } else {
                    range.setStart(element.firstChild || element, start);
                    range.setEnd(element.firstChild || element, end);
                }

				this.selectedElements = [element.firstChild || element];
				this.range = range;

				sel.removeAllRanges();
				sel.addRange(range);
			};

			this.wrap = function(element){
			    that.instance.addState(that.editZone.html());
			    var commonAncestor = that.range.commonAncestorContainer;
			    var startOffset = that.range.startOffset;
			    var endOffset = that.range.endOffset;
			    
				var elementAtCaret = commonAncestor;
				if (
					elementAtCaret.nodeType === 1
					&& (elementAtCaret.getAttribute('contenteditable') || elementAtCaret.nodeName === 'TD')
				) {
					var wrapper = $('<div>&#8203;</div>');
					$(elementAtCaret).append(wrapper);
					elementAtCaret = wrapper[0];
				}
				if (elementAtCaret.parentNode.nodeName === 'TD') {
					var wrapper = $('<div>&#8203;</div>');
					$(elementAtCaret.parentNode).append(wrapper);
					wrapper.append(elementAtCaret);
				}

				if(
                    this.isCursor() || commonAncestor.nodeType === 3 || textNodes.indexOf(commonAncestor.nodeName) !== -1
                ){
				    element.html('&#8203;');

					while (textNodes.indexOf(elementAtCaret.nodeName) !== -1 || elementAtCaret.nodeType === 3) {
						if (elementAtCaret.parentNode.nodeType === 1 && elementAtCaret.parentNode.getAttribute('contenteditable') || elementAtCaret.parentNode.nodeName === 'TD') {
							var newEl = document.createElement('div');
							if(elementAtCaret.nodeType === 1){
								$(newEl).html($(elementAtCaret).html());
							}
							else{
								$(newEl).text(elementAtCaret.textContent);
							}
							
							elementAtCaret.parentNode.insertBefore(newEl, elementAtCaret);
							elementAtCaret.remove();
							elementAtCaret = newEl;
						}
						else{
							elementAtCaret = elementAtCaret.parentNode;
						}
					}

					element.html($(elementAtCaret).html());
					elementAtCaret.parentNode.insertBefore(element[0], elementAtCaret);
					elementAtCaret.parentNode.removeChild(elementAtCaret);

					var sel = window.getSelection();
					var r = document.createRange();
					r.setStart(element[0].firstChild, startOffset);
					r.setEnd(element[0].firstChild, endOffset);
					sel.removeAllRanges();
					sel.addRange(r);
				}
				else {
				    if (formatNodes.indexOf(elementAtCaret.nodeName) !== -1) {
				        element.html($(elementAtCaret).html());
				        elementAtCaret.parentNode.insertBefore(element[0], elementAtCaret);
				        elementAtCaret.remove();
						var sel = window.getSelection();
						var r = document.createRange();
						r.setStart(element[0], 0);
						var endOffset = element[0].textContent.length;
						if(element[0].childNodes.length){
							endOffset = element[0].childNodes.length;
						}
						r.setEnd(element[0], endOffset);
						sel.removeAllRanges();
						sel.addRange(r);
				    }
				    else {
				        var foundFirst = false;
				        var foundLast = false;
				        for (var i = 0; i < elementAtCaret.childNodes.length; i++) {
				            var item = elementAtCaret.childNodes[i];
				            if (item === this.range.startContainer ||
                                (this.range.startContainer.nodeType === 1 && $(this.range.startContainer).find(item).length) ||
                                (item.nodeType === 1 && $(item).find(this.range.startContainer).length)) {
				                foundFirst = true;
				            }
				            if (item === this.range.endContainer ||
                                (this.range.endContainer.nodeType === 1 && $(this.range.endContainer).find(item).length) ||
                                (item.nodeType === 1 && $(item).find(this.range.endContainer).length)) {
				                foundLast = true;
				                if (this.range.endOffset === 0) {
				                    break;
				                }
				            }
				            if (!foundFirst) {
				                continue;
				            }
				            var el = $(element[0].outerHTML);
				            while (textNodes.indexOf(item.nodeName) !== -1 || item.nodeType === 3) {
				                item = item.parentNode;
				            }
							if(!item.parentNode){
								continue;
							}

				            el.html(item.innerHTML || item.textContent);
				            item.parentNode.insertBefore(el[0], item);
							item.remove();
				            var sel = window.getSelection();
							var r = document.createRange();
							r.setStart(el[0], 0);
							var endOffset = el[0].textContent.length;
							if(el[0].childNodes.length){
								endOffset = el[0].childNodes.length;
							}
							r.setEnd(el[0], endOffset);
							sel.removeAllRanges();
							sel.addRange(r);
				            if (foundLast) {
				                break;
				            }
				        }
				    }
				}
				that.instance.addState(that.editZone.html());
				setTimeout(function () {
				    this.instance.trigger('selectionchange', { selection: this.instance.selection });
					this.instance.trigger('change');
				}.bind(this), 100);
			};

			this.isCursor = function () {
			    return this.range.startContainer === this.range.endContainer && this.range.startOffset === this.range.endOffset;
			}

			this.wrapText = function(el){
				this.instance.addState(this.editZone.html());
				if(!this.selectedElements.length){
					el.html('<br />');
					this.editZone.append(el);
					this.selectNode(el[0]);
				}
				else{
					var addedNodes = [];
					this.selectedElements.forEach(function(item, index){
						var node = $(el[0].outerHTML);
						if(item.nodeType === 1){
							$(item).wrapInner(node);
						}
						else{
							if(that.range.startContainer === item && that.range.startOffset >= 0 && that.range.startContainer !== that.range.endContainer){
							    node.html(item.textContent.substring(that.range.startOffset));
							    item.parentNode.insertBefore(node[0], item.nextSibling);
								item.textContent = item.textContent.substring(0, that.range.startOffset);
							}
							else if (that.range.endContainer === item && that.range.endOffset <= item.textContent.length && that.range.startContainer !== that.range.endContainer) {
							    node.text(item.textContent.substring(0, that.range.endOffset));
								item.parentNode.insertBefore(node[0], item);
								item.textContent = item.textContent.substring(that.range.endOffset);
							}
							else if (that.range.startContainer === that.range.endContainer && that.range.startContainer === item) {
							    node.html(item.textContent.substring(that.range.startOffset, that.range.endOffset));
								var textBefore = document.createTextNode('');
								textBefore.textContent = item.textContent.substring(0, that.range.startOffset);
								item.parentNode.insertBefore(node[0], item);
								item.parentNode.insertBefore(textBefore, node[0]);
								item.textContent = item.textContent.substring(that.range.endOffset);
							}
							else {
							    node.html(item.textContent);
							    item.parentNode.insertBefore(node[0], item);
							    item.textContent = "";
							}
							addedNodes.push(node[0]);
						}
					});
					addedNodes.forEach(that.selectNode);
				}

				that.instance.trigger('change');
			};

			function applyCSSCursor(css){
			    var el = $('<span>&#8203;</span>');
				if (!that.range && !that.editZone.html()) {
					var elementAtCaret = $('<div></div>').appendTo(that.editZone);
				}
				else {
					var elementAtCaret = that.range.startContainer;
					if (elementAtCaret.nodeType === 1 && elementAtCaret.nodeName === 'SPAN') {
						el.attr('style', $(elementAtCaret).attr('style'));
					}
				}
				
				el.css(css);
				var nodeBefore = $('<span>' + elementAtCaret.textContent.substring(0, that.range.startOffset) + '</span>');
				var nodeAfter = $('<span>' + elementAtCaret.textContent.substring(that.range.startOffset) + '</span>');
				if (nodeAfter.text().length) {
				    elementAtCaret.parentNode.insertBefore(nodeAfter[0], elementAtCaret);
				    elementAtCaret.parentNode.insertBefore(el[0], nodeAfter[0]);
				    elementAtCaret.parentNode.insertBefore(nodeBefore[0], el[0]);
				    elementAtCaret.remove();
				}
				else {
				    elementAtCaret.parentNode.insertBefore(el[0], elementAtCaret.nextSibling);
				}
				
				that.moveCaret(el[0], 1);
			}

			function applyCSSNode(css){
				var element = that.range.startContainer;
				if (element.nodeType !== 1 && element.parentNode.nodeName !== 'SPAN') {
					var el = document.createElement('span');
					el.textContent = element.textContent;
					element.parentNode.insertBefore(el, element.nextSibling);
					$(element).remove();
					element = el;
				}
				else {
					element = element.parentNode;
				}
				$(element).css(css);
				$(element).find('*').css(css);
				that.selectNode(element);
			}

			function applyCSSBetween(nodeStart, nodeEnd, css, keepRangeStart, startOffset) {
			    if (startOffset === undefined) {
			        startOffset = that.range.startOffset;
			    }
			    var addedNodes = [];
			    var sibling = nodeStart;
			    var i = startOffset;
			    do {
			        if (sibling.nodeType === 1) {
			            $(sibling).css(css);
			        }
			        else {
			            var el = $('<span></span>')
							.css(css);
			            if (sibling === nodeStart && sibling === nodeEnd) {
			                el.html(sibling.textContent.substring(startOffset, that.range.endOffset));
			                if (el.html().length > 0) {
			                    sibling.parentNode.insertBefore(el[0], sibling);
			                    var afterText = document.createTextNode(sibling.textContent.substring(that.range.endOffset));
			                    sibling.parentNode.insertBefore(afterText, el[0].nextSibling);
			                    sibling.textContent = sibling.textContent.substring(0, startOffset);
			                    var sel = document.getSelection();
			                    var r = document.createRange();
			                    if (keepRangeStart) {
			                        r.setStart(that.range.startContainer, startOffset);
			                    }
			                    else {
			                        r.setStart(el[0], 0);
			                    }

			                    r.setEnd(el[0], 1);
			                    sel.removeAllRanges();
			                    sel.addRange(r);
			                    that.range = r;
			                }
			            }
			            else if (
                            sibling === nodeEnd
                            || (sibling.parentNode === that.range.endContainer && sibling === that.range.endContainer.childNodes[that.range.endOffset])
                        ) {
			                el.text(sibling.textContent.substring(0, that.range.endOffset));
			                if (el.text()) {
			                    sibling.parentNode.insertBefore(el[0], sibling);
			                    sibling.textContent = sibling.textContent.substring(that.range.endOffset);
			                    var sel = document.getSelection();
			                    var r = document.createRange();
			                    r.setStart(that.range.startContainer, startOffset);
			                    r.setEnd(el[0], 1);
			                    sel.removeAllRanges();
			                    sel.addRange(r);
			                    that.range = r;
			                }
			            }
			            else if (sibling === nodeStart) {
			                el.text(sibling.textContent.substring(startOffset, sibling.textContent.length));
			                if(el.text()){
			                    sibling.parentNode.insertBefore(el[0], sibling.nextSibling);
			                    sibling.textContent = sibling.textContent.substring(0, startOffset);
			                    if (!keepRangeStart) {
			                        var sel = document.getSelection();
			                        var r = document.createRange();
			                        r.setStart(el[0], 0);
			                        r.setEnd(that.range.endContainer, that.range.endOffset);
			                        sel.removeAllRanges();
			                        sel.addRange(r);
			                        that.range = r;
			                    }
			                }
			            }
			            else {
			                el.text(sibling.textContent.substring(0, sibling.textContent.length));
			                if (el.text()) {
			                    sibling.parentNode.insertBefore(el[0], sibling);
			                    sibling.textContent = sibling.textContent.substring(sibling.textContent.length);
			                }
			                
			            }
			        }
					if(sibling !== nodeEnd){
						sibling = sibling.nextSibling;
					}
			        
			        i++;
			    } while (
                    sibling && sibling !== nodeEnd
                    && !(sibling.parentNode === that.range.endContainer && sibling === that.range.endContainer.childNodes[that.range.endOffset])
                    && !$(sibling).find(that.range.endContainer).length
                );

			    return addedNodes;
			}

			function applyCSSText(css){
				var el = $(document.createElement('span'));
				$(el).css(css);

				el.html(that.range.startContainer.textContent.substring(that.range.startOffset, that.range.endOffset));
				var textBefore = document.createTextNode('');
				textBefore.textContent = that.range.startContainer.textContent.substring(0, that.range.startOffset);
				that.range.startContainer.parentNode.insertBefore(el[0], that.range.startContainer);
				that.range.startContainer.parentNode.insertBefore(textBefore, el[0]);
				that.range.startContainer.textContent = that.range.startContainer.textContent.substring(that.range.endOffset);

				var sel = document.getSelection();
				var r = document.createRange();
				r.setStart(el[0], 0);
				r.setEnd(el[0], 1);
				sel.removeAllRanges();
				sel.addRange(r);
			}

			function applyCSS(css) {
			    that.instance.addState(that.editZone.html());

				if(that.isCursor()){
					applyCSSCursor(css);
				}
				else if (that.range.startContainer === that.range.endContainer &&
					(
						that.range.startContainer.nodeType === 3 &&
						that.range.startOffset === 0 &&
						that.range.endOffset === that.range.startContainer.textContent.length
					)
                ) {
				    applyCSSNode(css)
				}
				else if(that.range.startContainer === that.range.endContainer && 
					(
						that.range.startContainer.nodeType === 1 &&
						that.range.startOffset === 0 &&
						that.range.endOffset === that.range.startContainer.childNodes.length
					)
				){
					if(that.range.startContainer !== that.editZone[0]){
					    $(that.range.startContainer).css(css);
					    $(that.range.startContainer).find('span').css(css);
					}
					else{
						$(that.range.startContainer).find('*').css(css);
					}
				}
				else{
					var addedNodes = [];

					if(that.range.commonAncestorContainer.nodeType === 3){
						addedNodes = addedNodes.concat(applyCSSText(css));
					}
					else{
						var foundFirst = false;
						for(var i = 0; i < that.range.commonAncestorContainer.childNodes.length; i++){
							var sibling = that.range.commonAncestorContainer.childNodes[i];
							if(
								sibling === that.range.startContainer || 
								(sibling.nodeType === 1 && $(sibling).find(that.range.startContainer).length) || 
								(that.range.startContainer.nodeType === 1 && $(that.range.startContainer).find(sibling).length && that.range.startOffset === i)
							){
								foundFirst = true;
							}
							if(!foundFirst){
								continue;
							}

							if(sibling === that.range.endContainer && that.range.endOffset === 0){
								break;
							}

							if (
                                sibling.nodeType === 1 && $(sibling).find(that.range.startContainer).length
                            ) {
							    addedNodes = addedNodes.concat(
									applyCSSBetween(that.range.startContainer, that.range.endContainer, css)
								);
							    continue;
							}

							if (
                                (
                                    that.range.startContainer.nodeType === 1
                                    && $(that.range.startContainer).find(sibling).length
                                    && that.range.startOffset === i
                                )
                                || sibling === that.range.startContainer
                            ) {
							    addedNodes = addedNodes.concat(
									applyCSSBetween(sibling, that.range.endContainer, css)
								);
							    continue;
							}

							if (
                                sibling.nodeType === 1 
                                && $(sibling).find(that.range.endContainer).length
                            ) {
							    var firstChild = sibling.firstChild;
							    var startOffset = 0;
							    if ($(sibling).find(that.range.startContainer).length) {
							        firstChild = that.range.startContainer;
							        startOffset = that.range.startOffset;
							    }
                                addedNodes = addedNodes.concat(
									applyCSSBetween(firstChild, that.range.endContainer, css, true, startOffset)
								);
                                break;
							}

							if (
                                (
                                    that.range.endContainer.nodeType === 1
                                    && $(that.range.endContainer).find(sibling).length
                                    && that.range.endOffset === i
                                ) ||
                                    sibling === that.range.endContainer
                                )
							{
							    var firstChild = that.range.endContainer.firstChild;
							    if (!firstChild) {
							        firstChild = that.range.endContainer;
							    }
							    addedNodes = addedNodes.concat(
									applyCSSBetween(firstChild,  that.range.endContainer, css, true)
								);
							    break;
							}

							if(sibling.nodeType === 1){
								addedNodes.push(sibling);
								$(sibling).css(css);
								$(sibling).find('*').css(css);
								continue;
							}
							else{
								var el = $(document.createElement('span'));
								el.css(css);
								addedNodes.push(el[0]);
								el.text(sibling.textContent);
								sibling.parentNode.insertBefore(el[0], sibling);
								sibling.remove();
							}

							if(sibling === that.range.endContainer){
								break;
							}
						}
					}

					that.instance.trigger('selectionchange', {
						selection: that.instance.selection
					});
				}

				//cleanup
				that.editZone.find('span').each(function(index, item){
					if(item.childNodes.length === 1 && item.childNodes[0].nodeType === 1){
						let cssObj = '{' + $(item).attr('style').replace(/;/i, ',').slice(0,-1) + '}';
						$(item).css(cssObj);
						$(item).html($(item).children().first().html());
					}
					if(!$(item).html()){
						$(item).remove();
					}
				});

                that.instance.addState(that.editZone.html());
				that.instance.trigger('contentupdated');
			}

			this.isEmpty = function () {
			    return !this.range || this.isCursor();
			};

			this.elementAtCaret = function () {
			    if (!this.range || !this.editZone.is(':focus')) {
			        return $();
			    }
			    var element = this.range.startContainer;
			    if (element.nodeType !== 1) {
			        element = element.parentNode;
			    }
			    return $(element);
			}

			this.css = function(params){
				if(typeof params === 'object'){
					applyCSS(params);
				}
				else {
				    if (!this.selectedElements.length) {
				        if (!this.range) {
				            return;
				        }
				        var node = this.range.startContainer;
				        if (node.nodeType === 1) {
				            return $(node).css(params);
				        }
				        else {
				            return $(node.parentNode).css(params);
				        }
				    }
				    var different = false;
				    var val = undefined;
				    this.selectedElements.forEach(function (item) {
						if(!item){
							return;
						}
				        var itemVal;
				        if (item.nodeType === 1) {
				            itemVal = $(item).css(params);
				        }
				        else{
				            itemVal = $(item.parentNode).css(params);
				        }

				        if (itemVal !== val && val !== undefined) {
				            different = true;
				        }
				        val = itemVal;
				    });
				    if (different) {
				        val = undefined;
				    }
				    return val;
				}
			};

			this.replaceHTML = function(htmlContent){
				that.instance.addState(that.editZone.html());
				var wrapper = $('<div></div>');
				wrapper.html(htmlContent + '&#8203;');
				if (this.range) {
				    this.range.deleteContents();
					this.range.insertNode(wrapper[0]);
				}
				else{
					this.editZone.append(wrapper);
				}

				that.instance.trigger('change');
			};

			this.replaceHTMLInline = function (htmlContent) {
			    that.instance.addState(that.editZone.html());
			    var wrapper = $('<span></span>');
			    wrapper.html(htmlContent);
			    if (this.range) {
			        this.range.deleteContents();
			        this.range.insertNode(wrapper[0]);
			    }
			    else {
			        this.editZone.append(wrapper);
					var sel = window.getSelection();
					var range = document.createRange();
					range.setStart(wrapper[0], wrapper[0].childNodes.length);
					sel.removeAllRanges();
					sel.addRange(range);
			    }

			    that.instance.trigger('change');
			};

			this.$ = function(){
				var jSelector = $();
				this.selectedElements.forEach(function(item){
					if(!item){
						return;
					}
					if(item.nodeType === 1){
						jSelector = jSelector.add(item);
					}
					else{
						jSelector = jSelector.add(item.parentNode);
					}

				});
				return jSelector;
			};
		},
		Toolbar: function(instance){
			instance.toolbarConfiguration.options.forEach(function(option){
				var optionElement = $('<div></div>');
				optionElement.addClass('option');
				optionElement.addClass(option.name.replace(/([A-Z])/g, "-$1").toLowerCase());
				instance.element.find('editor-toolbar').append(optionElement);
				var optionScope = instance.scope.$new();

				var optionResult = option.run(instance);
				if (optionResult.template) {
				    optionElement.html(instance.compile(optionResult.template)(optionScope));
				    optionResult.link(optionScope, optionElement, instance.attributes);
				}
				if (optionResult.templateUrl) {
				    http().get(optionResult.templateUrl).done(function (content) {
				        optionElement.html(instance.compile(content)(optionScope));
				        optionResult.link(optionScope, optionElement, instance.attributes);
				    }.bind(this));
				}
			});
		},
		ToolbarConfiguration: function(){
			this.collection(RTE.Option);
			this.option = function(name, fn){
				this.options.push({
					name: name,
					run: fn
				});
			};
		},
		Option: function(){

		},
		setModel: function(){
			model.makeModels(RTE);
			RTE.baseToolbarConf = new RTE.ToolbarConfiguration();
		},
		addDirectives: function(module){
			this.setModel();

			// Editor options
			RTE.baseToolbarConf.option('undo', function(instance){
				return {
					template: '<i tooltip="editor.option.undo"></i>',
					link: function(scope, element, attributes){
						element.addClass('disabled');
						element.on('click', function(){
							instance.undo();
							if(instance.stateIndex === 0){
								element.addClass('disabled');
							}
							else{
								element.removeClass('disabled');
							}
							instance.trigger('change');
						});

						instance.on('contentupdated', function(e){
							if(instance.stateIndex === 0){
								element.addClass('disabled');
							}
							else{
								element.removeClass('disabled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('redo', function(instance){
				return {
					template: '<i tooltip="editor.option.redo"></i>',
					link: function(scope, element, attributes){
						element.addClass('disabled');
						element.on('click', function(){
							instance.redo();
							if(instance.stateIndex === instance.states.length){
								element.addClass('disabled');
							}
							else{
								element.removeClass('disabled');
							}
							instance.trigger('change');
						});

						instance.on('contentupdated', function(e){
						    if (instance.stateIndex === instance.states.length) {
								element.addClass('disabled');
							}
							else{
								element.removeClass('disabled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('bold', function(instance){
				return {
					template: '<i tooltip="editor.option.bold"></i>',
					link: function(scope, element, attributes){
					    element.on('click', function () {
					        if (!instance.editZone.is(':focus')) {
					            instance.focus();
					        }

					        if (document.queryCommandState('bold')) {
					            element.removeClass('toggled');
					            instance.selection.css({ 'font-weight': 'normal' });
					        }
					        else {
					            element.addClass('toggled');
					            instance.selection.css({ 'font-weight': 'bold' });
					        }
					    });

					    instance.on('selectionchange', function (e) {
					        if (document.queryCommandState('bold')) {
					            element.addClass('toggled');
					        }
					        else {
					            element.removeClass('toggled');
					        }
					    });
					}
				};
			});

			RTE.baseToolbarConf.option('italic', function(instance){
				return {
					template: '<i tooltip="editor.option.italic"></i>',
					link: function(scope, element, attributes){
						element.on('click', function(){
							if(!instance.editZone.is(':focus')){
								instance.focus();
							}

							if(document.queryCommandState('italic')){
							    element.removeClass('toggled');
							    instance.selection.css({ 'font-style': 'normal' });
							}
							else{
							    element.addClass('toggled');
							    instance.selection.css({ 'font-style': 'italic' });
							}
						});

						instance.on('selectionchange', function(e){
							if(document.queryCommandState('italic')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('underline', function(instance){
				return {
					template: '<i tooltip="editor.option.underline"></i>',
					link: function(scope, element, attributes){
						element.on('click', function(){
						    if (document.queryCommandState('underline')) {
								instance.execCommand('underline');
						        element.removeClass('toggled');
							}
						    else {
						        instance.selection.css({ 'text-decoration': 'underline' });
						        element.addClass('toggled');
							}
						});

						instance.on('selectionchange', function(e){
							if(document.queryCommandState('underline')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

			function beforeJustify(instance){
				instance.editZone.find('mathjax').html('');
				instance.editZone.find('mathjax').removeAttr('contenteditable');
			}

			function afterJustify(instance){
				instance.editZone.find('mathjax').each(function(index, item){
					var scope = angular.element(item).scope();
					scope.updateFormula(scope.formula)
				})
				instance.editZone.find('mathjax').attr('contenteditable', 'false');

				instance.trigger('justify-changed');
			}

			RTE.baseToolbarConf.option('justifyLeft', function(instance){
				return {
					template: '<i tooltip="editor.option.justify.left"></i>',
					link: function(scope, element, attributes){
						element.addClass('toggled');
						element.on('click', function () {
							if(!instance.editZone.is(':focus')){
								instance.focus();
							}
							beforeJustify(instance)
							instance.execCommand('justifyLeft');
							if(document.queryCommandState('justifyLeft')){
								element.addClass('toggled');							}
							else{
								element.removeClass('toggled');
							}

							instance.editZone.find('img').each(function (index, item) {
							    if ($(item).css('text-align') === 'left' && !$(item).hasClass('smiley')) {
							        $(item).css({ 'float': 'left', 'z-index': 0 });
							    }
							});

							afterJustify(instance)
						});

						instance.on('selectionchange', function(e){
							if(document.queryCommandState('justifyLeft') && instance.selection.css('float') !== 'right' && instance.selection.css('z-index') !== "1"){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});

						instance.on('justify-changed', function(e){
							if(document.queryCommandState('justifyLeft') && instance.selection.css('float') !== 'right' && instance.selection.css('z-index') !== "1"){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('justifyRight', function(instance){
				return {
					template: '<i tooltip="editor.option.justify.right"></i>',
					link: function(scope, element, attributes){
					    element.on('click', function () {
							if(!instance.editZone.is(':focus')){
								instance.focus();
							}

							beforeJustify(instance);
							if(!document.queryCommandState('justifyRight')){
								instance.execCommand('justifyRight');
								element.addClass('toggled');
							}
							else{
								instance.execCommand('justifyLeft');
								element.removeClass('toggled');
							}

							instance.editZone.find('img').each(function (index, item) {
							    if ($(item).css('text-align') === 'right' && !$(item).hasClass('smiley')) {
							        $(item).css({ 'float': 'right', 'z-index': '0' });
							    }
							});

							afterJustify(instance);
						});

					    instance.on('selectionchange', function (e) {
							if(document.queryCommandState('justifyRight') || instance.selection.css('float') === 'right'){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});

						instance.on('justify-changed', function(e){
							if(document.queryCommandState('justifyRight') || instance.selection.css('float') === 'right'){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('justifyCenter', function(instance){
				return {
					template: '<i tooltip="editor.option.justify.center"></i>',
					link: function(scope, element, attributes){
						element.on('click', function(){
							if(!instance.editZone.is(':focus')){
								instance.focus();
							}

							beforeJustify(instance);
							if(!document.queryCommandState('justifyCenter')){
								instance.execCommand('justifyCenter');
								element.addClass('toggled');
							}
							else{
								instance.execCommand('justifyLeft');
								element.removeClass('toggled');
							}

							instance.editZone.find('img').each(function (index, item) {
							    if ($(item).css('text-align') === 'center' && !$(item).hasClass('smiley')) {
                                    // z-index is a hack to track margin width; auto width is computed as 0 in FF
							        $(item).css({ 'float': 'none', 'margin': 'auto', 'z-index': '1' });
							    }
							    else if(!$(item).hasClass('smiley')){
                                    // z-index is a hack to track margin width; auto width is computed as 0 in FF
							        $(item).css({ 'float': 'left', 'z-index': '0' });
                                }
							});

							afterJustify(instance);
						});

						instance.on('selectionchange', function(e){
                            // z-index is a hack to track margin width; auto width is computed as 0 in FF
							if(document.queryCommandState('justifyCenter')
                                || (instance.selection.css('margin-left') === instance.selection.css('margin-right') && instance.selection.css('z-index') === '1')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});

						instance.on('justify-changed', function(e){
                            // z-index is a hack to track margin width; auto width is computed as 0 in FF
							if(document.queryCommandState('justifyCenter')
                                || (instance.selection.css('margin-left') === instance.selection.css('margin-right') && instance.selection.css('z-index') === '1')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('justifyFull', function(instance){
				return {
					template: '<i tooltip="editor.option.justify.full"></i>',
					link: function(scope, element, attributes){
						element.on('click', function(){
							if(!instance.editZone.is(':focus')){
								instance.focus();
							}

							beforeJustify(instance);
							if(!document.queryCommandState('justifyFull')){
								element.addClass('toggled');
								instance.execCommand('justifyFull');
							}
							else{
								instance.execCommand('justifyLeft');
								element.removeClass('toggled');
							}

							afterJustify(instance);
						});

						instance.on('selectionchange', function(e){
							if(document.queryCommandState('justifyFull')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});

						instance.on('justify-changed', function(e){
							if(document.queryCommandState('justifyFull')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('ulist', function(instance){
				return {
					template: '<i tooltip="editor.option.ulist"></i>',
					link: function(scope, element, attributes){
					    element.on('mousedown', function () {

							if(!instance.editZone.is(':focus')){
								instance.editZone.focus();
							}

							if (instance.editZone.children('div').length === 0) {
							    instance.editZone.append('<div><br></div>');
							    instance.editZone.focus();
							}

							instance.execCommand('insertUnorderedList', false, null);
							if(document.queryCommandState('insertUnorderedList')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});

						instance.on('selectionchange', function(e){
							if(document.queryCommandState('insertUnorderedList')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('olist', function(instance){
				return {
					template: '<i tooltip="editor.option.olist"></i>',
					link: function(scope, element, attributes){
						element.on('mousedown', function(){
							if(!instance.editZone.is(':focus')){
								instance.editZone.focus();
							}

                            if (instance.editZone.children('div').length === 0) {
                                instance.editZone.append('<div><br></div>');
                                instance.editZone.focus();
                            }

							instance.execCommand('insertOrderedList', false, null);
							if(document.queryCommandState('insertOrderedList')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});

						instance.on('selectionchange', function(e){
							if(document.queryCommandState('insertOrderedList')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});
					}
				};
			});

            function setSpectrum(){
                if($('.sp-replacer').length === 0){
                    return;
                }
                
                $('input[type=color]').css({
                    position: 'absolute',
                    opacity: 0,
                    'pointer-events': 'none'
                });
                $('.sp-replacer').on('mouseover', function(e){ 
                    $(e.target).parent().find('input[type=color]').trigger('mouseover', [e]);
                });
                $('.sp-replacer').on('mouseout', function(e){ 
                    $(e.target).parent().find('input[type=color]').trigger('mouseout', [e]);
                });
            } 

			RTE.baseToolbarConf.option('color', function(instance){
				return {
				    template: '<i tooltip="editor.option.color"></i>' +
                        '<input tooltip="editor.option.color" type="color" />',
					link: function (scope, element, attributes) {
					    element.on('click', 'i', function () {
					        element.find('input').click();
					    });
					    if (navigator.userAgent.indexOf('Edge') !== -1) {
					        element.find('input').attr('type', 'text');
					    }
					    if (!$.spectrum) {
							$.spectrum = {};
							http().get('/infra/public/spectrum/spectrum.js').done(function(data){
								eval(data);
								setSpectrum();
								if ($.spectrum && $.spectrum.palettes && element.find('input')[0].type === 'text') {
								    $('body').find('.option.color input, .option.background-color input').spectrum({preferredFormat: "hex"});
								    setSpectrum();
								}
							});
							var stylesheet = $('<link rel="stylesheet" type="text/css" href="/infra/public/spectrum/spectrum.css" />');
							$('head').prepend(stylesheet);
						}
					    if ($.spectrum && $.spectrum.palettes && element.find('input')[0].type === 'text') {
					        element.find('input').spectrum({ preferredFormat: "hex" });
					        setSpectrum();
					    }
						scope.foreColor = "#000000";
						element.children('input').on('change', function(){
							scope.foreColor = $(this).val();
							scope.$apply('foreColor');
						});

						scope.$watch('foreColor', function(){
						    if(scope.foreColor !== eval(instance.selection.css('color')) && !(instance.selection.isEmpty() && scope.foreColor === '#000000')) {
						        instance.selection.css({ 'color': scope.foreColor });
						    }
						});

						instance.on('selectionchange', function(e){
						    scope.foreColor = eval(instance.selection.css('color'));
							element.children('input').val(scope.foreColor);
						});
					}
				};
			});

			RTE.baseToolbarConf.option('backgroundColor', function(instance){
				return {
					template: '<i></i><input tooltip="editor.option.backgroundcolor" type="color" />',
					link: function(scope, element, attributes){
						element.on('click', 'i', function () {
					        element.find('input').click();
					    });
						if (navigator.userAgent.indexOf('Edge') !== -1) {
						    element.find('input').attr('type', 'text');
						}
						if(!$.spectrum){
							$.spectrum = {};
							http().get('/infra/public/spectrum/spectrum.js').done(function(data){
								eval(data);
								if ($.spectrum && $.spectrum.palettes && element.find('input')[0].type === 'text') {
								    $('body').find('.option.color input, .option.background-color input').spectrum({ preferredFormat: "hex" });
								    setSpectrum();
								}
							});
							var stylesheet = $('<link rel="stylesheet" type="text/css" href="/infra/public/spectrum/spectrum.css" />');
							$('head').prepend(stylesheet);
						}
						else if ($.spectrum && $.spectrum.palettes && element.find('input')[0].type === 'text') {
						    element.find('input[type=color]').spectrum({ preferredFormat: "hex" });
                            setSpectrum();
						}
						element.children('input').on('change', function () {
						    if (!$(this).val()) {
						        return;
						    }
							scope.backColor = $(this).val();
							scope.$apply('backColor');
						});

						scope.$watch('backColor', function () {
						    var rgbColor = {};
						    if (typeof scope.backColor === 'string') {
						        if(scope.backColor[0] === '#'){
						            rgbColor = {
						                r: parseInt(scope.backColor.substring(1, 3), 16),
						                g: parseInt(scope.backColor.substring(3, 5), 16),
						                b: parseInt(scope.backColor.substring(5, 7), 16)
						            }
						        }
						        else if (scope.backColor.startsWith('rgb')) {
						            var spl = scope.backColor.split('(')[1].split(',');
						            rgbColor = {
						                r: parseInt(spl[0]),
						                g: parseInt(spl[1]),
						                b: parseInt(spl[2]),
						                a: parseInt(spl[3])
						            }
						        }
						    
						        if (rgbColor.r > 130 && rgbColor.g > 130 && rgbColor.b > 130 && rgbColor.a !== 0) {
						            element.find('i').css({ 'color': '#000' });
						        }
						        else {
						            element.find('i').css({ 'color': '#fff' });
						        }
						    }
						    
						    if(scope.backColor !== eval(instance.selection.css('background-color')) && rgbColor.a !== 0 && scope.backColor) {
						        instance.selection.css({ 'background-color': scope.backColor });
						    }
						});

						instance.on('selectionchange', function(e){
						    scope.backColor = eval(instance.selection.css('background-color'));
						    if (scope.backColor === 'rgba(255, 255, 255, 0)') {
						        scope.backColor = '';
						    }
						    element.children('input').val(scope.backColor);
						    scope.$apply('backColor');
						});
					}
				};
			});

			RTE.baseToolbarConf.option('font', function(instance){
				return {
					template:
					'<select-list display="font" display-as="fontFamily" placeholder="editor.font.placeholder" tooltip="editor.option.font">' +
					'<opt ng-repeat="font in fonts" ng-click="setFontFamily(font)" ' +
                    'value="font" style="font-family: [[font.fontFamily]]">[[font.fontFamily]]</opt>' +
					'</select-list>',
					link: function(scope, element, attributes){

						function loadImportedFonts(){
							return _.map(
								_.flatten(
									_.map(
										document.styleSheets,
										function(stylesheet){
											return _.filter(
												stylesheet.cssRules,
												function(cssRule){
													return cssRule instanceof CSSFontFaceRule &&
														cssRule.style.cssText.toLowerCase().indexOf('fontello') === -1 &&
														cssRule.style.cssText.toLowerCase().indexOf('glyphicon') === -1 &&
														cssRule.style.cssText.toLowerCase().indexOf('fontawesome') === -1 &&
													    cssRule.style.cssText.toLowerCase().indexOf('mathjax') === -1;
												}
											)
										}
									)
								),
								function(fontFace){
									return {
										fontFamily: fontFace.style.cssText.split('font-family:')[1].split(';')[0]
									}
								}
							);
						}

						scope.fonts = [{ fontFamily: 'Arial' }, { fontFamily: 'Verdana' }, { fontFamily: 'Tahoma' }, { fontFamily: "Comic Sans MS" }];
						scope.font = '';

						setTimeout(function() {
							var importedFonts = loadImportedFonts();
							importedFonts = _.uniq(importedFonts, function(item, key, a) { 
								return item.fontFamily;
							});
							scope.fonts = scope.fonts.concat(importedFonts);
							scope.font = _.find(scope.fonts, function (font) {
							    return $('p').css('font-family').toLowerCase().indexOf(font.fontFamily.toLowerCase()) !== -1
							});
						}, 1000);

						scope.setFontFamily = function (font) {
						    scope.font = font;
							instance.execCommand('fontName', false, scope.font.fontFamily);
						};

						instance.on('selectionchange', function(e){
						    scope.font = _.find(scope.fonts, function (font) {
						        return font.fontFamily.trim() === instance.selection.css('font-family');
						    });
						});
					}
				};
			});

			RTE.baseToolbarConf.option('fontSize', function(instance) {
				return {
					template: '<select-list placeholder="size" display="font.fontSize.size" tooltip="editor.option.fontSize">' +
					'<opt ng-repeat="fontSize in font.fontSizes" ng-click="setSize(fontSize)" ' +
                        'style="font-size: [[fontSize.size]]px; line-height: [[fontSize.size]]px">' +
                            '[[fontSize.size]]' +
                        '</opt>' +
					'</select-list>',
					link: function (scope, element, attributes) {
					    scope.font = {
					        fontSizes: [{ size: 8 }, { size: 10 }, { size: 12 }, { size: 14 },
                                { size: 16 }, { size: 18 }, { size: 20 }, { size: 24 }, { size: 28 },
                                { size: 34 }, { size: 42 }, { size: 64 }, { size: 72 }],
					        fontSize: {}
					    };

					    scope.setSize = function (fontSize) {
					        scope.font.fontSize = { size: fontSize.size };
							instance.selection.css({
							    'font-size': fontSize.size + 'px',
                                'line-height': fontSize.size + 'px'
							});
					    };

					    instance.on('selectionchange', function (e) {
					        if (instance.selection.css('font-size')) {
					            scope.font.fontSize = { size: parseInt(instance.selection.css('font-size')) };
					        }
					        else {
					            scope.font.fontSize = { size: undefined };
					        }

						});

						element.children('.options').on('click', 'opt', function () {
						    element.children('.options').addClass('hidden');
						});
					}
				}
			});

			RTE.baseToolbarConf.option('format', function(instance) {
				return {
					template: '<select-list model="format" placeholder="editor.format.paragraph" display-as="label" display="format" tooltip="editor.option.format">' +
					'<opt ng-repeat="format in formats" value="format" ng-click="wrap(format)"><div bind-html="format.option"></div></opt>' +
					'</select-list>',
					link: function(scope, element, attributes){
						scope.formats = [
							{
								apply: { tag: 'p' },
								option: '<p translate content="[[format.label]]"></p>',
								label: 'editor.format.paragraph'
							},
							{
								apply: { tag: 'h1' },
								option: '<h1 translate content="[[format.label]]"></h1>',
								label: 'editor.format.title1'
							},
							{
								apply: { tag: 'h2' },
								option: '<h2 translate content="[[format.label]]"></h2>',
								label: 'editor.format.title2'
							},
							{
								apply: { tag: 'h3' },
								option: '<h3 translate content="[[format.label]]"></h3>',
								label: 'editor.format.title3'
							},
							{
								apply: { tag: 'p', classes: ['info'] },
								option: '<p class="info" translate content="[[format.label]]"></p>',
								label: 'editor.format.info'
							},
							{
								apply: { tag: 'p', classes: ['warning'] },
								option: '<p class="warning" translate content="[[format.label]]"></p>',
								label: 'editor.format.warning'
							}
						];

						instance.on('selectionchange', function (e) {
                            if(!e){
                                return;
                            }
						    var testElement = e.selection.$();
						    if (instance.selection.isEmpty()) {
						        testElement = instance.selection.elementAtCaret();
						    }
						    var found = false;
							scope.formats.forEach(function (format) {
							    var hasClass = true;
							    if (format.apply.classes) {
							        format.apply.classes.forEach(function (className) {
							            hasClass = hasClass && testElement.hasClass(className);
							        });
							    }

							    if (testElement.is(format.apply.tag) && hasClass) {
									scope.format = format;
									found = true;
								}
							});
							if(!found){
								scope.format = scope.formats[0];
							}
						});

						scope.wrap = function (format) {
						    scope.format = format;
							var newEl = $('<' + scope.format.apply.tag + '></' + scope.format.apply.tag + '>');
							if(scope.format.apply.classes){
								scope.format.apply.classes.forEach(function(element){
									newEl.addClass(element);
								});
							}

							instance.selection.wrap(newEl);
						}
					}
				}
			});

			RTE.baseToolbarConf.option('subscript', function (instance) {
			    return {
			        template: '<i tooltip="editor.option.subscript"></i>',
			        link: function (scope, element, attributes) {
			            element.on('click', function () {
			                if (!instance.editZone.is(':focus')) {
			                    instance.focus();
			                }

			                if (instance.selection.css('vertical-align') !== 'sub') {
			                    element.addClass('toggled');
			                    instance.selection.css({ 'vertical-align': 'sub', 'font-size': '12px' });
			                }
			                else {
			                    element.removeClass('toggled');
			                    instance.selection.css({ 'vertical-align': '', 'font-size': '' });
			                }
			            });

			            instance.on('selectionchange', function (e) {
			                if (instance.selection.css('vertical-align') === 'sub') {
			                    element.addClass('toggled');
			                }
			                else {
			                    element.removeClass('toggled');
			                }
			            });
			        }
			    };
			});

			RTE.baseToolbarConf.option('superscript', function (instance) {
			    return {
			        template: '<i tooltip="editor.option.superscript"></i>',
			        link: function (scope, element, attributes) {
			            element.on('click', function () {
			                if (!instance.editZone.is(':focus')) {
			                    instance.focus();
			                }

			                if (instance.selection.css('vertical-align') !== 'super') {
			                    element.addClass('toggled');
			                    instance.selection.css({ 'vertical-align': 'super', 'font-size': '12px' });
			                }
			                else {
			                    element.removeClass('toggled');
			                    instance.selection.css({ 'vertical-align': '', 'font-size': '' });
			                }
			            });

			            instance.on('selectionchange', function (e) {
			                if (instance.selection.css('vertical-align') === 'super') {
			                    element.addClass('toggled');
			                }
			                else {
			                    element.removeClass('toggled');
			                }
			            });
			        }
			    };
			});

			RTE.baseToolbarConf.option('removeFormat', function (instance) {
			    return {
			        template: '<i tooltip="editor.option.removeformat"></i>',
			        link: function (scope, element, attributes) {
			            element.on('click', function () {
			                if (!instance.editZone.is(':focus')) {
			                    instance.focus();
			                }

			                var format = {
			                    'font-style': 'normal',
			                    'background-color': 'transparent',
			                    'font-weight': 'normal',
			                    'text-decoration': 'none',
			                    'color': 'inherit',
			                    'font-size': 'initial',
			                    'line-height': 'initial',
                                'font-family': 'inherit'
			                };

			                instance.selection.css(format);
			                instance.execCommand('removeFormat');
							instance.trigger('selectionchange', { selection: instance.selection });
			            });

			            instance.on('selectionchange', function (e) {
			                if (document.queryCommandEnabled('removeFormat')) {
			                    element.removeClass('disabled');
			                }
			                else {
			                    element.addClass('disabled');
			                }
			            });
			        }
			    };
			});

			RTE.baseToolbarConf.option('image', function(instance){
				return {
				    template: '<i ng-click="imageOption.display.pickFile = true" tooltip="editor.option.image"></i>' +
					'<div ng-if="imageOption.display.pickFile">' +
                    '<lightbox show="imageOption.display.pickFile" on-close="imageOption.display.pickFile = false;">' +
					'<media-library ng-change="updateContent()" multiple="true" ng-model="imageOption.display.files" file-format="\'img\'" visibility="imageOption.visibility"></media-library>' +
					'</lightbox>' +
                    '</div>',
				    link: function (scope, element, attributes) {
				        ui.extendSelector.touchEvents('[contenteditable] img');

				        scope.imageOption = {
				            display: { pickFile: false },
				            visibility: 'protected'
				        }

						if(instance.element.attr('public')){
						    scope.imageOption.visibility = 'public'
						}

                        // border-color is a hack to track margin width; auto width is computed as 0 in FF
						instance.bindContextualMenu(scope, 'img', [
							{
							    label: 'editor.edit.image',
							    action: function (e) {
							        instance.selection.selectNode(e.target);
							        scope.imageOption.display.pickFile = true;
							    }

							},
							{
							    label: 'editor.remove.image',
							    action: function (e) {
							        $(e.target).remove();
							        instance.trigger('change');
							    }
							},
                            {
                                label: 'editor.align.right',
                                action: function (e) {
                                    $(e.target).css({ float: 'right', margin: '10px', 'z-index': '0' });
                                    instance.selection.selectNode(e.target);
                                    instance.trigger('change');
                                    instance.trigger('justify-changed');
                                }
                            },
                            {
                                label: 'editor.align.left',
                                action: function (e) {
                                    $(e.target).css({ float: 'left', margin: '10px', 'z-index': '0' });
                                    instance.selection.selectNode(e.target);
                                    instance.trigger('change');
                                    instance.trigger('justify-changed');
                                }
                            },
                            {
                                label: 'editor.align.center',
                                action: function (e) {
                                    $(e.target).css({ float: 'none', margin: 'auto', 'z-index': '1' });
                                    instance.selection.selectNode(e.target);
                                    instance.trigger('change');
                                    instance.trigger('justify-changed');
                                }
                            }
						]);

						instance.editZone.addClass('drawing-zone');
						scope.display = {};
						scope.updateContent = function () {
						    var path = '/workspace/document/';
						    if (scope.imageOption.visibility === 'public') {
						        path = '/workspace/pub/document/';
						    }
						    var html = '<div>';
						    scope.imageOption.display.files.forEach(function (file) {
						        html += '<img src="' + path + file._id + '" />';
						    });

						    html += '<div><br></div><div><br></div></div>';
						    instance.selection.replaceHTML(html);
						    instance.addState(instance.editZone.html());
							scope.imageOption.display.pickFile = false;
							scope.imageOption.display.files = [];
						    instance.focus();
						};

						instance.element.on('drop', function (e) {
                            var image;
						    if (e.originalEvent.dataTransfer.mozSourceNode) {
						        image = e.originalEvent.dataTransfer.mozSourceNode;
						    }

							//delay to account for image destruction and recreation
							setTimeout(function(){
                                if(image && image.tagName && image.tagName === 'IMG'){
                                    image.remove();
                                }
                                instance.addState(instance.editZone.html());
							    ui.extendElement.resizable(instance.editZone.find('img'), {
							        moveWithResize: false,
                                    mouseUp: function() {
                                        instance.trigger('contentupdated');
                                        instance.addState(instance.editZone.html());
                                    }
							    });
							}, 20)
						});
					}
				}
			});

			RTE.baseToolbarConf.option('attachment', function (instance) {
			    return {
			        template: '<i ng-click="attachmentOption.display.pickFile = true" tooltip="editor.option.attachment"></i>' +
					'<div ng-if="attachmentOption.display.pickFile">' +
                    '<lightbox show="attachmentOption.display.pickFile" on-close="cancel()">' +
					'<media-library ng-change="updateContent()" multiple="true" ng-model="attachmentOption.display.files" file-format="\'any\'" visibility="attachmentOption.visibility"></media-library>' +
					'</lightbox>' +
                    '</div>',
			        link: function (scope, element, attributes) {
			            element.on('mousedown', 'a', function (e) {
			                e.stopPropagation();
			                $(e.target).parents('.download-attachments')[0].dispatchEvent(e);
			            });

			            scope.attachmentOption = {
			                display: { pickFile: false },
			                visibility: 'protected'
			            }

			            if (instance.element.attr('public')) {
			                scope.attachmentOption.visibility = 'public'
			            }

			            scope.cancel = function () {
			                scope.attachmentOption.display.pickFile = false;
			            }

			            instance.bindContextualMenu(scope, '.download-attachments', [
							{
							    label: 'editor.edit.attachment',
							    action: function (e) {
							        if (!$(e.target).hasClass('download-attachments')) {
							            e.target = $(e.target).parents('.download-attachments')[0];
							        }
							        
							        instance.selection.selectNode(e.target);

							        var files = [];
							        $(e.target).find('a').each(function (index, item) {
							            var pathSplit = $(item).attr('href').split('/');
							            files.push(pathSplit[pathSplit.length - 1])
							        });
							        model.mediaLibrary.appDocuments.documents.deselectAll();
							        model.mediaLibrary.appDocuments.documents.map(function (doc) {
							            if (files.indexOf(doc._id) !== -1) {
							                doc.selected = true;
							            }
							            return doc;
							        });
							        scope.attachmentOption.display.pickFile = true;
							    }
							},
							{
							    label: 'editor.remove.attachment',
							    action: function (e) {
							        if (!$(e.target).hasClass('download-attachments')) {
							            e.target = $(e.target).parents('.download-attachments')[0];
							        }
							        $(e.target).remove();
							        instance.trigger('contentupdated');
							    }
							}
			            ]);

			            scope.display = {};
			            scope.updateContent = function () {
			                var path = '/workspace/document/';
			                if (scope.attachmentOption.visibility === 'public') {
			                    path = '/workspace/pub/document/';
			                }

			                var html = '<div class="download-attachments">' +
                                '<h2>' + lang.translate('editor.attachment.title') + '</h2>' +
                                '<div class="attachments">';
			                scope.attachmentOption.display.files.forEach(function (file) {
			                    html += '<a href="' + path + file._id + '"><div class="download"></div>' + file.name + '</a>';
			                });

			                html += '</div></div><div><br /><div><br /></div></div>';
			                instance.selection.replaceHTML(html);
			                instance.addState(instance.editZone.html());
			                scope.attachmentOption.display.pickFile = false;
			                scope.attachmentOption.display.files = [];
			                instance.focus();
			                model.mediaLibrary.appDocuments.documents.deselectAll();
			            };
			        }
			    }
			});

			RTE.baseToolbarConf.option('sound', function(instance){
				return {
				    template: '<i ng-click="soundOption.display.pickFile = true" tooltip="editor.option.sound"></i>' +
                    '<div ng-if="soundOption.display.pickFile">' +
					'<lightbox show="soundOption.display.pickFile" on-close="soundOption.display.pickFile = false;">' +
					'<media-library ng-change="updateContent()" ng-model="soundOption.display.file" file-format="\'audio\'" visibility="soundOption.visibility"></media-library>' +
					'</lightbox>' +
                    '</div>',
					link: function(scope, element, attributes){
						instance.editZone.addClass('drawing-zone');
						scope.soundOption = {
						    display: { pickFile: false },
						    visibility: 'protected'
						}

						if (instance.element.attr('public')) {
						    scope.soundOption.visibility = 'public'
						}
						scope.updateContent = function () {
						    var path = '/workspace/document/';
						    if (scope.soundOption.visibility === 'public') {
						        path = '/workspace/pub/document/';
						    }

							instance.selection.replaceHTML(
								'<div><br /></div>' +
								'<div class="audio-wrapper"><audio src="' + path + scope.soundOption.display.file._id + '" controls preload="none"></audio></div>' +
								'<div><br /></div>'
							);
							scope.soundOption.display.pickFile = false;
							scope.soundOption.display.file = undefined;
						};
					}
				}
			});

			RTE.baseToolbarConf.option('embed', function (instance) {
			    return {
			        template: '<i ng-click="display.copyEmbed = true" tooltip="editor.option.embed"></i>' +
					'<embedder ng-model="display.htmlCode" on-change="applyHtml()" show="display.copyEmbed"></embedder>',
			        link: function (scope, element, attributes) {
			            scope.display = {
			                htmlCode: ''
			            };

			            scope.applyHtml = function (template) {
							instance.selection.replaceHTML(scope.display.htmlCode);
						};
			        }
			    }
			});

			RTE.baseToolbarConf.option('mathjax', function(instance){
				return {
					template: '<i ng-click="display.fillFormula = true" tooltip="editor.option.mathjax"></i>' +
					'<lightbox show="display.fillFormula" on-close="display.fillFormula = false;">' +
					'<textarea ng-model="display.formula"></textarea>' +
					'<mathjax formula="[[display.formula]]"></mathjax>' +
					'<div class="row">' +
					'<button type="button" ng-click="updateContent()" class="right-magnet"><i18n>apply</i18n></button>' +
					'<button type="button" ng-click="cancel()" class="right-magnet cancel"><i18n>cancel</i18n></button>' +
					'</div>' +
					'</lightbox>',
					link: function(scope, element, attributes){
						scope.display = {
							formula: '{-b \\pm \\sqrt{b^2-4ac} \\over 2a}'
						};
                        
                        var editNode = undefined;
                        
						scope.updateContent = function(){
                            if(editNode){
                                $(editNode).attr('formula', scope.display.formula);
                                angular.element(editNode.firstChild).scope().updateFormula(scope.display.formula);
				instance.trigger('change');
                            }
                            else{
                                instance.selection.replaceHTMLInline(instance.compile(
                                    '<span>&nbsp;<mathjax contenteditable="false" formula="'+ scope.display.formula + '"></mathjax>&nbsp;</span>'
                                )(scope));
                            }
							
							scope.display.fillFormula = false;
                            editNode = undefined;
						};
                        
                        scope.cancel = function(){
                            editNode = undefined;
                            scope.display.fillFormula = false;
                        };
                        
                        instance.bindContextualMenu(scope, 'mathjax', [
							{
							    label: 'editor.edit.mathjax',
							    action: function (e) {
							        instance.selection.selectNode(e.target);
							        scope.display.fillFormula = true;
                                    scope.display.formula = $(e.target).attr('formula');
							        editNode = e.target;
							    }

							},
							{
							    label: 'editor.remove.mathjax',
							    action: function (e) {
							        $(e.target).remove();
							    }
							}
						]);
					}
				}
			});

			RTE.baseToolbarConf.option('linker', function(instance){
				return {
					template: '<i ng-click="linker.openLinker()" tooltip="editor.option.link"></i>' +
					'<div ng-include="\'/infra/public/template/linker.html\'"></div>',
					link: function (scope, element, attributes) {
					    ui.extendSelector.touchEvents('[contenteditable] a');
						scope.linker = {
							display: {},
							apps: [],
							search: {
								application: {},
								text: ''
							},
							params: {},
							resource: {}
						};

						instance.bindContextualMenu(scope, 'a', [
							{
							    label: 'editor.edit.link',
							    action: function (e) {
							        instance.selection.selectNode(e.target);
							        scope.linker.display.chooseLink = true;
							        scope.linker.openLinker($(e.target).data('app-prefix'), $(e.target).attr('href'), e.target);
							    }

							},
							{
							    label: 'editor.remove.link',
							    action: function (e) {
							        var content = document.createTextNode($(e.target).text());
							        e.target.parentNode.insertBefore(content, e.target);
							        $(e.target).remove();
							    }
							}
						]);

						scope.linker.openLinker = function (appPrefix, address, element) {
						    var sel = window.getSelection();
						    instance.selection.range = sel.getRangeAt(0);
						    scope.linker.display.chooseLink = true;
						    if (appPrefix) {
						        scope.linker.search.application.address = '/' + appPrefix;
						        scope.linker.loadApplicationResources(function () {
						            scope.linker.searchApplication(function () {
						                var resource = _.findWhere(scope.linker.resources, { path: address });
						                scope.linker.applyResource(resource);
						                scope.$apply();
						            });
						        });
						    }
						    else {
						        if (!element && instance.selection.selectedElements.length === 1) {
						            element = instance.selection.selectedElements[0];
                                }
						        if (element) {
						            if (element.nodeType !== 1) {
						                element = element.parentNode;
						            }
						            if (element.nodeName === 'A') {
						                var link = $(element).attr('href');
						                scope.linker.params.blank = $(element).attr('target') === '_blank';
						                scope.linker.params.tooltip = $(element).attr('tooltip') || '';

						                if (link.split('http')[0] === '' && link.split('http').length > 1) {
						                    scope.linker.externalLink = true;
						                    scope.linker.params.link = link;
						                }
						                else {
						                    scope.linker.externalLink = false;
						                    scope.linker.openLinker(link.split('/')[1].split('#')[0], link);
						                }
						            }
						        }
						    }
						};

						scope.linker.loadApplicationResources = function(cb){
							var split = scope.linker.search.application.address.split('/');
							var prefix = split[split.length - 1];
							scope.linker.params.appPrefix = prefix;
							if(!cb){
								cb = function(){
									scope.linker.searchApplication();
									scope.$apply('linker');
								};
							}
							Behaviours.loadBehaviours(scope.linker.params.appPrefix, function (appBehaviour) {
							    Behaviours.applicationsBehaviours[prefix].loadResources(cb);
							    scope.linker.addResource = Behaviours.applicationsBehaviours[prefix].create;
							});
						};

						scope.linker.searchApplication = function(cb){
							var split = scope.linker.search.application.address.split('/');
							var prefix = split[split.length - 1];
							scope.linker.params.appPrefix = prefix;
							Behaviours.loadBehaviours(scope.linker.params.appPrefix, function(appBehaviour){
								scope.linker.resources = _.filter(appBehaviour.resources, function(resource) {
									return scope.linker.search.text !== '' && (lang.removeAccents(resource.title.toLowerCase()).indexOf(lang.removeAccents(scope.linker.search.text).toLowerCase()) !== -1 ||
										resource._id === scope.linker.search.text);
								});
								if(typeof cb === 'function'){
									cb();
								}
							});
						};

						scope.linker.createResource = function(){
							Behaviours.loadBehaviours(scope.linker.params.appPrefix, function(appBehaviour){
								appBehaviour.create(scope.linker.resource, function(){
									scope.linker.search.text = scope.linker.resource.title;
									
									scope.linker.searchApplication();
									scope.$apply();
								});
							});
						};

						scope.linker.applyLink = function(link){
							scope.linker.params.link = link;
						};

						scope.linker.applyResource = function(resource){
							scope.linker.params.link = resource.path;
							scope.linker.params.id = resource._id;
						};

						scope.linker.saveLink = function(){
							if(scope.linker.params.blank){
								scope.linker.params.target = '_blank';
							}

							var linkNode;
							var selectedNode = instance.selection.range.startContainer;
							if (selectedNode && selectedNode.nodeType !== 1
                                && selectedNode.parentNode.childNodes.length === 1
                                && instance.selection.range.startOffset === 0
                                && instance.selection.range.endOffset === selectedNode.textContent.length) {
                                selectedNode = selectedNode.parentNode;
                            }
							if (selectedNode && selectedNode.nodeName === 'A') {
							    linkNode = $(selectedNode);
							}
							else {
							    linkNode = $('<a></a>');
							}

							if(scope.linker.params.link){
								linkNode.attr('href', scope.linker.params.link);

								if (scope.linker.params.appPrefix && !scope.linker.externalLink) {
									linkNode.attr('data-app-prefix', scope.linker.params.appPrefix);
									if(scope.linker.params.appPrefix !== 'workspace' && !scope.linker.externalLink){
										linkNode.data('reload', true);
									}
								}
								if(scope.linker.params.id){
									linkNode.attr('data-id', scope.linker.params.id);
								}
								if(scope.linker.params.blank){
									scope.linker.params.target = '_blank';
									linkNode.attr('target', scope.linker.params.target);
								}
								if(scope.linker.params.tooltip){
									linkNode.attr('tooltip', scope.linker.params.tooltip);
								}
								else if(linkNode.attr('tooltip')){
									linkNode.removeAttr('tooltip');
									linkNode.off('mouseover');
								}
							}

							if (selectedNode && selectedNode.nodeName === 'A') {
							    instance.selection.moveCaret(linkNode[0], linkNode.text().length);
							    instance.trigger('change');
							    scope.linker.display.chooseLink = false;
							    scope.linker.params = {};
							    scope.linker.display.search = {
							        application: {},
							        text: ''
							    };
							    scope.linker.externalLink = false;
							    return;
							}

							if (instance.selection.isCursor()) {
							    linkNode.text(scope.linker.params.link);
							    instance.selection.replaceHTMLInline(instance.compile(linkNode[0].outerHTML)(scope));
							}
							else {
							    instance.selection.wrapText(linkNode);
							}

							instance.focus();
							scope.linker.display.chooseLink = false;
							scope.linker.params = {};
							scope.linker.display.search = {
							    application: {},
							    text: ''
							};
							scope.linker.externalLink = false;
						};

						scope.linker.cancel = function(){
						    scope.linker.display.chooseLink = false;
						    scope.linker.params = {};
						    scope.linker.display.search = {
						        application: {},
						        text: ''
						    };
						    scope.linker.externalLink = false;
						};

						http().get('/resources-applications').done(function(apps){
							scope.linker.apps = _.filter(model.me.apps, function(app){
                                return _.find(
                                    apps,
                                    function (match) {
                                        return app.address.indexOf(match) !== -1 && app.icon && app.address.indexOf('#') === -1
                                    }
                                );
							});

						    scope.linker.apps = _.map(scope.linker.apps, function(app) {
						        app.displayName = lang.translate(app.displayName);
						        return app;
						    });

							scope.linker.search.application = _.find(scope.linker.apps, function(app){ return app.address.indexOf(appPrefix) !== -1 });
							if(!scope.linker.search.application){
								scope.linker.search.application = scope.linker.apps[0];
								scope.linker.searchApplication(function(){
									scope.linker.loadApplicationResources(function(){});
								})
							}
							else{
								scope.linker.loadApplicationResources(function(){});
							}

							scope.$apply('linker');
						});
					}
				}
			});

			RTE.baseToolbarConf.option('unlink', function(instance){
				return {
					template: '<i tooltip="editor.option.unlink"></i>',
					link: function(scope, element, attributes){
						element.addClass('disabled');
						element.on('click', function () {
						    var currentNode = instance.selection.range.startContainer;
						    if (currentNode.nodeType !== 1) {
						        currentNode = currentNode.parentNode;
						    }
						    if (currentNode.nodeName !== 'A') {
						        return;
						    }
						    var content = document.createTextNode($(currentNode).text());
						    currentNode.parentNode.insertBefore(content, currentNode);
						    $(currentNode).remove();
							element.addClass('disabled');
						});

						instance.on('selectionchange', function (e) {
						    var currentNode = e.selection.range.startContainer;
						    if(currentNode.nodeType !==1){
						        currentNode = currentNode.parentNode;
						    }
						    if (currentNode.nodeName === 'A') {
								element.removeClass('disabled');
							}
							else{
								element.addClass('disabled');
							}
						});
					}
				};
			});

			RTE.baseToolbarConf.option('smileys', function(instance){
				return {
					template: '' +
					'<i tooltip="editor.option.smileys"></i>' +
					'<lightbox show="display.pickSmiley" on-close="display.pickSmiley = false;">' +
					'<h2>Insérer un smiley</h2>' +
					'<div class="row smileys">' +
					'<img ng-repeat="smiley in smileys" class="smiley" ng-click="addSmiley(smiley)" skin-src="/img/smileys/[[smiley]].png" />' +
					'</div>' +
					'</lightbox>',
					link: function(scope, element, attributes){
						scope.display = {};
						scope.smileys = [ "happy", "proud", "dreamy", "love", "tired", "angry", "worried", "sick", "joker", "sad" ];
						scope.addSmiley = function (smiley) {
						    //do not replace with i, as i is used by other websites for italic and
                            //is often copy-pasted in the editor
						    var content = instance.compile(
                                '<img skin-src="/img/smileys/' + smiley + '.png" class="smiley" />'
                            )(scope.$parent);
							instance.selection.replaceHTMLInline(content);
							scope.display.pickSmiley = false;
						}

						element.children('i').on('click', function(){
							scope.display.pickSmiley = true;
						});
					}
				};
			});

			RTE.baseToolbarConf.option('table', function(instance){
				return {
					template: '' +
					'<popover mouse-event="click">' +
					'<i popover-opener opening-event="click" tooltip="editor.option.table"></i>' +
					'<popover-content>' +
					'<div class="draw-table"></div>' +
					'</popover-content>' +
					'</popover>',
					link: function(scope, element, attributes){
						var nbRows = 12;
						var nbCells = 12;
						var drawer = element.find('.draw-table');
						for(var i = 0; i < nbRows; i++){
							var line = $('<div class="row"></div>');
							drawer.append(line);
							for(var j = 0; j < nbCells; j++){
								line.append('<div class="one cell"></div>');
							}
						}

						ui.extendSelector.touchEvents('[contenteditable] td');

                        element.find('i').on('click', function(){
                            if (element.find('popover-content').hasClass('hidden')) {
						        setTimeout(function () {
						            element.parents('editor-toolbar').each(function(index, item) {
                                        $(item).css({
                                            'margin-top': '-' + item.scrollTop + 'px',
                                            'min-height': '0',
                                            'height': 'auto'
                                        })
						            });
						            element.parents().css({
						                 overflow: 'visible'
						            });
						        }, 0);
							}
						    else {
						        element.parents().css({ overflow: '' });
						        element.parents('editor-toolbar').each(function (index, item) {
						            $(item).css({ 'margin-top': '', 'min-height': '', height: '' })
						        });
							}
                        })

						drawer.find('.cell').on('mouseover', function(){
							var line = $(this).parent();
							for(var i = 0; i <= line.index(); i++){
								var row = $(drawer.find('.row')[i]);
								for(var j = 0; j <= $(this).index(); j++){
									var cell = $(row.find('.cell')[j]);
									cell.addClass('match');
								}
							}
						});

						drawer.find('.cell').on('mouseout', function(){
							drawer.find('.cell').removeClass('match');
						});

						drawer.find('.cell').on('click', function(){
							var table = document.createElement('table');
							var line = $(this).parent();
							for(var i = 0; i <= line.index(); i++){
								var row = $('<tr></tr>');
								$(table).append(row);
								for(var j = 0; j <= $(this).index(); j++){
									var cell = $('<td></td>');
									cell.html('<br />')
									row.append(cell);
								}
							}
							instance.selection.replaceHTML('<div>' + table.outerHTML + '</div>');
							instance.trigger('contentupdated');
						});

						instance.bindContextualMenu(scope, 'td', [
							{
								label: 'editor.add.row',
								action: function(e){
									var newRow = $($(e.target).parent()[0].outerHTML);
									newRow.find('td').html('<br />');
									$(e.target).parent().after(newRow);
								}

							},
							{
								label: 'editor.add.column',
								action: function(e){
									var colIndex = $(e.target).index();
									$(e.target).parents('table').find('tr').each(function(index, row){
										$(row).children('td').eq(colIndex).after('<td><br /></td>')
									});
								}
							},
							{
								label: 'editor.remove.row',
								action: function(e){
									$(e.target).parent().remove();
								}
							},
							{
								label: 'editor.remove.column',
								action: function(e){
									var colIndex = $(e.target).index();
									$(e.target).parents('table').find('tr').each(function(index, row){
										$(row).children('td').eq(colIndex).remove();
									});
								}
							}
						]);
					}
				}
			});

			RTE.baseToolbarConf.option('templates', function(instance){
				return {
					template: '<i tooltip="editor.option.templates"></i>' +
					'<lightbox show="display.pickTemplate" on-close="display.pickTemplate = false;">' +
					'<h2><i18n>editor.option.templates</i18n></h2>' +
					'<ul class="thought-out-actions">' +
					'<li ng-repeat="template in templates" ng-click="applyTemplate(template)">' +
					    '<img ng-src="[[template.image]]" class="cell" />' +
					    '<div class="cell vertical-spacing horizontal-spacing" translate content="[[template.title]]"></div>' +
					'</li>' +
					'</ul>' +
					'</lightbox>',
					link: function (scope, element, attributes) {
					    var skinPath = skin.basePath + '../entcore-css-lib/editor-resources/img/';
						scope.templates = [
							{
							    title: 'editor.templates.emptypage.title',
							    image: skinPath + 'templates-preview-emptypage.svg',
								html: '<div class="twelve cell column"><article></article></div>'
							},
							{
							    title: 'editor.templates.twocols.title',
							    image: skinPath + 'templates-preview-twocols.svg',
								html:
								'<div class="row">' +
								'<div class="six cell column">' +
									'<article>' +
										'<h2>' +
                                        lang.translate('editor.templates.coltitle') +
                                        '</h2>' +
										'<p>' +
                                        lang.translate('editor.templates.colfiller') +
                                        '</p>' +
									'</article>' +
								'</div>' +
								'<div class="six cell column">' +
									'<article>' +
										'<h2>' +
                                        lang.translate('editor.templates.coltitle') +
                                        '</h2>' +
										'<p>' +
                                        lang.translate('editor.templates.colfiller') +
                                        '</p>' +
									'</article>' +
								'</div>' +
								'</div>'
							},
							{
							    title: 'editor.templates.threecols.title',
							    image: skinPath + 'templates-preview-threecols.svg',
								html:
								'<div class="row">' +
								'<div class="four cell column">' +
									'<article>' +
										'<h2>' +
                                        lang.translate('editor.templates.coltitle') +
                                        '</h2>' +
										'<p>' +
                                        lang.translate('editor.templates.colfiller') +
                                        '</p>' +
									'</article>' +
								'</div>' +
								'<div class="four cell column">' +
									'<article>' +
										'<h2>' +
                                        lang.translate('editor.templates.coltitle') +
                                        '</h2>' +
										'<p>' +
                                        lang.translate('editor.templates.colfiller') +
                                        '</p>' +
									'</article>' +
								'</div>' +
								'<div class="four cell column">' +
									'<article>' +
										'<h2>' +
                                        lang.translate('editor.templates.coltitle') +
                                        '</h2>' +
										'<p>' +
                                        lang.translate('editor.templates.colfiller') +
                                        '</p>' +
									'</article>' +
								'</div>' +
								'</div>'
							},
							{
							    title: 'editor.templates.illustration.title',
							    image: skinPath + 'templates-preview-illustration.svg',
								html:
								'<div class="row">' +
									'<div class="three cell column">' +
										'<article>' +
											'<img src="' + skinPath + 'image-default.svg" />' +
										'</article>' +

									'</div>' +
									'<div class="nine cell column">' +
										'<article>' +
											'<h2>' +
                                                lang.translate('editor.templates.illustration.titlefiller') +
                                            '</h2>' +
											'<p>' +
                                            lang.translate('editor.templates.illustration.textfiller') +
                                            '</p>' +
										'</article>' +
									'</div>' +
								'</div>'
							},
							{
							    title: 'editor.templates.dominos.title',
							    image: skinPath + 'templates-preview-dominos.svg',
								html:
								'<div class="dominos">' +
									'<div class="item">' +
										'<section class="domino pink">' +
										'<div class="top">' +
											'<img src="' + skinPath + 'image-default.svg" class="fixed twelve cell" />' +
										'</div>' +
										'<div class="bottom">' +
											'<div class="content">' +
                                                lang.translate('editor.templates.dominos.textfiller') +
											'</div>' +
										'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino blue">' +
											'<div class="top">' +
												'<img src="' + skinPath + 'image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
											'<div class="bottom">' +
												'<div class="content">' +
													lang.translate('editor.templates.dominos.textfiller') +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino orange">' +
											'<div class="top">' +
												'<img src="' + skinPath + 'image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
												'<div class="bottom">' +
												'<div class="content">' +
													lang.translate('editor.templates.dominos.textfiller') +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino purple">' +
											'<div class="top">' +
												'<img src="' + skinPath + 'image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
											'<div class="bottom">' +
												'<div class="content">' +
													lang.translate('editor.templates.dominos.textfiller') +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino green">' +
											'<div class="top">' +
												'<img src="' + skinPath + 'image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
											'<div class="bottom">' +
												'<div class="content">' +
													lang.translate('editor.templates.dominos.textfiller') +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino white">' +
											'<div class="top">' +
												'<img src="' + skinPath + 'image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
												'<div class="bottom">' +
												'<div class="content">' +
													lang.translate('editor.templates.dominos.textfiller') +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
								'</div>'
							}
						];
						scope.display = {};
						scope.applyTemplate = function(template){
							scope.display.pickTemplate = false;
							instance.selection.replaceHTML(_.findWhere(scope.templates, { title: template.title}).html);
						};

						element.children('i').on('click', function(){
							scope.display.pickTemplate = true;
							scope.$apply('display');
						});
					}
				}
			});

			//Editor
			module.directive('editor', function($parse, $compile) {
			    return {
			        restrict: 'E',
			        template: '' +
			            '<button type="button" class="editor-toolbar-opener"></button>' +
			            '<button type="button" class="close-focus">OK</button>' +
			            '<editor-toolbar></editor-toolbar>' +
			            '<contextual-menu><ul></ul></contextual-menu>' +
			            '<popover>' +
			            '<i class="tools" popover-opener opening-event="click"></i>' +
			            '<popover-content>' +
			            '<ul>' +
			            '<li><i18n>editor.mode.wysiwyg</i18n></li>' +
			            '<li><i18n>editor.mode.html</i18n></li>' +
			            '<li><i18n>editor.mode.mixed</i18n></li>' +
			            '</ul>' +
			            '</popover-content>' +
			            '</popover>' +
			            '<div><div contenteditable="true"></div></div>' +
			            '<textarea></textarea>' +
			            '<code class="language-html"></code>',
			        link: function (scope, element, attributes) {
			            if (navigator.userAgent.indexOf('Trident') !== -1 || navigator.userAgent.indexOf('Edge') !== -1) {
			                element.find('code').hide();
			            }
			            $('body').append(
                            $('<link />')
                                .attr('rel', 'stylesheet')
                                .attr('type', 'text/css')
                                .attr('href', '/infra/public/js/prism/prism.css')
                       );

			            loader.openFile({
			                url: '/infra/public/js/prism/prism.js',
			                callback: function () {

			                },
			                error: function () {

			                }
			            });

			            element.find('.close-focus').on('click', function(){
			                element.removeClass('focus');
			                element.parent().data('lock', false);
			                element.parents('grid-cell').data('lock', false);
                            element.trigger('editor-blur');
                            $('body').css({ overflow: 'auto' });
                        });

                        element.find('.editor-toolbar-opener').on('click', function(e){
                            if(!$(this).hasClass('active')){
                                $(this).addClass('active');
                                element.find('editor-toolbar').addClass('opened');
                            }
                            else{
                                $(this).removeClass('active')
                                element.find('editor-toolbar').removeClass('opened');
                            }
                        });
                        
                        element.find('.editor-toolbar-opener').on('touchstart', function(e){
							e.preventDefault();
							if(!$(this).hasClass('active')){
                                $(this).addClass('active');
                                element.find('editor-toolbar').addClass('opened');
                            }
                            else{
                                $(this).removeClass('active')
                                element.find('editor-toolbar').removeClass('opened');
                            }
                            setTimeout(function(){
                                var sel = window.getSelection();
                                sel.removeAllRanges();
                                sel.addRange(editorInstance.selection.range);
                            }, 100);
                        });
                        
                        document.execCommand("enableObjectResizing", false, false);
                        document.execCommand("enableInlineTableEditing", null, false);
                        document.execCommand("insertBrOnReturn", false, true);

                        element.addClass('edit');
                        var editZone = element.find('[contenteditable=true]');
                        var htmlZone = element.children('textarea');
                        var highlightZone = element.children('code');
                        var toolbarElement = element.children('editor-toolbar');
			            document.execCommand("styleWithCSS", false, true);

                        if(attributes.inline !== undefined){
                            element.children('editor-toolbar').addClass('inline');
                        }

                        var toolbarConf = RTE.baseToolbarConf;
                        if(attributes.toolbarConf){
                            toolbarConf = scope.$eval(attributes.toolbarConf);
                        }
						
						var editorInstance;
						var instance = $parse(attributes.instance);
						if(!instance(scope)){
							editorInstance = new RTE.Instance({
								toolbarConfiguration: toolbarConf,
								element: element,
								scope: scope,
								compile: $compile,
								editZone: editZone
							});
						}
						else{
							editorInstance = instance;
						}

			            editorInstance.addState('');
                        var ngModel = $parse(attributes.ngModel);
                        if(!ngModel(scope)){
                            ngModel.assign(scope, '');
                        }

                        scope.$watch(
                            function () {
                                return ngModel(scope);
                            },
                            function (newValue) {
                                $(newValue).find('.math-tex').each(function (index, item) {
                                    var mathItem = $('<mathjax></mathjax>');
                                    mathItem.attr('formula', item.textContent.replace('\\(', '$$$$').replace('\\)', '$$$$').replace('x = ', ''));
                                    $(item).removeClass('math-tex');
                                    $(item).text('');
                                    $(item).append(mathItem);
                                });

                                if (
                                    newValue !== editZone.html() &&
                                    !editZone.is(':focus') &&
                                    $('editor-toolbar').find(':focus').length === 0
                                ) {
                                    editZone.html($compile(ngModel(scope))(scope));
                                }
                                if(newValue !== htmlZone.val() && !htmlZone.is(':focus')){
                                    if(window.html_beautify){
                                        htmlZone.val(html_beautify(newValue));
                                        highlightZone.text(html_beautify(newValue));
                                        Prism.highlightAll();
                                    }
                                    //beautifier is not loaded on mobile
                                    else{
                                        htmlZone.val(newValue);
                                    }
                                }
                            }
                        );

                        $(window).on('resize', function () {
                            highlightZone.css({ top: (element.find('editor-toolbar').height() + 1) + 'px' });
                        });

                        var previousScroll = 0;
                        function sticky() {
							if(element.parents('.editor-media').length > 0){
								return;
							}
							
							if(toolbarElement.css('position') !== 'absolute'){
								toolbarElement.css({ 'position': 'absolute', 'top': '0px' });
								element.css({ 'padding-top': toolbarElement.height() + 1 + 'px' });
							}
							var topDistance = element.offset().top - $('.height-marker').height();
							if (topDistance < (window.scrollY || window.pageYOffset)) {
							    topDistance = (window.scrollY || window.pageYOffset);
                            }
                            if (topDistance > editZone.offset().top + editZone.height() - toolbarElement.height()) {
                                topDistance = editZone.offset().top + editZone.height() - toolbarElement.height();
                            }
                            if (attributes.inline !== undefined) {
                                toolbarElement.offset({
                                    top: topDistance + $('.height-marker').height()
                                });
                                element.children('popover').offset({
                                    top: topDistance + $('.height-marker').height() + 10 - parseInt(element.css('margin-top'))
                                });
                            }
                            else {
                                toolbarElement.offset({
                                    top: topDistance + $('.height-marker').height()
                                });
                                element.children('popover').offset({
                                    top: topDistance + $('.height-marker').height() + 10
                                });
                            }
                            
                            setTimeout(function () {
                                highlightZone.offset({ top: htmlZone.offset().top });
                            }, 100);

                            previousScroll = (window.scrollY || window.pageYOffset);

                            var placeEditorToolbar = requestAnimationFrame(sticky);
                        }

						if(ui.breakpoints.tablette <= $(window).width()){
							var placeEditorToolbar = requestAnimationFrame(sticky);
						}

                        element.children('popover').find('li:first-child').on('click', function(){
                            element.removeClass('html');
                            element.removeClass('both');
                            element.addClass('edit');
                            editorInstance.trigger('contentupdated');
                        });

                        element.children('popover').find('li:nth-child(2)').on('click', function(){
                            element.removeClass('edit');
                            element.removeClass('both');
                            element.addClass('html');
                            highlightZone.css({ top: (element.find('editor-toolbar').height() + 1) + 'px' });
                            editorInstance.trigger('contentupdated');
                            setTimeout(function () {
                                editorInstance.trigger('contentupdated');
                            }, 300);
                            if(window.html_beautify){
                                return;
                            }
                            http().get('/infra/public/js/beautify-html.js').done(function(content){
                                eval(content);
                                htmlZone.val(html_beautify(ngModel(scope)));
                                highlightZone.text(html_beautify(ngModel(scope)));
                                Prism.highlightAll();
                            });
                        });

                        element.children('popover').find('li:nth-child(3)').on('click', function(){
                            element.removeClass('edit');
                            element.removeClass('html');
                            element.addClass('both');
                            highlightZone.css({ top: (element.find('editor-toolbar').height() + 1) + 'px' });
                            editorInstance.trigger('contentupdated');
                            setTimeout(function () {
                                editorInstance.trigger('contentupdated');
                            }, 300);
                            if(window.html_beautify){
                                return;
                            }
                            http().get('/infra/public/js/beautify-html.js').done(function(content){
                                eval(content);
                                htmlZone.val(html_beautify(ngModel(scope)));
                                highlightZone.text(html_beautify(ngModel(scope)));
                                Prism.highlightAll();
                            });
                        });

                        function b64toBlob(b64Data, contentType, sliceSize) {
                            contentType = contentType || '';
                            sliceSize = sliceSize || 512;

                            var byteCharacters = atob(b64Data);
                            var byteArrays = [];

                            for (var offset = 0; offset < byteCharacters.length; offset += sliceSize) {
                                var slice = byteCharacters.slice(offset, offset + sliceSize);

                                var byteNumbers = new Array(slice.length);
                                for (var i = 0; i < slice.length; i++) {
                                byteNumbers[i] = slice.charCodeAt(i);
                                }

                                var byteArray = new Uint8Array(byteNumbers);

                                byteArrays.push(byteArray);
                            }

                            var blob = new Blob(byteArrays, { type: contentType });
                            blob.name = "image";
                            return blob;
                        }

                        element.find('.option i').click(function(){
                            if(!editZone.is(':focus')){
                                editZone.focus();
                            }

                            scope.$apply(function(){
                                scope.$eval(attributes.ngChange);
                                ngModel.assign(scope, editZone.html());
                            });
                        });

						editorInstance.on('change', function(){
							editorInstance.trigger('contentupdated');
							setTimeout(function(){
								
								if(attributes.onChange){
									scope.$eval(attributes.onChange);
								}
								
								scope.$apply();
							}, 10);
						});

                        editorInstance.on('contentupdated', function () {
                            if(parseInt(htmlZone.css('min-height')) < editZone.height()){
                                htmlZone.css('min-height', editZone.height() + 'px');
                            }
                            ui.extendElement.resizable(element.find('[contenteditable]').find('img, table, .column'), {
                                moveWithResize: false,
                                lock: {
                                    left: true,
                                    top: true
                                },
                                mouseUp: function() {
                                    editorInstance.trigger('contentupdated');
                                    editorInstance.addState(editorInstance.editZone.html());
                                }
                            });
                            htmlZone.css({ 'min-height': '250px', height: 0 });
                            var newHeight = htmlZone[0].scrollHeight + 2;
                            if(newHeight > htmlZone.height()){
                                htmlZone.height(newHeight);
                            }

                            if (htmlZone[0].scrollHeight > parseInt(htmlZone.css('min-height')) && !element.hasClass('edit')) {
                                editZone.css('min-height', htmlZone[0].scrollHeight + 2 + 'px');
                            }

                            if(editorInstance.selection.changed()){
                                editorInstance.trigger('selectionchange', {
                                    selection: editorInstance.selection
                                });
                            }

                            editZone.find('img').each(function(index, item){
                               if($(item).attr('src').startsWith('data:')){
                                   var split = $(item).attr('src').split('data:')[1].split(',');
                                   var blob = b64toBlob(split[1], split[0].split(';')[0]);
								   blob.name = 'image';
                                   $(item).attr('src', 'http://loading');
                                   workspace.Document.prototype.upload(blob, '', function(file){
                                       $(item).attr('src', '/workspace/document/' + file._id);
                                       notify.info('editor.b64.uploaded');
                                       editorInstance.trigger('contentupdated');
                                   });
                               }
                            });

                            if (!scope.$$phase) {
                                scope.$apply(function () {
                                    scope.$eval(attributes.ngChange);
                                    var content = editZone.html();
                                    ngModel.assign(scope, content);
                                });
                            }
                            else {
                                scope.$eval(attributes.ngChange);
                                var content = editZone.html();
                                ngModel.assign(scope, content);
                            }
                        });

                        var placeToolbar = function () {
                            if (attributes.inline !== undefined && $(window).width() > ui.breakpoints.tablette) {
                                element.children('editor-toolbar').css({
                                    left: 0
                                });
                                element.css({ 'padding-top': toolbarElement.height() + 1 + 'px' });
                            }
                        }

                        element.parents().on('resizing', placeToolbar)
                        element.on('click', function(e){
                            placeToolbar();

                            if(e.target === element.find('.close-focus')[0] || element.hasClass('focus')){
                                return;
                            }

                            element.trigger('editor-focus');
                            element.addClass('focus');
                            element.parent().data('lock', true);
                            element.parents('grid-cell').data('lock', true);
                            if ($(window).width() < ui.breakpoints.tablette) {
                                $('body').css({ overflow: 'hidden' });
								window.scrollTo(0,0);
								setTimeout(function(){
									var sel = document.getSelection();
									var r = document.createRange();
									r.setStart(editZone[0].firstChild, 0);
									sel.removeAllRanges();
									sel.addRange(r);
								}, 600);
                            }
                        });

                        $('body').on('mousedown', function(e){
                            if(e.target !== element.find('.editor-toolbar-opener')[0] && element.find('editor-toolbar, .editor-toolbar-opener').find(e.target).length === 0){
                                element.find('editor-toolbar').removeClass('opened');
                                element.find('.editor-toolbar-opener').removeClass('active');
                            }

                            if (element.find(e.target).length === 0 && !$(e.target).hasClass('sp-choose') && element.hasClass('focus')) {
                                element.children('editor-toolbar').removeClass('show');
                                element.trigger('editor-blur');
                                element.removeClass('focus');
								editorInstance.trigger('change');
                                $('body').css({ overflow: 'auto' });
                                element.parent().data('lock', false);
                                element.parents('grid-cell').data('lock', false);
                                element.find('code').attr('style', '');

                                if(attributes.inline !== undefined){
                                    element.css({
                                        'margin-top': 0,
                                        'padding-top': 0
                                    });
                                    element.children('editor-toolbar').attr('style', '');
                                }
                            }
                        });

                        $('editor-toolbar').on('mousedown', function(e){
                            e.preventDefault();
                        });

                        function wrapFirstLine() {
                            if (editZone.contents()[0] && editZone.contents()[0].nodeType === 3) {
                                var div = $('<div></div>');
                                div.text(editZone.contents()[0].textContent);
                                $(editZone.contents()[0]).remove();
                                editZone.prepend(div);
                                editorInstance.selection.moveCaret(div[0], div.text().length);
                                editorInstance.trigger('contentupdated');
                            }
                        }

                        function editingDone(){
                            editorInstance.addState(editZone.html());
                        }

                        var typingTimer;
                        var editingTimer;

                        editZone.on('paste', function () {
							setTimeout(function(){
								editorInstance.editZone.find('[resizable]').removeAttr('resizable').css('cursor', 'initial');
								editorInstance.editZone.find('[bind-html]').removeAttr('bind-html');
								editorInstance.editZone.find('[ng-include]').removeAttr('ng-include');
								editorInstance.editZone.find('[ng-repeat]').removeAttr('ng-repeat');
								editorInstance.editZone.find('[data-ng-repeat]').removeAttr('data-ng-repeat');
								editorInstance.editZone.find('[ng-transclude]').removeAttr('ng-transclude');
								if(editorInstance.editZone.find('portal').length){
									var portal = editorInstance.editZone.find('portal');
									editorInstance.editZone[0].innerHTML = $('<div>' + (portal.find('[bind-html]').html() || '') + '</div>')
								}
								editorInstance.addState(editZone.html());
								if(editorInstance.editZone[0].childNodes.length > editorInstance.editZone[0].children.length){
									var wrapper = $('<div></div>');
									while(editorInstance.editZone[0].childNodes.length){
										wrapper.append(editorInstance.editZone[0].childNodes[0]);
									}
									editorInstance.editZone.append(wrapper);
								}
								editorInstance.trigger('contentupdated');
								editorInstance.scope.$apply();
							}, 100);
                        });

                        editZone.on('keydown', function (e) {
                            clearTimeout(typingTimer);
                            clearTimeout(editingTimer);
                            typingTimer = setTimeout(wrapFirstLine, 10);
                            
                            var sel = window.getSelection();
                            if (sel.rangeCount > 0) {
                                var range = sel.getRangeAt(0);
                                if (range.startContainer.nodeType !== 1 && e.which > 64 && e.which < 91 && range.startContainer.parentNode !== null) {     
                                    var currentTextNode = range.startContainer;
                                    var initialOffset = range.startOffset;
                                    if (initialOffset === currentTextNode.textContent.length) {
                                        initialOffset = -1;
                                    }
                                    if (range.startContainer.parentNode.innerHTML === '&#8203;' && range.startOffset === 1) {
                                        var node = range.startContainer.parentNode;
                                        
                                        setTimeout(function () {
                                            node.innerHTML = node.innerHTML.substring(7);
                                            setTimeout(function () {
                                                var range = document.createRange();
                                                if (initialOffset === -1) {
                                                    initialOffset = (node.firstChild || node).textContent.length
                                                }
                                                range.setStart((node.firstChild || node), initialOffset);
                                                sel.removeAllRanges();
                                                sel.addRange(range);
                                            }, 1);
                                        }, 1);
                                        
                                    }
                                }
                            }
                            

                            if (!e.ctrlKey) {
                                editingTimer = setTimeout(editingDone, 500);
                            }

                            if (e.keyCode === 13) {
                                editorInstance.addState(editZone.html());
                                
                                var parentContainer = range.startContainer;

                                if (
                                        (parentContainer.nodeType === 1 && parentContainer.nodeName === 'LI') ||
                                        (parentContainer.parentNode.nodeType === 1 && parentContainer.parentNode.nodeName === 'LI') ||
                                        (parentContainer.nodeType === 1 && parentContainer.nodeName === 'TD') ||
                                        (parentContainer.parentNode.nodeType === 1 && parentContainer.parentNode.nodeName === 'TD')
                                    ) {
                                    return;
                                }

                                var blockContainer = parentContainer;
                                while (blockContainer.nodeType !== 1 || textNodes.indexOf(blockContainer.nodeName) !== -1) {
                                    blockContainer = blockContainer.parentNode;
                                }
                                if (parentContainer === editZone[0]) {
                                    var wrapper = $('<div></div>');
                                    $(editZone[0]).append(wrapper);
                                    wrapper.html('&#8203;');
                                    blockContainer = wrapper[0];
                                    parentContainer = wrapper[0];
                                }
                                if (blockContainer === editZone[0]) {
                                    var startOffset = range.startOffset;
                                    var wrapper = $('<div></div>');
                                    
                                    while (editZone[0].childNodes.length) {
                                        $(wrapper).append(editZone[0].childNodes[0]);
                                    }
                                    $(blockContainer).append(wrapper);
                                    blockContainer = wrapper[0];
                                    var sel = document.getSelection();
                                    var r = document.createRange();
                                    r.setStart(parentContainer, startOffset);
                                    sel.removeAllRanges();
                                    sel.addRange(r);
                                    range = r;
                                }
                                var newNodeName = 'div';
                                if ((parentContainer.nodeType === 1 && range.startOffset < parentContainer.childNodes.length)
                                    || (parentContainer.nodeType === 3 && range.startOffset < parentContainer.textContent.length)) {
                                    newNodeName = blockContainer.nodeName.toLowerCase();
                                }
                                var newLine = $('<' + newNodeName + '>&#8203;</' + newNodeName + '>');

                                blockContainer.parentNode.insertBefore(newLine[0], blockContainer.nextSibling);

                                newLine.attr('style', $(blockContainer).attr('style'));
                                newLine.attr('class', $(blockContainer).attr('class'));
                                
                                e.preventDefault();
                                var rangeStart = 1;
								if(parentContainer.nodeType === 3){
									var content = document.createElement('span');
									var content = parentContainer.textContent.substring(range.startOffset, parentContainer.textContent.length);
									if(!content){
										content = '&#8203;';
									}
									else {
										rangeStart = 0;
									}
									newLine.html(content);
									parentContainer.textContent = parentContainer.textContent.substring(0, range.startOffset);
								}
								else{
									while(parentContainer.childNodes.length > range.startOffset){
										newLine.append(parentContainer.childNodes[range.startOffset]);
									}
								}

                                var nodeCursor = parentContainer;
                                while (nodeCursor !== blockContainer) {
                                    var cursorClone;
                                    if (nodeCursor.nodeType === 1) {
                                        cursorClone = document.createElement(nodeCursor.nodeName.toLowerCase());
                                        $(cursorClone).attr('style', $(nodeCursor).attr('style'));
                                        $(cursorClone).attr('class', $(nodeCursor).attr('class'));
                                        $(cursorClone).append(newLine[0].firstChild);
                                        newLine.prepend(cursorClone);
                                    }
                                            
                                    var sibling = nodeCursor.nextSibling;
                                    while (sibling !== null) {
                                        //order matters here. appending sibling before getting nextsibling breaks the loop
                                        var currentSibling = sibling;
                                        sibling = sibling.nextSibling;
                                        newLine.append(currentSibling);
                                    }

                                    nodeCursor = nodeCursor.parentNode;
                                }

                                if (!parentContainer.wholeText && parentContainer.nodeType === 3) {
                                    // FF forces encode on textContent, this is a hack to get the actual entities codes,
                                    // since innerHTML doesn't exist on text nodes
                                    parentContainer.textContent = $('<div>&#8203;</div>')[0].textContent;
                                }

                                var range = document.createRange();
								var newStartContainer = newLine[0];
								while(newStartContainer.firstChild){
									newStartContainer = newStartContainer.firstChild;
								}
                                range.setStart(newStartContainer, rangeStart);

                                sel.removeAllRanges();
                                sel.addRange(range);
                            }

                            if (e.keyCode === 8 || e.keyCode === 46) {
                                editorInstance.addState(editZone.html());
                                // for whatever reason, ff likes to create several ranges for table selection
                                // which messes up their deletion
                                for (var i = 0; i < sel.rangeCount; i++) {
                                    var startContainer = sel.getRangeAt(i).startContainer;
                                    if (startContainer.nodeType === 1 && startContainer.nodeName === 'TD' || startContainer.nodeName === 'TR') {
                                        startContainer.remove();
                                    }
                                }
                                editZone.find('table').each(function (index, item) {
                                    if ($(item).find('tr').length === 0) {
                                        $(item).remove();
                                    }
                                });
                            }
                            if (e.ctrlKey && e.keyCode === 86) {
                                setTimeout(function() {
                                    editorInstance.editZone.find('i').contents().unwrap().wrap('<em/>');
                                    editorInstance.addState(editorInstance.editZone.html());
                                }, 0);
                            }
                            if(e.keyCode === 90 && e.ctrlKey && !e.shiftKey){
                                editorInstance.undo();
                                e.preventDefault();
                                scope.$apply();
                            }
                            if((e.keyCode === 90 && e.ctrlKey && e.shiftKey) || (e.keyCode === 89 && e.ctrlKey)){
                                editorInstance.redo();
                                e.preventDefault();
                                scope.$apply();
                            }
                            if(e.keyCode === 9){
                                e.preventDefault();
                                var currentTag;
                                if(editorInstance.selection.range.startContainer.tagName){
                                    currentTag = editorInstance.selection.range.startContainer;
                                }
                                else{
                                    currentTag = editorInstance.selection.range.startContainer.parentNode;
                                }
                                if(currentTag.tagName === 'TD'){
                                    var nextTag = currentTag.nextSibling;
                                    if(!nextTag){
                                        nextTag = $(currentTag).parent('tr').next().children('td')[0];
                                    }
                                    if(!nextTag){
                                        var newLine = $('<tr></tr>');
                                        for(var i = 0; i < $(currentTag).parent('tr').children('td').length; i++){
                                            newLine.append($('<td><br /></td>'));
                                        }
                                        nextTag = newLine.children('td')[0];
                                        $(currentTag).closest('table').append(newLine);
                                    }
                                    editorInstance.selection.moveCaret(nextTag, nextTag.firstChild.textContent.length);
                                }
                                else if (currentTag.tagName === 'LI') {
                                    document.execCommand('indent');
                                }
                                else {
                                    editorInstance.selection.range.insertNode($('<span style="padding-left: 25px;">&#8203;</span>')[0]);
                                }
                            }
                        });

                        editZone.on('keyup', function(e){
                            htmlZone.css({ 'min-height': '250px', height: 0 });
                            var newHeight = htmlZone[0].scrollHeight + 2;
                            if(newHeight > htmlZone.height()){
                                htmlZone.height(newHeight);
                            }
                        });

                        editorInstance.on('contentupdated', function (e) {
                            htmlZone.css({ 'min-height': '250px', height: 0 });
                            editZone.css({ 'min-height': '250px' });
                            var newHeight = htmlZone[0].scrollHeight + 2;
                            if (newHeight > htmlZone.height()) {
                                htmlZone.height(newHeight);
                            }
                            if (newHeight > parseInt(editZone.css('min-height')) && !element.hasClass('edit')) {
                                editZone.css('min-height', newHeight);
                            }

                            scope.$apply(function(){
                                scope.$eval(attributes.ngChange);
                                ngModel.assign(scope, htmlZone.val());
                            });
                        });

                        htmlZone.on('keydown', function (e) {
                            // free main thread so it can render textarea changes
                            setTimeout(function () {
                                highlightZone.text($(this).val());
                                Prism.highlightAll();
                            }.bind(this), 200);
                            if(e.keyCode === 9){
                                e.preventDefault();
                                var start = this.selectionStart;
                                var end = this.selectionEnd;

                                $(this).val($(this).val().substring(0, start) + "\t" + $(this).val().substring(end));

                                this.selectionStart = this.selectionEnd = start + 1;
                            }
                        });

                        htmlZone.on('blur', function(){
                            scope.$apply(function(){
                                scope.$eval(attributes.ngChange);
                                ngModel.assign(scope, htmlZone.val());
                            });
							editorInstance.trigger('change');
                        });

                        element.on('dragover', function(e){
                            element.addClass('droptarget');
                        });

                        element.on('dragleave', function(){
                            element.removeClass('droptarget');
                        });

                        element.find('[contenteditable]').on('drop', function (e) {
							if(!e.originalEvent.dataTransfer){
								return;
							}
                            var visibility = 'protected';
                            if (element.attr('public') !== undefined) {
                                visibility = 'public';
                            }

                            element.removeClass('droptarget');
                            var el = {};
                            var files = e.originalEvent.dataTransfer.files;
                            if(!files.length){
                                return;
                            }
                            e.preventDefault();
                            var range;
                            var sel = window.getSelection();
                            if (document.caretRangeFromPoint) {
                                range = document.caretRangeFromPoint(e.originalEvent.clientX, e.originalEvent.clientY);
                            }
                            else if (document.caretPositionFromPoint) {
                                var caretPosition = document.caretPositionFromPoint(e.originalEvent.clientX, e.originalEvent.clientY);
                                range = document.createRange();
                                range.setStart(caretPosition.offsetNode, caretPosition.offset);
                            }
                            if (range) {
                                
                                sel.removeAllRanges();
                                sel.addRange(range);
                                editorInstance.selection.range = range;
                            }

                            for(var i = 0; i < files.length; i++){
                                (function(){
                                    var name = files[i].name;
                                    workspace.Document.prototype.upload(files[i], 'file-upload-' + name + '-' + i, function(doc){
                                        var path = '/workspace/document/';
                                        if (visibility === 'public') {
                                            path = '/workspace/pub/document/';
                                        }

                                        if (name.indexOf('.mp3') !== -1 || name.indexOf('.wav') !== -1 || name.indexOf('.ogg') !== -1) {
                                            el = $('<audio controls></audio>');
                                            el.attr('src', path + doc._id)
                                        }
                                        else if (name.toLowerCase().indexOf('.png') !== -1 || name.toLowerCase().indexOf('.jpg') !== -1 || name.toLowerCase().indexOf('.jpeg') !== -1 || name.toLowerCase().indexOf('.svg') !== -1) {
                                            el = $('<img />');
                                            el.attr('src', path + doc._id)
                                        }
                                        else {
                                            el = $('<div class="download-attachments">' +
                                                '<h2>' + lang.translate('editor.attachment.title') + '</h2>' +
			                                    '<div class="attachments">' +
                                                    '<a href="'+ path + doc._id + '"><div class="download"></div>' + name + '</a>' +
                                            '</div></div><div><br /><div><br /></div></div>');
                                        }

                                        editorInstance.selection.replaceHTML('<div>' + el[0].outerHTML + '<div><br></div><div><br></div></div>');
                                    }, visibility);
                                }())
                            }
                        });

                        scope.$on('$destroy', function () {
                            cancelAnimationFrame(placeEditorToolbar);
                        });
			        }
			    };
			});


			//Style directives

			module.directive('selectList', function(){
				return {
					restrict: 'E',
					transclude: true,
					scope: {
						displayAs: '@',
						placeholder: '@',
                        display: '='
					},
					template: '' +
					    '<div class="selected-value">[[showValue()]]</div>' +
                        '<div class="options hidden" ng-transclude></div>',
					link: function(scope, element, attributes){
						scope.showValue = function(){
							if(!scope.display){
								return lang.translate(scope.placeholder);
							}
							if(!scope.displayAs){
								return lang.translate(scope.display);
							}
							return lang.translate(scope.display[scope.displayAs]);
						};

						element.children('.options').on('mouseover', function (e){
							e.stopPropagation()
						});

						element.children('.selected-value').on('click', function(){
						    if (element.children('.options').hasClass('hidden')) {
						        setTimeout(function () {
						            element.parent().css({ 'z-index': 9999 });
						            element.parents('editor-toolbar').each(function(index, item) {
                                        $(item).css({
                                            'margin-top': '-' + item.scrollTop + 'px',
                                            'min-height': '0',
                                            'height': 'auto'
                                        })
						            });
						            element.parents().css({
						                 overflow: 'visible'
						            });
						        }, 0);
						        
								element.children('.options').removeClass('hidden');
								element.children('.options').height(element.children('.options')[0].scrollHeight);
							}
						    else {
						        element.parent().css({ 'z-index': '' });
						        element.parents().css({ overflow: '' });
						        element.parents('editor-toolbar').each(function (index, item) {
						            $(item).css({ 'margin-top': '', 'min-height': '', height: '' })
						        });
								element.children('.options').addClass('hidden');
							}
						});

						$('body').click(function(e){
						    if (e.target === element.find('.selected-value')[0] ||
                                element.children('.options').hasClass('hidden')) {
								return;
							}

						    if (element.parents('lightbox').length === 0) {
						        element.parent().css({ 'z-index': '' });
						        element.parents().css({ overflow: '' });
						    }

						    element.parents('editor-toolbar').each(function (index, item) {
						        $(item).css({ 'margin-top': '', 'min-height': '', height: '' })
						    });

							element.children('.options').addClass('hidden');
						});
					}
				}
			});

			module.directive('popover', function(){
				return {
					controller: function(){},
					restrict: 'E',
					link: function (scope, element, attributes) {

					}
				};
			});

			module.directive('popoverOpener', function(){
				return {
					require: '^popover',
					link: function(scope, element, attributes){
					    var parentNode = element.parents('popover');
					    var mouseEvent = parentNode.attr('mouse-event') || 'mouseover';
						var popover = parentNode.find('popover-content');
						parentNode.on(mouseEvent, function (e) {
						    if (mouseEvent === 'click') {
                                if (popover.hasClass('hidden')) {
                                    e.stopPropagation();
                                }

						        $('body').one('click', function (e) {
						            popover.addClass("hidden");
						        });
						    }

							if(popover.offset().left + popover.width() > $(window).width()){
								popover.addClass('right');
							}
							if(popover.offset().left < 0){
								popover.addClass('left');
							}
							if(popover.offset().top + popover.height() > $(window).height()){
								popover.addClass('bottom');
							}
							popover.removeClass("hidden");
						});

                         if(mouseEvent === 'mouseover') {
                            parentNode.on('mouseout', function (e) {
                                popover.addClass("hidden");
                            });
                        }
					}
				};
			});

			module.directive('popoverContent', function(){
				return {
					require: '^popover',
					restrict: 'E',
					link: function(scope, element, attributes){
						element.addClass("hidden");
					}
				};
			});

			module.directive('mathjax', function(){
				return {
					restrict: 'E',
					scope: {
						formula: '@'
					},
					link: function (scope, element, attributes) {
					    if (!window.MathJax) {
							loader.openFile({
								async: true,
								ajax: false,
								url: '/infra/public/mathjax/MathJax.js',
								success: function(){
								    MathJax.Hub.Config({
								        messageStyle: 'none',
								        tex2jax: { preview: 'none' },
								        jax: ["input/TeX", "output/CommonHTML"],
								        extensions: ["tex2jax.js", "MathMenu.js", "MathZoom.js", "AssistiveMML.js"],
								        TeX: {
								            extensions: ["AMSmath.js", "AMSsymbols.js", "noErrors.js", "noUndefined.js"]
								        }
								    });
					            	MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
								}
							});
					    }

                        scope.updateFormula = function(newVal){
                            element.text('$$' + newVal + '$$');
							if (window.MathJax && window.MathJax.Hub) {
							    MathJax.Hub.Queue(["Typeset",MathJax.Hub]);
							}
                        };

						attributes.$observe('formula', function(newVal){
							scope.updateFormula(newVal);
						});
					}
				}
			});
		}
	};
}());

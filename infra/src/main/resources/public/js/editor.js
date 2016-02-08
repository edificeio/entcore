/// <reference path="./jquery-1.10.2.min.js" />

var RTE;
window.RTE = (function () {
    function rgb(r, g, b) {
        return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
    }
    var rgba = rgb;
    var transparent = 'rgba(255, 255, 255, 0)';

	loader.openFile({
        url: '/infra/public/js/prism/prism.js',
        callback: function () {

        },
        error: function () {

        }
    });

    $('body').append(
        $('<link />')
            .attr('rel', 'stylesheet')
            .attr('type', 'text/css')
            .attr('href', '/infra/public/js/prism/prism.css')
   );

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
				if (!(that.editZone.find(range.startContainer.parentNode).length && range.startContainer.parentNode !== that.editZone[0]) ||
                    !that.editZone.find(range.endContainer.parentNode).length && range.endContainer.parentNode !== that.editZone[0]) {
					return;
				}
				var selector = [];
				if(range.startContainer === range.endContainer){
					if(range.startContainer.childNodes.length){
						for(var i = range.startOffset; i < range.endOffset; i++){
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
					    if (((range.intersectsNode && range.intersectsNode(item)) || $(range.commonAncestorContainer).find(item).length > 0)
                            && item !== range.startContainer.parentNode
                            && item !== range.endContainer.parentNode
                            && !$(item).find(range.startContainer.parentNode).length
                            && !$(item).find(range.endContainer.parentNode).length) {
							selector.push(item);
						}
					});

					if (range.endContainer !== that.editZone[0] && range.endOffset > 0) {
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
			    same = same || this.instance.element.find(range.startContainer).length === 0;
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

				if (!element.textContent && element.nodeType === 1) {
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
				if(!this.selectedElements.length){
					element.html('&nbsp;');
					var elementAtCaret = this.range.startContainer;
					if (elementAtCaret.nodeType === 1 && elementAtCaret.getAttribute('contenteditable')) {
					    var newEl = document.createElement('div');
					    elementAtCaret.appendChild(newEl);
					    elementAtCaret = newEl;
					}
					if (elementAtCaret.nodeType === 3) {
					    element.text(elementAtCaret.textContent);
					    elementAtCaret.parentNode.parentNode.insertBefore(element[0], elementAtCaret.parentNode);
					    elementAtCaret.parentNode.remove();
					}
					else {
					    if (elementAtCaret.innerHTML) {
					        element.html(elementAtCaret.innerHTML);
					    }

					    elementAtCaret.parentNode.insertBefore(element[0], elementAtCaret);
					    elementAtCaret.remove();
					}
					this.moveCaret(element[0], element.text().length);
				}
				else{
					this.selectedElements.forEach(function(item){
						var el = $(element[0].outerHTML);
						el.html(item.innerHTML || item.textContent);
						if(!item.parentNode){
							return;
						}
						if (item.nodeType !== 1 && item.parentNode.nodeName !== 'DIV') {
						    item = item.parentNode;
						}
						item.parentNode.replaceChild(el[0], item);
						that.selectNode(el[0]);
					});
				}

				this.instance.trigger('contentupdated');
			};

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
							$(item).wrap(node);
						}
						else{
							if(index === 0 && that.range.startOffset >= 0 && that.range.startContainer !== that.range.endContainer){
								el.html(item.textContent.substring(that.range.startOffset));
								item.parentNode.insertBefore(el[0], item.nextSibling);
								item.textContent = item.textContent.substring(0, that.range.startOffset);
							}
							else if(index === that.selectedElements.length - 1 && that.range.endOffset <= item.textContent.length && that.range.startContainer !== that.range.endContainer){
								el.text(item.textContent.substring(0, that.range.endOffset));
								item.parentNode.insertBefore(el[0], item);
								item.textContent = item.textContent.substring(that.range.endOffset);
							}
							else if(that.range.startContainer === that.range.endContainer && index === 0){
								el.html(item.textContent.substring(that.range.startOffset, that.range.endOffset));
								var textBefore = document.createTextNode('');
								textBefore.textContent = item.textContent.substring(0, that.range.startOffset);
								item.parentNode.insertBefore(el[0], item);
								item.parentNode.insertBefore(textBefore, el[0]);
								item.textContent = item.textContent.substring(that.range.endOffset);
							}
							addedNodes.push(el[0]);
						}
					});
					addedNodes.forEach(that.selectNode);
				}

				that.instance.trigger('contentupdated');
			};

			function applyCSS(css) {
                if (!that.range) {
                    return;
                }
			    that.instance.addState(that.editZone.html());

				if(!that.selectedElements.length){
					var el = $('<span>&nbsp;</span>');
					el.css(css);
					var elementAtCaret = that.range.startContainer;
					if (elementAtCaret.nodeType !== 1) {
					    elementAtCaret = elementAtCaret.parentNode;
					}
					if (elementAtCaret.nodeName === 'SPAN') {
					    elementAtCaret = elementAtCaret.parentNode;
					}
					if (that.editZone.find(elementAtCaret).length === 0) {
					    elementAtCaret = that.editZone[0];
					}
					$(elementAtCaret).append(el);
					that.moveCaret(el[0], 1);
				}
				else if (that.selectedElements.length === 1 &&
                    that.range.startOffset === 0 && that.range.endOffset === that.selectedElements[0].textContent.length) {
				    var element = that.selectedElements[0];
				    if (element.nodeType !== 1) {
				        element = element.parentNode;
				    }
				    $(element).css(css);
				    that.selectNode(element);
				}
				else{
					var addedNodes = [];
					that.selectedElements.forEach(function(item, index){
						if(item.nodeType === 1){
                            addedNodes.push(item);
							$(item).css(css);
						}
						else{
						    var el = $(document.createElement('span'));
							el.css(css);

							if(index === 0 && that.range.startOffset >= 0 && that.range.startContainer !== that.range.endContainer){
								el.html(item.textContent.substring(that.range.startOffset));
								item.parentNode.insertBefore(el[0], item.nextSibling);
								item.textContent = item.textContent.substring(0, that.range.startOffset);
							}
							else if(index === that.selectedElements.length - 1 && that.range.endOffset <= item.textContent.length && that.range.startContainer !== that.range.endContainer){
								el.text(item.textContent.substring(0, that.range.endOffset));
								item.parentNode.insertBefore(el[0], item);
								item.textContent = item.textContent.substring(that.range.endOffset);
							}
							else if(that.range.startContainer === that.range.endContainer && index === 0){
								el.html(item.textContent.substring(that.range.startOffset, that.range.endOffset));
								var textBefore = document.createTextNode('');
								textBefore.textContent = item.textContent.substring(0, that.range.startOffset);
								item.parentNode.insertBefore(el[0], item);
								item.parentNode.insertBefore(textBefore, el[0]);
								item.textContent = item.textContent.substring(that.range.endOffset);
							}
							addedNodes.push(el[0]);
						}
					});
                    if(addedNodes.length === 1){
                        that.selectNode(addedNodes[0]);
                    }
                    else{
                        var sel = window.getSelection();
                        var range = document.createRange();

                        range.setStartBefore(addedNodes[0]);
                        range.setEndAfter(addedNodes[addedNodes.length - 1]);

                        sel.removeAllRanges();
                        sel.addRange(range);
                        that.instance.trigger('selectionchange');
                    }
				}

                that.instance.addState(that.editZone.html());
				that.instance.trigger('contentupdated');
			}

			this.isEmpty = function () {
			    return !this.range || this.range.startContainer === this.range.endContainer && this.startOffset === this.endOffset;
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
				wrapper.html(htmlContent);
				if (this.range) {
				    this.range.deleteContents();
					this.range.insertNode(wrapper[0]);
				}
				else{
					this.editZone.append(wrapper);
				}

				this.instance.trigger('contentupdated');
			};

			this.$ = function(){
				var jSelector = $();
				this.selectedElements.forEach(function(item){
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
				optionElement.html(instance.compile(optionResult.template)(optionScope));
				optionResult.link(optionScope, optionElement, instance.attributes);
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
							instance.trigger('contentupdated')
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
							instance.trigger('contentupdated');
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
						element.on('click', function(){
							if(!instance.editZone.is(':focus')){
								instance.focus();
							}
							instance.execCommand('bold');
							if(document.queryCommandState('bold')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
							}
						});

						instance.on('selectionchange', function(e){
							if(document.queryCommandState('bold')){
								element.addClass('toggled');
							}
							else{
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
							    instance.selection.css({ 'font-style': '' });
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
							instance.execCommand('underline');
							if(document.queryCommandState('underline')){
								element.addClass('toggled');
							}
							else{
								element.removeClass('toggled');
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

			RTE.baseToolbarConf.option('justifyLeft', function(instance){
				return {
					template: '<i tooltip="editor.option.justify.left"></i>',
					link: function(scope, element, attributes){
						element.addClass('toggled');
						element.on('click', function () {
							if(!instance.editZone.is(':focus')){
								instance.focus();
							}
							instance.execCommand('justifyLeft');
							if(document.queryCommandState('justifyLeft')){
								element.addClass('toggled');							}
							else{
								element.removeClass('toggled');
							}

							instance.editZone.find('img').each(function (index, item) {
							    if ($(item).css('text-align') === 'left') {
							        $(item).css({ 'float': 'left', 'z-index': 0 });
							    }
							});

							instance.trigger('justify-changed');
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

							if(!document.queryCommandState('justifyRight')){
								instance.execCommand('justifyRight');
								element.addClass('toggled');
							}
							else{
								instance.execCommand('justifyLeft');
								element.removeClass('toggled');
							}

							instance.editZone.find('img').each(function (index, item) {
							    if ($(item).css('text-align') === 'right') {
							        $(item).css({ 'float': 'right', 'z-index': '0' });
							    }
							});

							instance.trigger('justify-changed');
						});

						instance.on('selectionchange', function(e){
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

							if(!document.queryCommandState('justifyCenter')){
								instance.execCommand('justifyCenter');
								element.addClass('toggled');
							}
							else{
								instance.execCommand('justifyLeft');
								element.removeClass('toggled');
							}

							instance.editZone.find('img').each(function (index, item) {
							    if ($(item).css('text-align') === 'center') {
                                    // z-index is a hack to track margin width; auto width is computed as 0 in FF
							        $(item).css({ 'float': 'none', 'margin': 'auto', 'z-index': '1' });
							    }
                                else{
                                    // z-index is a hack to track margin width; auto width is computed as 0 in FF
							        $(item).css({ 'float': 'left', 'z-index': '0' });
                                }
							});

							instance.trigger('justify-changed');
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

							if(!document.queryCommandState('justifyFull')){
								element.addClass('toggled');
								instance.execCommand('justifyFull');
							}
							else{
								instance.execCommand('justifyLeft');
								element.removeClass('toggled');
							}

							instance.trigger('justify-changed');
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
						element.on('mousedown', function(){
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
					template: '<input tooltip="editor.option.color" type="color" />',
					link: function (scope, element, attributes) { 
						if(!$.spectrum){
							$.spectrum = {};
							http().get('/infra/public/spectrum/spectrum.js').done(function(data){
								eval(data);
                                setSpectrum();
							});
							var stylesheet = $('<link rel="stylesheet" type="text/css" href="/infra/public/spectrum/spectrum.css" />');
							$('head').prepend(stylesheet);
						}
						else if ($.spectrum && $.spectrum.palettes && element.find('input[type=color]')[0].type === 'text') {
						    element.find("input[type=color]").spectrum();
                            setSpectrum();
						}
						scope.foreColor = "#000000";
						element.children('input').on('change', function(){
							scope.foreColor = $(this).val();
							scope.$apply('foreColor');
						});

						scope.$watch('foreColor', function(){
						    if(scope.foreColor !== eval(instance.selection.css('color'))) {
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
					template: '<input tooltip="editor.option.backgroundcolor" type="color" />',
					link: function(scope, element, attributes){
						if(!$.spectrum){
							$.spectrum = {};
							http().get('/infra/public/spectrum/spectrum.js').done(function(data){
								eval(data);
                                setSpectrum();
							});
							var stylesheet = $('<link rel="stylesheet" type="text/css" href="/infra/public/spectrum/spectrum.css" />');
							$('head').prepend(stylesheet);
						}
						else if ($.spectrum && $.spectrum.palettes && element.find('input[type=color]')[0].type === 'text') {
						    element.find('input[type=color]').spectrum();
                            setSpectrum();
						}
						element.children('input').on('change', function(){
							scope.backColor = $(this).val();
							scope.$apply('backColor');
						});

						scope.$watch('backColor', function () {
						    if(scope.backColor !== eval(instance.selection.css('background-color'))) {
						        instance.selection.css({ 'background-color': scope.backColor });
						    }
						});

						instance.on('selectionchange', function(e){
						    scope.backColor = eval(instance.selection.css('background-color'));
							element.children('input').val(scope.backColor);
						});
					}
				};
			});

			RTE.baseToolbarConf.option('font', function(instance){
				return {
					template:
					'<select-list display="font" display-as="fontFamily" placeholder="Police" tooltip="editor.option.font">' +
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

						scope.fonts = [{ fontFamily: 'Arial' }, { fontFamily: 'Verdana' }, { fontFamily: 'Tahoma' }, { fontFamily: "'Comic Sans MS'" }];
						scope.font = '';

						setTimeout(function() {
							var importedFonts = loadImportedFonts();
							scope.fonts = scope.fonts.concat(importedFonts);
							scope.font = _.findWhere(scope.fonts, { fontFamily: $('p').css('font-family') });
						}, 0);

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
					template: '<select-list placeholder="Taille" display="font.fontSize.size" tooltip="editor.option.fontSize">' +
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
					template: '<select-list model="format" placeholder="Paragraphe" display-as="label" display="format" tooltip="editor.option.format">' +
					'<opt ng-repeat="format in formats" value="format" ng-click="wrap(format)"><div bind-html="format.option"></div></opt>' +
					'</select-list>',
					link: function(scope, element, attributes){
						scope.formats = [
							{
								apply: { tag: 'p' },
								option: '<p>[[format.label]]</p>',
								label: 'Paragraphe'
							},
							{
								apply: { tag: 'h1' },
								option: '<h1>[[format.label]]</h1>',
								label: 'Titre 1'
							},
							{
								apply: { tag: 'h2' },
								option: '<h2>[[format.label]]</h2>',
								label: 'Titre 2'
							},
							{
								apply: { tag: 'h3' },
								option: '<h3>[[format.label]]</h3>',
								label: 'Titre 3'
							},
							{
								apply: { tag: 'p', classes: ['info'] },
								option: '<p class="info">[[format.label]]</p>',
								label: 'Information'
							},
							{
								apply: { tag: 'p', classes: ['warning'] },
								option: '<p class="warning">[[format.label]]</p>',
								label: 'Avertissement'
							}
						];

						instance.on('selectionchange', function (e) {
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
			                instance.execCommand('removeFormat');
			                if (document.queryCommandEnabled('removeFormat')) {
			                    element.removeClass('disabled');
			                }
			                else {
			                    element.addClass('disabled');
			                }
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
					'<lightbox show="imageOption.display.pickFile" on-close="imageOption.display.pickFile = false;">' +
					'<media-library ng-change="updateContent()" multiple="true" ng-model="imageOption.display.files" file-format="\'img\'" visibility="imageOption.visibility"></media-library>' +
					'</lightbox>',
				    link: function (scope, element, attributes) {
				        ui.extendSelector.touchEvents('[contenteditable] img');

				        scope.imageOption = {
				            display: { pickFile: false },
				            visibility: 'protected'
				        }

						if(instance.element.attr('public')){
							scope.visibility = 'public'
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
							    }
							},
                            {
                                label: 'editor.align.right',
                                action: function (e) {
                                    $(e.target).css({ float: 'right', margin: '10px', 'z-index': '0' });
                                    instance.selection.selectNode(e.target);
                                    instance.trigger('contentupdated');
                                    instance.trigger('justify-changed');
                                }
                            },
                            {
                                label: 'editor.align.left',
                                action: function (e) {
                                    $(e.target).css({ float: 'left', margin: '10px', 'z-index': '0' });
                                    instance.selection.selectNode(e.target);
                                    instance.trigger('contentupdated');
                                    instance.trigger('justify-changed');
                                }
                            },
                            {
                                label: 'editor.align.center',
                                action: function (e) {
                                    $(e.target).css({ float: 'none', margin: 'auto', 'z-index': '1' });
                                    instance.selection.selectNode(e.target);
                                    instance.trigger('contentupdated');
                                    instance.trigger('justify-changed');
                                }
                            }
						]);

						instance.editZone.addClass('drawing-zone');
						scope.display = {};
						scope.updateContent = function () {
						    var html = '<div>';
						    scope.imageOption.display.files.forEach(function (file) {
						        html += '<img src="/workspace/document/' + file._id + '" draggable native />';
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
							}, 200)
						});
					}
				}
			});

			RTE.baseToolbarConf.option('sound', function(instance){
				return {
					template: '<i ng-click="display.pickFile = true" tooltip="editor.option.sound"></i>' +
					'<lightbox show="display.pickFile" on-close="display.pickFile = false;">' +
					'<media-library ng-change="updateContent()" ng-model="display.file" file-format="\'audio\'"></media-library>' +
					'</lightbox>',
					link: function(scope, element, attributes){
						instance.editZone.addClass('drawing-zone');
						scope.display = {};
						scope.updateContent = function(){
							instance.selection.replaceHTML(
								'<div><br /></div>' +
								'<div class="audio-wrapper"><audio src="/workspace/document/' + scope.display.file._id + '" controls draggable native></audio></div>' +
								'<div><br /></div>'
							);
							scope.display.pickFile = false;
							scope.display.file = undefined;
						};

						instance.element.on('drop', function (e) {
                            var audio;
						    if (e.originalEvent.dataTransfer.mozSourceNode) {
						        audio = e.originalEvent.dataTransfer.mozSourceNode;
						    }

							//delay to account for sound destruction and recreation
							setTimeout(function(){
                                if(audio && audio.tagName && audio.tagName === 'AUDIO'){
                                    audio.remove();
                                }
								ui.extendElement.resizable(instance.editZone.find('audio'), {
								    moveWithResize: false,
                                    mouseUp: function() {
                                        instance.trigger('contentupdated');
                                        instance.addState(instance.editZone.html());
                                    }
								});
							}, 200)
						});
					}
				}
			});

			RTE.baseToolbarConf.option('embed', function (instance) {
			    return {
			        template: '<i ng-click="display.copyEmbed = true" tooltip="editor.option.embed"></i>' +
					'<lightbox show="display.copyEmbed" on-close="display.copyEmbed = false;">' +
					'<h2>Choisir un modèle</h2>' +
					'<p class="info"><i18n>info.video.embed</i18n></p>' +
					'<textarea ng-model="display.htmlCode"></textarea>' +
					'<div class="row">' +
					'<button type="button" ng-click="applyHtml()" class="right-magnet"><i18n>apply</i18n></button>' +
					'<button type="button" ng-click="display.copyEmbed = false" class="cancel right-magnet"><i18n>cancel</i18n></button>' +
					'</div>' +
					'</lightbox>',
			        link: function (scope, element, attributes) {
			            scope.display = {};
			            scope.applyHtml = function (template) {
			                scope.display.copyEmbed = false;
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
                                angular.element(editNode).scope().updateFormula(scope.display.formula);
                            }
                            else{
                                instance.selection.replaceHTML(instance.compile(`
                                    <div class="row"><br/></div>
                                    <div class="row">
                                        <mathjax formula="`+ scope.display.formula + `"></mathjax>
                                    </div>
                                    <div><br /></div>
                                `)(scope));
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
                        
                        // style placed here temporarily to avoid rebuilding css
                        // to be moved in 1.16
                        $(`
                            <style>
                                [contenteditable] mathjax{
                                    display: block;
                                    float: left;
                                    position: relative;
                                    border: 1px solid #ccc;
                                }
                                [contenteditable] mathjax::after{
                                    display: block;
                                    content: " ";
                                    height: 100%;
                                    width: 100%;
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    -moz-user-select: none;
                                    -webkit-user-select: none;
                                    user-select: none;
                                }
                            </style>
                        `).appendTo('head');
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
							Behaviours.applicationsBehaviours[prefix].loadResources(cb);
							scope.linker.addResource = Behaviours.applicationsBehaviours[prefix].create;
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
								scope.linker.resource.title = scope.linker.search.text;
								if(typeof cb === 'function'){
									cb();
								}
							});
						};

						scope.linker.createResource = function(){
							Behaviours.loadBehaviours(scope.linker.params.appPrefix, function(appBehaviour){
								appBehaviour.create(scope.linker.resource, function(){
									scope.linker.searchApplication();
									scope.linker.search.text = scope.linker.resource.title;
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
							}

							if (selectedNode && selectedNode.nodeName === 'A') {
							    instance.selection.moveCaret(linkNode[0], linkNode.text().length);
							    instance.trigger('contentupdated');
							    scope.linker.display.chooseLink = false;
							    scope.linker.params = {};
							    scope.linker.display.search = {
							        application: {},
							        text: ''
							    };
							    scope.linker.externalLink = false;
							    return;
							}

							if (instance.selection.selectedElements.length === 0) {
							    linkNode.text(scope.linker.params.link);
							    instance.selection.replaceHTML(linkNode[0].outerHTML);
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
									function(match){
										return app.address.indexOf(match) !== -1 && app.icon
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
						element.on('click', function(){
							document.execCommand('unlink');
							element.addClass('disabled');
						});

						instance.on('selectionchange', function(e){
							if(e.selection.$().is('a')){
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
					'<div class="row">' +
					'<img ng-repeat="smiley in smileys" ng-click="addSmiley(smiley)" skin-src="/img/smileys/[[smiley]].png" />' +
					'</div>' +
					'</lightbox>',
					link: function(scope, element, attributes){
						scope.display = {};
						scope.smileys = [ "happy", "proud", "dreamy", "love", "tired", "angry", "worried", "sick", "joker", "sad" ];
						scope.addSmiley = function (smiley) {
						    //do not replace with i, as i is used by other websites for italic and
                            //is often copy-pasted in the editor
							var content = instance.compile('<img skin-src="/img/smileys/' + smiley + '.png" draggable native style="height: 60px; width: 60px;" />')(scope.$parent);
							instance.selection.replaceHTML(content);
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
									cell.html('&nbsp;')
									row.append(cell);
								}
							}
							instance.selection.replaceHTML(table.outerHTML);
							element.find('popover-content').addClass('hidden');
							instance.trigger('contentupdated');
						});

						instance.bindContextualMenu(scope, 'td', [
							{
								label: 'editor.add.row',
								action: function(e){
									var newRow = $($(e.target).parent()[0].outerHTML);
									newRow.find('td').html('&nbsp;');
									$(e.target).parent().after(newRow);
								}

							},
							{
								label: 'editor.add.column',
								action: function(e){
									var colIndex = $(e.target).index();
									$(e.target).parents('table').find('tr').each(function(index, row){
										$(row).children('td').eq(colIndex).after('<td>&nbsp;</td>')
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
					'<h2>Choisir un modèle</h2>' +
					'<ul class="thought-out-actions">' +
					'<li ng-repeat="template in templates" ng-click="applyTemplate(template)">' +
					    '<img ng-src="[[template.image]]" class="cell" />' +
					    '<div class="cell vertical-spacing horizontal-spacing">[[template.title]]</div>' +
					'</li>' +
					'</ul>' +
					'</lightbox>',
					link: function (scope, element, attributes) {
					    var split = $('#theme').attr('href').split('/');
					    var skinPath = split.slice(0, split.length - 2).join('/') + '/img';
						scope.templates = [
							{
							    title: 'Page blanche',
                                image: skinPath + '/icons/editor/templates-preview-emptypage.svg',
								html: '<div class="twelve cell column"><article></article></div>'
							},
							{
							    title: 'Deux colonnes',
							    image: skinPath + '/icons/editor/templates-preview-twocols.svg',
								html:
								'<div class="row">' +
								'<div class="six cell column">' +
									'<article>' +
										'<h2>Titre de votre première colonne</h2>' +
										'<p>Vous pouvez entrer ici le texte de votre première colonne</p>' +
									'</article>' +
								'</div>' +
								'<div class="six cell column">' +
									'<article>' +
										'<h2>Titre de votre deuxième colonne</h2>' +
										'<p>Vous pouvez entrer ici le texte de votre deuxième colonne</p>' +
									'</article>' +
								'</div>' +
								'</div>'
							},
							{
							    title: 'Trois colonnes',
							    image: skinPath + '/icons/editor/templates-preview-threecols.svg',
								html:
								'<div class="row">' +
								'<div class="four cell column">' +
									'<article>' +
										'<h2>Titre de votre première colonne</h2>' +
										'<p>Vous pouvez entrer ici le texte de votre première colonne</p>' +
									'</article>' +
								'</div>' +
								'<div class="four cell column">' +
									'<article>' +
										'<h2>Titre de votre deuxième colonne</h2>' +
										'<p>Vous pouvez entrer ici le texte de votre deuxième colonne</p>' +
									'</article>' +
								'</div>' +
								'<div class="four cell column">' +
									'<article>' +
										'<h2>Titre de votre troisième colonne</h2>' +
										'<p>Vous pouvez entrer ici le texte de votre troisième colonne</p>' +
									'</article>' +
								'</div>' +
								'</div>'
							},
							{
							    title: 'Illustration et texte',
							    image: skinPath + '/icons/editor/templates-preview-illustration.svg',
								html:
								'<div class="row">' +
									'<div class="three cell column">' +
										'<article>' +
											'<img src="' + skinPath + '/illustrations/image-default.svg" />' +
										'</article>' +

									'</div>' +
									'<div class="nine cell column">' +
										'<article>' +
											'<h2>Titre de votre texte</h2>' +
											'<p>Vous pouvez entrer ici votre texte. Pour changer l\'image du modèle, cliquez sur l\'image, puis sur le bouton' +
											'"Insérer une image" dans la barre de boutons de l\'éditeur.</p>' +
										'</article>' +
									'</div>' +
								'</div>'
							},
							{
							    title: 'Vignettes',
							    image: skinPath + '/icons/editor/templates-preview-dominos.svg',
								html:
								'<div class="dominos">' +
									'<div class="item">' +
										'<section class="domino pink">' +
										'<div class="top">' +
											'<img src="' + skinPath + '/illustrations/image-default.svg" class="fixed twelve cell" />' +
										'</div>' +
										'<div class="bottom">' +
											'<div class="content">' +
                                                'Entrez ici votre texte' +
											'</div>' +
										'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino blue">' +
											'<div class="top">' +
												'<img src="' + skinPath + '/illustrations/image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
											'<div class="bottom">' +
												'<div class="content">' +
													'Entrez ici votre texte' +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino orange">' +
											'<div class="top">' +
												'<img src="' + skinPath + '/illustrations/image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
												'<div class="bottom">' +
												'<div class="content">' +
													'Entrez ici votre texte' +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino purple">' +
											'<div class="top">' +
												'<img src="' + skinPath + '/illustrations/image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
											'<div class="bottom">' +
												'<div class="content">' +
													'Entrez ici votre texte' +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino green">' +
											'<div class="top">' +
												'<img src="' + skinPath + '/illustrations/image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
											'<div class="bottom">' +
												'<div class="content">' +
													'Entrez ici votre texte' +
												'</div>' +
											'</div>' +
										'</section>' +
									'</div>' +
									'<div class="item">' +
										'<section class="domino white">' +
											'<div class="top">' +
												'<img src="' + skinPath + '/illustrations/image-default.svg" class="fixed twelve cell" />' +
											'</div>' +
												'<div class="bottom">' +
												'<div class="content">' +
													'Entrez ici votre texte' +
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
			            '<button class="editor-toolbar-opener"></button>' +
			            '<button class="close-focus">OK</button>' +
			            '<editor-toolbar></editor-toolbar>' +
			            '<contextual-menu><ul></ul></contextual-menu>' +
			            '<popover>' +
			            '<i class="tools" popover-opener opening-event="click"></i>' +
			            '<popover-content>' +
			            '<ul>' +
			            '<li>Editeur de texte</li>' +
			            '<li>Code HTML</li>' +
			            '<li>Mode mixte</li>' +
			            '</ul>' +
			            '</popover-content>' +
			            '</popover>' +
			            '<div><div contenteditable="true"></div></div>' +
			            '<textarea></textarea>' +
			            '<code class="language-html"></code>',
			        link: function(scope, element, attributes) {
			            element.find('.close-focus').on('click', function(){
                            element.removeClass('focus');
                            element.trigger('editor-blur');
                        });

                        element.find('.editor-toolbar-opener').on('click', function(){
                            if(!$(this).hasClass('active')){
                                $(this).addClass('active');
                                element.find('editor-toolbar').addClass('opened');
                            }
                            else{
                                $(this).removeClass('active')
                                element.find('editor-toolbar').removeClass('opened');
                            }
                        });
                        document.execCommand("enableObjectResizing", false, false);
                        document.execCommand("enableInlineTableEditing", null, false);

                        element.addClass('edit');
                        var editZone = element.find('[contenteditable=true]');
                        var htmlZone = element.children('textarea');
			            var highlightZone = element.children('code');
                        document.execCommand('styleWithCSS', true);

                        if(attributes.inline !== undefined){
                            element.children('editor-toolbar').addClass('inline');
                        }

                        var toolbarConf = RTE.baseToolbarConf;
                        if(attributes.toolbarConf){
                            toolbarConf = scope.$eval(attributes.toolbarConf);
                        }

			            var editorInstance = new RTE.Instance({
                            toolbarConfiguration: toolbarConf,
                            element: element,
                            scope: scope,
                            compile: $compile,
                            editZone: editZone
                        });

			            editorInstance.addState('');
                        var ngModel = $parse(attributes.ngModel);
                        if(!ngModel(scope)){
                            ngModel.assign(scope, '');
                        }

                        scope.$watch(
                            function(){
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

                                if(newValue !== editZone.html() && !editZone.is(':focus')){
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

                        element.on('dragenter', function(e){
                            e.preventDefault();
                        });

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

                        element.find('.option i').click(function(){
                            if(!editZone.is(':focus')){
                                editZone.focus();
                            }

                            scope.$apply(function(){
                                scope.$eval(attributes.ngChange);
                                ngModel.assign(scope, editZone.html());
                            });
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

                            scope.$apply(function(){
                                scope.$eval(attributes.ngChange);
                                var content = editZone.html();
                                ngModel.assign(scope, content);
                            });
                        });

                        element.on('click', function(e){
                            if(attributes.inline !== undefined){
                                element.children('editor-toolbar').css({
                                    left: 0,
                                    top: -element.children('editor-toolbar').height() + 'px'
                                });
                                element.css({
                                    'margin-top': element.children('editor-toolbar').height() + 'px'
                                });
                            }

                            if(e.target === element.find('.close-focus')[0]){
                                return;
                            }

                            element.trigger('editor-focus');
                            element.addClass('focus');
                            element.data('lock', true);
                        });

                        $('body').on('mousedown', function(e){
                            if(e.target !== element.find('.editor-toolbar-opener')[0] && element.find('editor-toolbar, .editor-toolbar-opener').find(e.target).length === 0){
                                element.find('editor-toolbar').removeClass('opened');
                                element.find('.editor-toolbar-opener').removeClass('active');
                            }

                            if(element.find(e.target).length === 0){
                                element.children('editor-toolbar').removeClass('show');
                                element.trigger('editor-blur');
                                element.removeClass('focus');
                                element.data('lock', false);

                                if(attributes.inline !== undefined){
                                    element.css({
                                        'margin-top': 0
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

                        editZone.on('keydown', function (e) {
                            clearTimeout(typingTimer);
                            clearTimeout(editingTimer);
                            typingTimer = setTimeout(wrapFirstLine, 10);
                            
                            if (!e.ctrlKey) {
                                editingTimer = setTimeout(editingDone, 500);
                            }

                            if (e.keyCode === 13) {
                                editorInstance.addState(editZone.html());
                            }

                            if (e.keyCode === 8 || e.keyCode === 46) {
                                editorInstance.addState(editZone.html());
                                // for whatever reason, ff likes to create several ranges for table selection
                                // which messes up their deletion
                                var sel = window.getSelection();
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
                                            newLine.append($('<td>&nbsp;</td>'));
                                        }
                                        nextTag = newLine.children('td')[0];
                                        $(currentTag).closest('table').append(newLine);
                                    }
                                    editorInstance.selection.moveCaret(nextTag);
                                }
                                else if (currentTag.tagName === 'LI') {
                                    document.execCommand('indent');
                                }
                                else {
                                    editorInstance.selection.range.insertNode($('<span style="padding-left: 25px;">&nbsp;</span>')[0]);
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
                            }.bind(this), 0);
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
                        });

                        element.on('dragover', function(e){
                            element.addClass('droptarget');
                        });

                        element.on('dragleave', function(){
                            element.removeClass('droptarget');
                        });

                        element.find('[contenteditable]').on('drop', function(e){
                            element.removeClass('droptarget');
                            var el = {};
                            var files = e.originalEvent.dataTransfer.files;
                            if(!files.length){
                                return;
                            }
                            e.preventDefault();
                            for(var i = 0; i < files.length; i++){
                                (function(){
                                    var name = files[i].name;
                                    workspace.Document.prototype.upload(files[i], 'file-upload-' + name + '-' + i, function(doc){
                                        if(name.indexOf('.mp3') !== -1 || name.indexOf('.wav') !== -1 || name.indexOf('.ogg') !== -1){
                                            el = $('<audio draggable native controls></audio>');
                                        }
                                        else{
                                            el = $('<img draggable native />');
                                        }

                                        el.attr('src', '/workspace/document/' + doc._id)
                                        editorInstance.selection.replaceHTML('<div>' + el[0].outerHTML + '<div><br></div><div><br></div></div>');
                                    });
                                }())

                            }
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
								return scope.placeholder;
							}
							if(!scope.displayAs){
								return scope.display;
							}
							return scope.display[scope.displayAs];
						};

						element.children('.selected-value').on('click', function(){
						    if (element.children('.options').hasClass('hidden')) {
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
						        
								element.children('.options').removeClass('hidden');
								element.children('.options').height(element.children('.options')[0].scrollHeight);
							}
						    else {
						        element.parents().css({ overflow: '' });
						        element.parents('editor-toolbar').each(function (index, item) {
						            $(item).css({ 'margin-top': '', 'min-height': '', height: '' })
						        });
								element.children('.options').addClass('hidden');
							}
						});

						$('body').click(function(e){
							if(e.target === element.find('.selected-value')[0]){
								return;
							}

							element.parents().css({ overflow: '' });
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
								url: '/infra/public/mathjax/MathJax.js?config=TeX-AMS-MML_HTMLorMML',
								success: function(){
									MathJax.Hub.Config({ messageStyle: 'none', tex2jax: { preview: 'none' } });
					            	MathJax.Hub.Typeset();
								}
							});
					    }

                        scope.updateFormula = function(newVal){
                            element.text('$$' + newVal + '$$');
							if (window.MathJax && window.MathJax.Hub) {
							    MathJax.Hub.Config({ messageStyle: 'none', tex2jax: { preview: 'none' } });
							    MathJax.Hub.Typeset();
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

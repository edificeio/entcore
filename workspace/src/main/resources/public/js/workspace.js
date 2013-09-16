var tools = (function(){
	return {
		roleFromFileType: function(fileType){
			var types = {
				'doc': function(type){
					return type.indexOf('officedocument') !== -1 && type.indexOf('wordprocessing') !== -1;
				},
				'xls': function(type){
					return type.indexOf('officedocument') !== -1 && type.indexOf('spreadsheet') !== -1;
				},
				'img': function(type){
					return type.indexOf('image') !== -1;
				}
			}

			for(var type in types){
				if(types[type](fileType)){
					return type;
				}
			}

			return 'unknown';
		},
		formatResponse: function(response){
			for(var i = 0; i < response.length; i++){
				response[i].metadata['content-type'] = tools.roleFromFileType(response[i].metadata['content-type']);
				if(response[i].metadata['content-type'] === 'img'){
					response[i].thumbnail = 'document/' + response[i]._id;
				}

				if(response[i].created){
					response[i].created = response[i].created.split(' ')[0];
					response[i].modified = response[i].modified.split(' ')[0];
				}
				if(response[i].sent){
					response[i].sent = response[i].sent.split(' ')[0];
				}

				response[i].anyComment = function(){
					return this.commentsCount !== 0;
				};

				if(!response[i].comments){
					response[i].commentsCount = 0;
					continue;
				}

				response[i].commentsCount = response[i].comments.length;

				for(var j = 0; j < response[i].comments.length; j++){
					response[i].comments[j].posted = response[i].comments[j].posted.split(' ')[0];
					if(response[i].comments[j].author === '') {
						response[i].comments[j].author = ''
					}
				}
			}
			return response;
		},
		mapFolderName: function(dir){
			if (dir.indexOf("_") !== -1) {
				return { name : dir.substring(dir.lastIndexOf("_") + 1), path : dir };
			}
			return { name : dir, path : dir };
		},
		getFolderTree: function(currentTree, done){
			workspace.action.getFolders(true, currentTree.path, function(data){
				data.forEach(function(folder){
					var folderData = tools.mapFolderName(folder);
					if(folder === currentTree.path || folder === 'Trash'){
						return;
					}

					currentTree.folders[folderData.name] = {
						name: folderData.name,
						path: folderData.path,
						folders: {}
					}
				})

				for(var subTree in currentTree.folders){
					tools.getFolderTree(currentTree.folders[subTree], done);
				}

				if(Object.keys(currentTree.folders).length === 0){
					done(currentTree)
				}
			});
		},
		displayFoldersTree: function(nodePattern, container){
			var root = { folders: {} };
			var treeView = function(node){
				if(Object.keys(node.folders).length === 0){
					return nodePattern(node, function(){ return ''; });
				}

				return nodePattern(node, function(){
					var html = '';
					for(var folder in node.folders){
						html += treeView(node.folders[folder]);
					}

					return html;
				});
			}
			tools.getFolderTree(root, function(){
				container.html(treeView(root));
				if($('.createFolder').length > 1){
					$('.createFolder').hide();
				}
			});
		}
	}
}());

var navigation = (function(){
	var currentURL = '/documents';

	var showFolders = function(path){
		var nodePattern = function(node, subNodes){
			if(node.name){
				return '<li>\
					<a call="documents" href="documents/' + node.path + '?hierarchical=true">' + node.name + '</a>\
					<ul data-folder="' + node.path + '">' + subNodes() + '</ul>\
				</li>';
			}
			else{
				return subNodes();
			}

		};

		var container = $('[data-folder="' + path + '"]');
		tools.displayFoldersTree(nodePattern, container);
	};

	var updater = {
		redirect: function(action){
			$('nav.vertical a, nav.vertical li').removeClass('selected');
			var current = $('nav.vertical a[href="' + action + '"]');
			current.addClass('selected');
			current.parent().addClass('selected');
			current.parents('li').addClass('selected');
			currentURL = action;
		},
		currentUrl: function(){
			return currentURL;
		},
		showFolders: function(path){
			showFolders(path);
		},
		refresh: function(){
			workspace.action.documents({url : this.currentUrl()}, function(){
				$('.selectAllCheckboxes').change()
			});
		}
	};

	$(document).ready(function(){
		$('nav.vertical').on('click', 'a', function(){
			updater.redirect($(this).attr('href'));
		})
	});

	return updater;
}());

var workspace = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : {
			documents : '<table summary="">\
							<thead>\
								<tr>\
									<th scope="col">\
										<input type="checkbox" class="selectAllCheckboxes" />\
									</th>\
									<th scope="col"></th>\
									<th scope="col" class="nine">{{#i18n}}name{{/i18n}}</th>\
									<th scope="col">{{#i18n}}modified{{/i18n}}</th>\
								</tr>\
							</thead>\
							<tbody>\
								{{#folders}}\
								<tr class="overline">\
									<td></td>\
									<td><i role="folder"></i></td>\
									<td><strong><a call="documents" href="documents/{{path}}?hierarchical=true">{{name}}</a></strong></td>\
									<td></td>\
								</tr>\
								<tr class="underline">\
									<td colspan="5"></td>\
								</tr>\
								{{/folders}}\
								{{#documents}}\
								<tr class="overline">\
									<td><input class="select-file" type="checkbox" name="files[]" value="{{_id}}" /></td>\
									<td><i role="{{#metadata}}{{content-type}}{{/metadata}}"></i></td>\
									<td><a href="document/{{_id}}">{{name}}</a></td>\
									<td>{{#formatDate}}{{modified}}{{/formatDate}}</td>\
								</tr>\
								<tr class="comments{{_id}} underline">\
									<td colspan="4" class="container-cell">\
										<a call="comment" href="{{_id}}" class="small button cell">{{#i18n}}workspace.document.comment{{/i18n}}</a>\
										<a href="share?id={{_id}}" call="share" class="small button cell">{{#i18n}}workspace.share{{/i18n}}</a>\
										{{#anyComment}}\
										<a call="showComment" href=".comments{{_id}}" class="cell right-magnet action-cell">{{#i18n}}workspace.document.comment.show{{/i18n}} ({{commentsCount}})</a>\
										{{/anyComment}}\
										<h2><span>{{#i18n}}workspace.comments{{/i18n}}</span><i class="right-magnet" role="close" call="hideComment"></i></h2>\
										<ul class="row">\
										{{#comments}}\
											<li class="twelve cell"><em>{{author}} - {{#formatDate}}{{posted}}{{/formatDate}} - </em><span>{{{comment}}}</span></li>\
										{{/comments}}\
										</ul>\
									</td>\
								</tr>\
								{{/documents}}\
							</tbody>\
						</table>\
						<header>&nbsp;</header>\
						<ul>\
						{{#folders}}\
						<li>\
							<a><i role="folder-large" href="documents/{{path}}?hierarchical=true" call="documents"></i></a>\
							<a href="documents/{{path}}?hierarchical=true" call="documents">{{name}}</a>\
						</li>\
						{{/folders}}\
						{{#documents}}\
						<li>\
							<a href="document/{{_id}}" call>\
								<i role="{{#metadata}}{{content-type}}{{/metadata}}-large">\
									<img src="{{thumbnail}}" alt="thumbnail" />\
								</i>\
							</a>\
							<input class="select-file" type="checkbox" name="files[]" value="{{_id}}" />\
							<a href="document/{{_id}}">{{name}}</a>\
						</li>\
						{{/documents}}\
						<div class="clear"></div>\
						</ul>',

			rack : '<table class="striped alternate" summary="">\
						<thead>\
							<tr>\
								<th scope="col">\
									<input type="checkbox" class="selectAllCheckboxes" />\
								</th>\
								<th scope="col"></th>\
								<th scope="col">{{#i18n}}name{{/i18n}}</th>\
								<th scope="col">{{#i18n}}from{{/i18n}}</th>\
								<th scope="col">{{#i18n}}to{{/i18n}}</th>\
								<th scope="col">{{#i18n}}sent{{/i18n}}</th>\
							</tr>\
						</thead>\
						<tbody>\
							{{#.}}\
							<tr>\
								<td><input class="select-file" type="checkbox" name="files[]" value="{{_id}}" /></td>\
								<td><i role="{{#metadata}}{{content-type}}{{/metadata}}"></i></td>\
								<td><a href="rack/{{_id}}">{{name}}</a></td>\
								<td>{{from}}</td>\
								<td>{{to}}</a></td>\
								<td>{{sent}}</td>\
							</tr>\
							{{/.}}\
						</tbody>\
					</table>\
					<header>&nbsp;</header>\
						<ul>\
						{{#folders}}\
						<li>\
							<a><i role="folder-large" href="documents/{{path}}?hierarchical=true" call="documents"></i></a>\
							<a href="documents/{{path}}?hierarchical=true" call="documents">{{name}}</a>\
						</li>\
						{{/folders}}\
						{{#documents}}\
						<li>\
							<a href="document/{{_id}}" call>\
								<i role="{{#metadata}}{{content-type}}{{/metadata}}-large">\
									<img src="{{thumbnail}}" alt="thumbnail" />\
								</i>\
							</a>\
							<input class="select-file" type="checkbox" name="files[]" value="{{_id}}" />\
							<a href="document/{{_id}}">{{name}}</a>\
						</li>\
						{{/documents}}\
						<div class="clear"></div>\
						</ul>',

			trash :'<table class="striped alternate monoline" summary="">\
						<thead>\
							<tr>\
								<th scope="col">\
									<input type="checkbox" class="selectAllCheckboxes" />\
								</th>\
								<th></th>\
								<th scope="col">{{#i18n}}name{{/i18n}}</th>\
								<th scope="col">{{#i18n}}modified{{/i18n}}</th>\
							</tr>\
						</thead>\
						<tbody>\
							{{#documents}}\
							<tr>\
								<td><input class="select-file" type="checkbox" name="files[]" value="document/{{_id}}" /></td>\
								<td><i role="{{#metadata}}{{content-type}}{{/metadata}}"></i></td>\
								<td><a href="document/{{_id}}">{{name}}</a></td>\
								<td>{{modified}}</td>\
							</tr>\
							{{/documents}}\
							{{#rack}}\
							<tr>\
								<td><input class="select-file" type="checkbox" name="files[]" value="rack/{{_id}}" /></td>\
								<td>{{#metadata}}{{content-type}}{{/metadata}}</td>\
								<td><a href="rack/{{_id}}">{{name}}</a></td>\
								<td>{{#formatDate}}{{modified}}{{/formatDate}}</td>\
							</tr>\
							{{/rack}}\
						</tbody>\
					</table>\
					<header>&nbsp;</header>\
						<ul>\
						{{#folders}}\
						<li>\
							<a><i role="folder-large" href="documents/{{path}}?hierarchical=true" call="documents"></i></a>\
							<a href="documents/{{path}}?hierarchical=true" call="documents">{{name}}</a>\
						</li>\
						{{/folders}}\
						{{#documents}}\
						<li>\
							<a href="document/{{_id}}" call>\
								<i role="{{#metadata}}{{content-type}}{{/metadata}}-large">\
									<img src="{{thumbnail}}" alt="thumbnail" />\
								</i>\
								<input class="select-file" type="checkbox" name="files[]" value="{{_id}}" />\
							</a>\
							<a href="document/{{_id}}">{{name}}</a>\
						</li>\
						{{/documents}}\
						<div class="clear"></div>\
						</ul>',
			addDocument : '	<form id="upload-form" method="post" action="document" enctype="multipart/form-data" class="cancel-flow">\
							<h1>{{#i18n}}workspace.add.document{{/i18n}}</h1>\
							<div class="row">\
								<div class="four cell">\
									<label>{{#i18n}}workspace.document.name{{/i18n}}</label>\
								</div>\
								<div class="eight cell">\
									<input type="text" name="name" />\
								</div>\
							</div>\
				            <div class="row">\
								<div class="eight cell right-magnet">\
									<div class="hidden-content">\
										<input type="file" name="file" id="new-file" />\
									</div>\
									<button class="file-button" data-linked="new-file">{{#i18n}}choose{{/i18n}}</button>\
									<em id="new-file-content">{{#i18n}}nofile{{/i18n}}</em>\
								</div>\
							</div>\
							<input call="sendFile" type="button" value="{{#i18n}}upload{{/i18n}}" />\
							</form>',

			sendRack : '<form id="upload-form" method="post" action="rack" enctype="multipart/form-data">\
						<label>{{#i18n}}workspace.rack.name{{/i18n}}</label>\
						<input type="text" name="name" />\
						<label>{{#i18n}}workspace.rack.to{{/i18n}}</label>\
						<select name="to">\
						{{#users}}\
							<option value="{{id}}">{{username}}</option>\
						{{/users}}\
						</select>\
						<label>{{#i18n}}workspace.rack.file{{/i18n}}</label>\
						<input type="file" name="file" />\
						<input call="sendFile" type="button" value="{{#i18n}}upload{{/i18n}}" />\
						</form>',

			comment : '<form method="post" action="document/{{id}}/comment">\
							<h1>{{#i18n}}workspace.comment{{/i18n}}</h1>\
							<textarea name="comment"></textarea>\
							<input call="sendComment" type="button" value="{{#i18n}}send{{/i18n}}" />\
						</form>',

			moveDocuments : '<form action="{{action}}" class="cancel-flow" data-current-path="{{currentPath}}">\
								<h1>{{#i18n}}workspace.move{{/i18n}}</h1>\
								<nav class="vertical">\
									<ul id="foldersTree" class="folders"></ul>\
								</nav>\
								<input call="moveOrCopyDocuments" type="button" value="{{#i18n}}workspace.move{{/i18n}}" />\
							</form>',
			copyDocuments : '<form action="{{action}}" class="cancel-flow" data-current-path="{{currentPath}}">\
								<h1>{{#i18n}}workspace.copy{{/i18n}}</h1>\
								<nav class="vertical">\
									<ul id="foldersTree" class="folders"></ul>\
								</nav>\
								<input call="moveOrCopyDocuments" type="button" value="{{#i18n}}workspace.copy{{/i18n}}" />\
							</form>'
		},
		action : {
			documents : function (o, callback) {
				var relativePath = undefined,
					that = this,
					directories;
				One.get(o.url).done(function(response){
					if (o.url.match(/^documents\/.*?/g)) {
						relativePath = o.url.substring(o.url.indexOf("/", 9) + 1, o.url.lastIndexOf("?"));
					}
					that.getFolders(o.url.indexOf("hierarchical=true") != -1, relativePath, function(data) {
						directories = _.filter(data, function(dir) {
							return dir !== relativePath && dir !== "Trash";
						});
						directories = _.map(directories, tools.mapFolderName);

						response = tools.formatResponse(response);
						$('#list').html(app.template.render("documents", { documents : response, folders : directories }));
						navigation.redirect(o.url);
						messenger.requireResize();

						if(typeof callback === 'function'){
							callback();
						}
					});
				});
			},
			iconsView: function(o){
				$('.list').removeClass('list-view').addClass('icons-view');
				messenger.requireResize();
			},
			listView: function(o){
				$('.list').removeClass('icons-view').addClass('list-view');
				messenger.requireResize();
			},
			rack : function (o) {
				One.get(o.url).done(function(response){
					response = tools.formatResponse(response);
					$('#list').html(app.template.render("rack", response));
					messenger.requireResize();
				});
			},
			share: function(o){
				One.get(o.url).done(function(data){
					$('#form-window').html(data);
					ui.showLightbox();

					$('#form-window table').addClass('monoline');
					$('.lightbox-backdrop, input[type=submit]').one('click', function(){
						ui.hideLightbox();
					})
				})
			},
			trash : function (o) {
				One.get("documents/Trash").done(function(documents) {
					One.get("rack/documents/Trash").done(function(rack) {
						documents = tools.formatResponse(documents);
						$('#list').html(app.template.render("trash",
								{ documents : documents, rack : rack }));
						messenger.requireResize();
					});
				});
			},

			addDocument : function (o) {
				$('#form-window').html(app.template.render("addDocument", {}));
				ui.showLightbox();
				messenger.requireResize();
				$('.lightbox-backdrop').one('click', function(){
					ui.hideLightbox();
				})
			},

			sendRack : function(o){
				One.get("users/available-rack").done(function(response) {
					if (response.status === "ok") {
						var users = [];
						for (var key in response.result) {
							users.push(response.result[key]);
						}
						$('#form-window').html(app.template.render("sendRack", { users : users }));
						ui.showLightbox();
						messenger.requireResize();
						$('.lightbox-backdrop').one('click', function(){
							ui.hideLightbox();
						});
					}
				});
			},

			sendFile : function(o) {
				var form = $('#upload-form'),
					fd = new FormData(),
					action = form.attr('action');
				fd.append('file', form.find('input[type=file]')[0].files[0]);
				if ("rack" === action) {
					action += '/' + form.find('input[name=to], select[name=to]').val();
				}

				ui.hideLightbox();
				One.postFile(action + '?' + form.serialize(), fd, {})
					.done(function(e){
						if(action !== 'rack'){
							var path = '';
							$('nav.vertical li.selected').each(function(index, item){
								if(index === 0){return;}
								if(path !== ''){
									path += '_';
								}
								path += $(this).contents(':not(ul)').text().trim()
							});

							if(path === ''){
								navigation.refresh();
							}
							else{
								One.put("documents/move/" + e._id + "/" + path)
									.done(function(){
										navigation.refresh()
									});
							}
						}
						else{
							navigation.refresh();
						}
					})
			},

			getFolders : function(hierarchical, relativePath, action) {
				var params = {
					hierarchical: hierarchical
				};
				if(relativePath){
					params.relativePath = relativePath;
				}

				One.get('folders', params)
					.done(action)
					.error(function(data) {app.notify.error(data)});
			},
			moveTrash : function(o) {
				if(navigation.currentUrl().indexOf('trash') !== -1){
					navigation.refresh();
					return;
				}
				var files = [];
				$("#list :checkbox:checked").each(function(i) {
					var obj = $(this);
					One.put(o.url + "/" + obj.val())
						.done(function(data){
							navigation.refresh();
						});
				});
			},

			comment : function(o) {
				$('#form-window').html(app.template.render("comment", { id : o.url }));
				ui.showLightbox();
				messenger.requireResize();
				$('.lightbox-backdrop').one('click', function(){
					ui.hideLightbox();
				})
			},

			sendComment : function(o) {
				ui.hideLightbox();
				var form = $(o.target).parents("form"),
					url = form.attr('action'),
					data = encodeURI(form.serialize()).replace(/(%0D%0A|%250D%250A)/gi, "<br />");

				var that = this;

				One.post(url, data)
				.done(function (data) {
					that.documents({url: navigation.currentUrl()}, function(){
						var targetFile = url.split('/')[1];
						var commentsLine = $('.comments' + targetFile);
						commentsLine.find('h2').show();
						commentsLine.find('ul').show();
					});
				})
				.error(function (data) {
					app.notify.error(data);
				});
			},

			showComment : function(o) {
				$(o.target).parent().find('ul, h2').slideDown('fast', function(){
					messenger.requireResize();
				});
			},
			hideComment: function(o){
				$(o.target).parent().parent().find('ul, h2').slideUp();
				messenger.requireResize();
			},
			remove : function (o) {
				var files = [];

				$("#list :checkbox:checked").each(function(i) {
					var obj = $(this);
					One.delete(obj.val())
						.done(function(){
							obj.parents("tr").remove();
						});
				});
			},

			move : function(o) {
					$('#form-window').html(app.template.render("moveDocuments", { action : 'documents/move', currentPath: o.url }));
					var showFolders = function(path){
					var nodePattern = function(node, subNodes){
						if(node.name){
							return '<li class="row">\
										<input type="radio" name="folder" />\
										<span class="folderPath"><a class="showCreate"><strong>+</strong></a>' + node.name + '</span>\
										<ul style="display:block" class="row">\
											<li class="row createFolder">\
												<input type="radio" name="folder" />\
												<em class="folderPath editable" contenteditable>Nouveau dossier</em>\
											</li>\
											' + subNodes() + '\
										</ul>\
									</li>';
						}
						else{
							return '\
								<li class="row">\
									<input type="radio" name="folder" />\
									<em class="folderPath editable" contenteditable>Nouveau dossier</em>\
								</li>\
								' + subNodes();
						}

					};

					var container = $('#foldersTree');
					tools.displayFoldersTree(nodePattern, container);
				};
				showFolders();

				ui.showLightbox();

				messenger.requireResize();
				$('.lightbox-backdrop').one('click', function(){
					ui.hideLightbox();
				})
			},
			copy : function(o) {
				$('#form-window').html(app.template.render("copyDocuments", { action : 'documents/copy', currentPath: o.url }));
				var showFolders = function(path){
					var nodePattern = function(node, subNodes){
						if(node.name){
							return '<li class="row">\
										<input type="checkbox" />\
										<span class="folderPath">\
										<a class="showCreate"><strong>+</strong></a>\
										' + node.name + '</span>\
										<ul style="display:block" class="row">\
											<li class="row createFolder">\
												<input type="checkbox" />\
												<em class="folderPath editable" contenteditable>Nouveau dossier</em>\
											</li>\
											' + subNodes() + '\
										</ul>\
									</li>';
						}
						else{
							return '\
								<li class="row">\
									<input type="checkbox" />\
									<em class="folderPath editable" contenteditable>Nouveau dossier</em>\
								</li>\
								' + subNodes();
						}

					};

					var container = $('#foldersTree');
					tools.displayFoldersTree(nodePattern, container);
				};
				showFolders();

				ui.showLightbox();

				messenger.requireResize();
				$('.lightbox-backdrop').one('click', function(){
					ui.hideLightbox();
				})
			},
			moveOrCopyDocuments : function(o) {
				ui.hideLightbox();
				var ids = "",
					form = $(o.target).parents("form"),
					action = form.attr("action"),
					method;

				if (action.match(/copy$/g)) {
					method = "post";
				} else {
					method = "put";
				}

				$("#list :checkbox:checked").each(function(i) {
					ids += "," + $(this).val();
				});
				if (ids != "") {
					ids = ids.substring(1);
				}

				$('.folders :checkbox:checked, .folders :radio:checked').each(function(){
					if(!$(this).parent('li').children('.folderPath').contents(':not(a)').text()){
						return;
					}

					var parentPath = function(element){
						var elementText = element.children('.folderPath').contents(':not(a)').text();
						if(element.parent().closest('li').length === 0){
							return  elementText;
						}
						return parentPath(element.parent().closest('li')) + '_' + elementText;
					}

					var path = parentPath($(this).parent('li'));

					One[method](action + "/" + ids + "/" + path)
						.done(function(){
							location.reload(true);
						});
				})
			}

		}
	});
	return app;
}();

$(document).ready(function(){
	workspace.init();
	workspace.action.documents({url : "documents?hierarchical=true"});
	workspace.action.getFolders(true, undefined, function(data) {
		navigation.redirect('documents?hierarchical=true');
	});

	navigation.showFolders(undefined);

	$('.workspace').on('change', '.select-file, .selectAllCheckboxes', function(){
		if($(this).hasClass('selectAllCheckboxes')){
			$('#list :checkbox').prop('checked', this.checked);
		}
		if($('.select-file:checked').length > 0){
			$('.contextual').removeAttr('disabled');
		}
		else{
			$('.contextual').attr('disabled', 'disabled');
		}
	});

	$('.workspace').on('click', '.showCreate', function(){
		$(this).parent().parent().find('.createFolder').first().show();
	})

	$('.workspace').on('mousedown', '.editable', function(){
		$(this).text('');
		$(this).focus();
		$(this).parent().find('input').prop('checked', true);
		$(this).addClass('active');
	});
});

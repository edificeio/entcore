var tools = (function(){
	var myId;

	var findAuthorizations = function(authorizations, owner){
		if(!(authorizations instanceof Array) || owner === myId){
			return {
				comment: true,
				share: true
			}
		}

		var myRights = {
			comment: false,
			share: false
		}

		authorizations.forEach(function(auth){
			if(auth.userId !== myId){
				return;
			}

			for(var property in auth){
				if(property.indexOf('comment') !== -1){
					myRights.comment = true;
				}
			}
		})

		return myRights;
	}

	return {
		myId: function(id){
			if(!id){
				return myId;
			}
			myId = id;
		},
		roleFromFileType: function(fileType){
			var types = {
				'doc': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('wordprocessing') !== -1;
				},
				'xls': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('spreadsheet') !== -1;
				},
				'img': function(type){
					return type.indexOf('image') !== -1;
				},
				'pdf': function(type){
					return type.indexOf('pdf') !== -1;
				},
				'ppt': function(type){
					return type.indexOf('document') !== -1 && type.indexOf('presentation') !== -1;
				},
				'video': function(type){
					return type.indexOf('video') !== -1;
				},
				'audio': function(type){
					return type.indexOf('audio') !== -1;
				}
			}

			for(var type in types){
				if(types[type](fileType)){
					return type;
				}
			}

			return 'unknown';
		},
		refresh: function(){
			window.location.reload();
		},
		contextualButtons: function(contextName){
			var contextsButtons = {
				documents: [
					{ text: 'workspace.add.document', call: 'addDocument', icon: true },
					{ text: 'workspace.send.rack', call: 'sendRack' },
					{ text: 'workspace.move.trash', call: 'moveTrash', url: 'document/trash', contextual: true },
					{ text: 'workspace.move', call: 'move', url: 'documents/move', contextual: true },
					{ text: 'workspace.copy', call: 'copy', url: 'documents/copy', contextual: true }
				],
				rack: [
					{ text: 'workspace.send.rack', call: 'sendRack' },
					{ text: 'workspace.move.racktodocs', call: 'copy', url: 'rack/documents/copy', contextual: true },
					{ text: 'workspace.move.trash', call: 'moveTrash', url: 'rack/trash', contextual: true }
				],
				trash: [
					{ text: 'workspace.move.trash', call: 'remove', contextual: true },
					{ text: 'workspace.trash.restore', call: 'move', url: 'documents/move', contextual: true }
				]
			}

			return contextsButtons[contextName];
		},
		formatResponse: function(response, callback){
			response.forEach(function(item){
				item.metadata['content-type'] = tools.roleFromFileType(item.metadata['content-type']);
				var fileNameSplit = item.metadata.filename.split('.');
				item.metadata.extension = fileNameSplit[fileNameSplit.length - 1];
				if(item.metadata['content-type'] === 'img'){
					item.thumbnail = 'document/' + item._id;
				}

				if(item.from){
					if(item.metadata['content-type'] === 'img'){
						item.thumbnail = 'rack/' + item._id;
					}
					One.get('/userbook/api/person?id=' + item.from)
						.done(function(fromData){
							One.get('/userbook/api/person?id=' + item.to)
								.done(function(toData){
									item.from = fromData.result[0].displayName;
									item.to = toData.result[0].displayName;
									callback(response);
								});
						});
				}

				if(item.created){
					item.created = item.created.split(' ')[0];
					item.modified = item.modified.split(' ')[0];
				}
				if(item.sent){
					item.sent = item.sent.split(' ')[0];
				}

				item.anyComment = function(){
					return this.commentsCount !== 0;
				};

				item.myRights = findAuthorizations(item.shared, item.owner);

				if(!item.comments){
					item.commentsCount = 0;
					return;
				}

				item.commentsCount = item.comments.length;

				for(var j = 0; j < item.comments.length; j++){
					item.comments[j].posted = item.comments[j].posted.split(' ')[0];
					if(item.comments[j].author === '') {
						item.comments[j].author = ''
					}
				}
			})

			callback(response);
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
									<th scope="col" class="five">{{#i18n}}name{{/i18n}}</th>\
									<th scope="col" class="four">{{#i18n}}owner{{/i18n}}</th>\
									<th scope="col" class="two">{{#i18n}}modified{{/i18n}}</th>\
								</tr>\
							</thead>\
							<tbody>\
								{{#folders}}\
								<tr class="overline">\
									<td></td>\
									<td><i role="folder"></i></td>\
									<td colspan="2"><strong><a call="documents" href="documents/{{path}}?hierarchical=true">{{name}}</a></strong></td>\
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
									<td><a href="document/{{_id}}" call>{{name}}</a><em>{{#metadata}}{{extension}}{{/metadata}}</em></td>\
									<td><a href="/userbook/annuaire#{{owner}}">{{ownerName}}</a></td>\
									<td>{{#formatDate}}{{modified}}{{/formatDate}}</td>\
								</tr>\
								<tr class="comments{{_id}} underline">\
									<td colspan="5" class="container-cell">\
										{{#myRights}}{{#comment}}<a call="comment" href="{{_id}}" class="small button cell">{{#i18n}}workspace.document.comment{{/i18n}}</a>{{/comment}}{{/myRights}}\
										{{#myRights}}{{#share}}<a href="share?id={{_id}}" call="share" class="small button cell">{{#i18n}}workspace.share{{/i18n}}{{/share}}{{/myRights}}</a>\
										{{#anyComment}}\
										<span class="cell right-magnet action-cell">\
											<a class="show" call="showComment" href=".comments{{_id}}">{{#i18n}}workspace.document.comment.show{{/i18n}} ({{commentsCount}})</a>\
											<a class="hide" call="showComment" href=".comments{{_id}}" style="display:none">{{#i18n}}workspace.document.comment.hide{{/i18n}}</a>\
										</span>\
										{{/anyComment}}\
										<h2><span>{{#i18n}}workspace.comments{{/i18n}}</span></h2>\
										<ul class="row">\
										{{#comments}}\
											<li class="twelve cell"><em>{{authorName}} - {{#formatDate}}{{posted}}{{/formatDate}} - </em><span>{{{comment}}}</span></li>\
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
							<a href="document/{{_id}}" call class="{{#metadata}}{{content-type}}{{/metadata}}-container">\
								<i role="{{#metadata}}{{content-type}}{{/metadata}}-large">\
									<div><img src="{{thumbnail}}" alt="thumbnail" /></div>\
								</i>\
							</a>\
							<input class="select-file" type="checkbox" name="files[]" value="{{_id}}" />\
							<a href="document/{{_id}}" call>{{name}}</a>\
							<a href="/userbook/annuaire#{{owner}}"><strong>{{ownerName}}</strong></a>\
						</li>\
						{{/documents}}\
						<div class="clear"></div>\
						</ul>',

			rack : '<table>\
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
								<td><a href="rack/{{_id}}" call>{{name}}</a><em>{{#metadata}}{{extension}}{{/metadata}}</em></td>\
								<td>{{fromName}}</td>\
								<td>{{toName}}</a></td>\
								<td>{{sent}}</td>\
							</tr>\
							<tr></tr>\
							{{/.}}\
						</tbody>\
					</table>\
					<header>&nbsp;</header>\
						<ul>\
						{{#.}}\
						<li>\
							<a href="rack/{{_id}}" call class="{{#metadata}}{{content-type}}{{/metadata}}-container">\
								<i role="{{#metadata}}{{content-type}}{{/metadata}}-large">\
									<div><img src="{{thumbnail}}" alt="thumbnail" /></div>\
								</i>\
							</a>\
							<input class="select-file" type="checkbox" name="files[]" value="{{_id}}" />\
							<a href="document/{{_id}}" call>{{name}}</a>\
						</li>\
						{{/.}}\
						<div class="clear"></div>\
						</ul>',

			trash :'<table>\
						<thead>\
							<tr>\
								<th scope="col">\
									<input type="checkbox" class="selectAllCheckboxes" />\
								</th>\
								<th></th>\
								<th scope="col" class="five">{{#i18n}}name{{/i18n}}</th>\
								<th scope="col" class="four">{{#i18n}}owner{{/i18n}}</th>\
								<th scope="col" class="two">{{#i18n}}modified{{/i18n}}</th>\
							</tr>\
						</thead>\
						<tbody>\
							{{#documents}}\
							<tr>\
								<td><input class="select-file" type="checkbox" name="files[]" value="document/{{_id}}" /></td>\
								<td><i role="{{#metadata}}{{content-type}}{{/metadata}}"></i></td>\
								<td><a href="document/{{_id}}" call>{{name}}</a><em>{{#metadata}}{{extension}}{{/metadata}}</em></td>\
								<td><a href="/userbook/annuaire#{{owner}}">{{ownerName}}</a></td>\
								<td>{{modified}}</td>\
							</tr>\
							<tr></tr>\
							{{/documents}}\
							{{#rack}}\
							<tr>\
								<td><input class="select-file" type="checkbox" name="files[]" value="rack/{{_id}}" /></td>\
								<td>{{#metadata}}{{content-type}}{{/metadata}}</td>\
								<td><a href="rack/{{_id}}" call>{{name}}</a></td>\
								<td>{{#formatDate}}{{modified}}{{/formatDate}}</td>\
							</tr>\
							<tr></tr>\
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
									<div><img src="{{thumbnail}}" alt="thumbnail" /></div>\
								</i>\
								<input class="select-file" type="checkbox" name="files[]" value="{{_id}}" />\
							</a>\
							<a href="document/{{_id}}" call>{{name}}</a>\
							<br /><a href="/userbook/annuaire#{{owner}}">{{ownerName}}</a>\
						</li>\
						{{/documents}}\
						<div class="clear"></div>\
						</ul>',
			addDocument : ' <div class="fixed-block height-four">\
								<form id="upload-form" method="post" action="document" enctype="multipart/form-data" class="fixed twelve cell">\
									<div class="twelve fluid cell">\
									<h1>{{#i18n}}workspace.add.document{{/i18n}}</h1>\
									<div class="row">\
										<div class="eight cell select-file">\
											<div class="hidden-content">\
												<input type="file" name="file" id="new-file" />\
											</div>\
											<button class="file-button">{{#i18n}}choose{{/i18n}}</button>\
											<input type="text" name="name" data-display-file value="{{#i18n}}nofile{{/i18n}}" />\
										</div>\
									</div>\
									<div class="lightbox-buttons">\
										<input class="cancel" type="button" value="{{#i18n}}cancel{{/i18n}}" />\
										<input call="sendFile" type="submit" value="{{#i18n}}upload{{/i18n}}" />\
									</div>\
									</div>\
								</form>\
							</div>',

			sendRack : '<div class="fixed-block height-four">\
							<form id="upload-form" method="post" action="rack" enctype="multipart/form-data" class="fixed twelve cell">\
								<div class="twelve cell fluid">\
									<h1>{{#i18n}}workspace.send.rack{{/i18n}}</h1>\
									<div class="row">\
										<label class="four cell">{{#i18n}}workspace.rack.file{{/i18n}}</label>\
										<div class="eight cell right-magnet select-file">\
											<div class="hidden-content">\
												<input type="file" name="file" id="new-file" />\
											</div>\
											<button class="file-button">{{#i18n}}choose{{/i18n}}</button>\
											<input type="text" name="name" data-display-file value="{{#i18n}}nofile{{/i18n}}" />\
										</div>\
									</div>\
									<div class="row">\
										<label class="four cell">{{#i18n}}workspace.rack.to{{/i18n}}</label>\
										<select class="eight cell" name="to" data-autocomplete>\
										{{#users}}\
											<option value="{{id}}">{{username}}</option>\
										{{/users}}\
										</select>\
									</div>\
									<div class="lightbox-buttons">\
										<input type="button" class="cancel" value="{{#i18n}}cancel{{/i18n}}" />\
										<input call="sendFile" type="button" value="{{#i18n}}upload{{/i18n}}" />\
									</div>\
								</div>\
							</form>\
						</div>',

			comment : '<form method="post" action="document/{{id}}/comment">\
							<h1>{{#i18n}}workspace.comment{{/i18n}}</h1>\
							<textarea name="comment"></textarea>\
							<div class="lightbox-buttons fluid">\
								<input class="cancel" type="button" value="{{#i18n}}cancel{{/i18n}}" />\
								<input call="sendComment" type="submit" value="{{#i18n}}send{{/i18n}}" />\
								<div class="clear"></div>\
							</div>\
						</form>',

			moveDocuments : '<form action="{{action}}" data-current-path="{{currentPath}}">\
								<div class="row">\
									<h1>{{#i18n}}workspace.move{{/i18n}}</h1>\
									<nav class="vertical">\
										<ul id="foldersTree" class="folders"></ul>\
									</nav>\
									<div class="lightbox-buttons fluid">\
										<input type="button" class="cancel" value="{{#i18n}}cancel{{/i18n}}" />\
										<input call="moveOrCopyDocuments" type="button" value="{{#i18n}}workspace.move{{/i18n}}" />\
										<div class="clear"></div>\
									</div>\
								</div>\
							</form>',
			copyDocuments : '<form action="{{action}}" class="cancel-flow" data-current-path="{{currentPath}}">\
								<h1>{{#i18n}}workspace.copy{{/i18n}}</h1>\
								<nav class="vertical">\
									<ul id="foldersTree" class="folders"></ul>\
								</nav>\
								<div class="lightbox-buttons fluid">\
									<input type="button" class="cancel" value="{{#i18n}}cancel{{/i18n}}" />\
									<input call="moveOrCopyDocuments" type="button" value="{{#i18n}}workspace.copy{{/i18n}}" />\
									<div class="clear"></div>\
								</div>\
							</form>',
			contextualButtons: '\
							{{#.}}\
							<a call="{{call}}" href="{{url}}" class="button {{#contextual}}contextual{{/contextual}}" {{#contextual}}disabled{{/contextual}}>\
									{{#icon}}<i role="add"></i>{{/icon}}{{#i18n}}{{text}}{{/i18n}}\
								</a>\
							{{/.}}'
		},
		action : {
			documents : function (o, callback) {
				var relativePath = undefined,
					that = this,
					directories;
				$('.action-buttons').html(app.template.render('contextualButtons', tools.contextualButtons('documents')))
				One.get(o.url).done(function(response){
					if (o.url.match(/^documents\/.*?/g)) {
						relativePath = o.url.substring(o.url.indexOf("/", 9) + 1, o.url.lastIndexOf("?"));
					}
					that.getFolders(o.url.indexOf("hierarchical=true") != -1, relativePath, function(data) {
						directories = _.filter(data, function(dir) {
							return dir !== relativePath && dir !== "Trash";
						});
						directories = _.map(directories, tools.mapFolderName);

						tools.formatResponse(response, function(data){
							$('#list').html(app.template.render("documents", { documents : response, folders : directories }));
							navigation.redirect(o.url);
							messenger.requireResize();

							if(typeof callback === 'function'){
								callback();
							}
						});
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
				$('.action-buttons').html(app.template.render('contextualButtons', tools.contextualButtons('rack')))
				One.get(o.url).done(function(response){
					tools.formatResponse(response, function(data){
						$('#list').html(app.template.render("rack", response));
						messenger.requireResize();
					});
				});
			},
			share: function(o){
				One.get(o.url).done(function(data){
					$('#form-window').html('<div class="twelve cell flowing"><h1>Partager</h1>' + data + '</div>');
					ui.showLightbox();

					$('#form-window table').addClass('monoline');
					$('.lightbox-backdrop, input[type=submit]').one('click', function(){
						ui.hideLightbox();
					})
				})
			},
			trash : function (o) {
				$('.action-buttons').html(app.template.render('contextualButtons', tools.contextualButtons('trash')))
				One.get("documents/Trash").done(function(documents) {
					One.get("rack/documents/Trash").done(function(rack) {
						tools.formatResponse(documents, function(data){
							$('#list').html(app.template.render("trash",
								{ documents : documents, rack : rack }));
							messenger.requireResize();
						});
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
					$('#form-window').html(app.template.render("sendRack", { users : response }));
					ui.showLightbox();
					messenger.requireResize();
					$('.lightbox-backdrop').one('click', function(){
						ui.hideLightbox();
					});
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

				$('.lightbox-window input[type=submit]').val('Chargement en cours').attr('disabled', true);
				One.postFile(action + '?' + form.serialize(), fd, {})
					.done(function(e){
						ui.hideLightbox();
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
							app.notify.info(app.i18n.translate('workspace.removed.message'));
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
				var linksContainer = $(o.target).parent();
				linksContainer.parent().find('ul, h2').slideToggle('fast', function(){
					if($(this).css('display') === 'none'){
						linksContainer.children('.show').show();
						linksContainer.children('.hide').hide();
					}
					else{
						linksContainer.children('.show').hide();
						linksContainer.children('.hide').show();
					}

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

			move: function(o) {
					$('#form-window').html(app.template.render("moveDocuments", { action : o.url, currentPath: o.url }));
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
				$('#form-window').html(app.template.render("copyDocuments", { action : o.url, currentPath: o.url }));
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
					var id = $(this).val().split('/');
					id = id[id.length - 1];
					ids += "," + id;
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

	One.get('/userbook/api/person').done(function(data){
		tools.myId(data.result[0].id);

		workspace.action.documents({url : "documents?hierarchical=true&filter=owner"});
		workspace.action.getFolders(true, undefined, function(data) {
			navigation.redirect('documents?hierarchical=true&filter=owner');
		});

		navigation.showFolders(undefined);
	})

	$('.workspace').on('change', '.select-file, .selectAllCheckboxes', function(){
		var applyCheckedClasses = function(node, check){
			if(node.hasClass('checked')){
				node.removeClass('checked');
				node.next().removeClass('checked');
			}
			else{
				node.addClass('checked');
				node.next().addClass('checked');
			}
		}

		if($(this).hasClass('selectAllCheckboxes')){
			$('#list :checkbox').prop('checked', this.checked);
			if(this.checked){
				$('tr').addClass('checked')
			}
			else{
				$('tr').removeClass('checked')
			}
		}
		else{
			applyCheckedClasses($(this).parent().parent());
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
		messenger.requireResize();
	})

	$('.workspace').on('mousedown', '.editable', function(){
		$(this).text('');
		$(this).focus();
		$(this).parent().find('input').prop('checked', true);
		$(this).addClass('active');
	});
});

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
				response[i].created = response[i].created.split(' ')[0];
				response[i].modified = response[i].modified.split(' ')[0];

				if(!response[i].comments){
					continue;
				}

				for(var j = 0; j < response[i].comments.length; j++){
					response[i].comments[j].posted = response[i].comments[j].posted.split(' ')[0];
					if(response[i].comments[j].author === '') {
						response[i].comments[j].author = ''
					}
				}
			}
			return response;
		}
	}
}());

var navigation = (function(){
	var currentURL = '/documents';
	var updater = {
		redirect: function(action){
			$('nav.vertical a, nav.vertical li').removeClass('selected');
			var current = $('nav.vertical a[href="' + action + '"]');
			current.addClass('selected');
			current.parent().addClass('selected');
			current.parent('li').parent('ul').parent('li').addClass('selected');
			currentURL = action;
		},
		currentUrl: function(){
			return currentURL;
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
										<a href="/share?id={{_id}}" call="share" class="small button cell">{{#i18n}}workspace.share{{/i18n}}</a>\
										<a call="showComment" href=".comments{{_id}}" class="cell right-magnet action-cell">{{#i18n}}workspace.document.comment.show{{/i18n}}</a>\
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
							<a href="/document/{{_id}}">\
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

			rack : '<table class="striped alternate" summary="">\
						<thead>\
							<tr>\
								<th scope="col">\
									<input type="checkbox" class="selectAllCheckboxes" />\
								</th>\
								<th scope="col">{{#i18n}}type{{/i18n}}</th>\
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
								<td>{{#metadata}}{{content-type}}{{/metadata}}</td>\
								<td><a href="rack/{{_id}}">{{name}}</a></td>\
								<td>{{from}}</td>\
								<td>{{to}}</a></td>\
								<td>{{sent}}</td>\
							</tr>\
							{{/.}}\
						</tbody>\
					</table>',

			trash :'<table class="striped alternate" summary="">\
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
					</table>',

			addDocument : '	<form id="upload-form" method="post" action="/document" enctype="multipart/form-data">\
							<h1>{{#i18n}}workspace.add.document{{/i18n}}</h1>\
							<label>{{#i18n}}workspace.document.name{{/i18n}}</label>\
							<input type="text" name="name" />\
							<label>{{#i18n}}workspace.document.file{{/i18n}}</label>\
							<input type="file" name="file" />\
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

			comment : '<form method="post" action="/document/{{id}}/comment">\
							<h1>{{#i18n}}workspace.comment{{/i18n}}</h1>\
							<textarea name="comment"></textarea>\
							<input call="sendComment" type="button" value="{{#i18n}}send{{/i18n}}" />\
						</form>',

			moveDocuments : '<form action="{{action}}" class="cancel-flow">\
								<h1>{{#i18n}}workspace.move{{/i18n}}</h1>\
								<table class="monoline folders large-box">\
									{{#folders}}\
										<tr>\
											<td class="icon">\
												<input type="radio" name="folder" />\
											</td>\
											<td class="icon">\
												<i role="folder"></i>\
											</td>\
											<td class="folderPath main-cell">{{.}}</td>\
										</tr>\
									{{/folders}}\
									<tr>\
										<td class="icon">\
											<input type="radio" name="folder" />\
										</td>\
										<td class="icon">\
										</td>\
										<td class="folderPath editable main-cell" contenteditable>Nouveau dossier</td>\
									</tr>\
								</table>\
								<input call="moveOrCopyDocuments" type="button" value="{{#i18n}}workspace.move{{/i18n}}" />\
							</form>',
			copyDocuments : '<form action="{{action}}" class="cancel-flow">\
								<h1>{{#i18n}}workspace.copy{{/i18n}}</h1>\
								<table class="monoline folders large-box">\
								{{#folders}}\
									<tr>\
										<td class="icon">\
											<input type="checkbox" />\
										</td>\
										<td class="icon">\
											<i role="folder"></i>\
										</td>\
										<td class="folderPath main-cell">\
											{{.}}\
										</td>\
									</tr>\
								{{/folders}}\
									<tr>\
										<td class="icon">\
											<input type="checkbox" />\
										</td>\
										<td class="icon">\
										</td>\
										<td class="folderPath editable main-cell" contenteditable>\
											Nouveau dossier\
										</td>\
									</tr>\
								</table>\
								<input call="moveOrCopyDocuments" type="button" value="{{#i18n}}workspace.copy{{/i18n}}" />\
							</form>'
		},
		action : {
			documents : function (o, callback) {
				var relativePath = undefined,
					that = this,
					directories;
				$.get(o.url).done(function(response){
					if (o.url.match(/^documents\/.*?/g)) {
						relativePath = o.url.substring(o.url.indexOf("/", 9) + 1, o.url.lastIndexOf("?"));
					}
					that.getFolders(o.url.indexOf("hierarchical=true") != -1, relativePath, function(data) {
						directories = _.filter(data, function(dir) {
							return dir !== relativePath && dir !== "Trash";
						});
						directories = _.map(directories, function(dir) {
							if (dir.indexOf("_") !== -1) {
								return { name : dir.substring(dir.lastIndexOf("_") + 1), path : dir };
							}
							return { name : dir, path : dir };
						});

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
				$.get(o.url).done(function(response){
					response = tools.formatResponse(response);
					$('#list').html(app.template.render("rack", response));
					messenger.requireResize();
				});
			},
			share: function(o){
				$.get(o.url, function(data){
					$('#form-window').html(data);
					ui.showLightbox();

					$('#form-window table').addClass('monoline');
					$('.lightbox-backdrop, input[type=submit]').one('click', function(){
						ui.hideLightbox();
					})
				})
			},
			trash : function (o) {
				$.get("/documents/Trash").done(function(documents) {
					$.get("/rack/documents/Trash").done(function(rack) {
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
				$.get("users/available-rack").done(function(response) {
					if (response.status === "ok") {
						var users = [];
						for (var key in response.result) {
							users.push(response.result[key]);
						}
						$('#form-window').html(app.template.render("sendRack", { users : users }));
						navigation.openLightbox();
						messenger.requireResize();
						$('.lightbox-backdrop').one('click', function(){
							navigation.closeLightbox();
						});
					}
				});
			},

			sendFile : function(o) {
				var form = $('#upload-form'),
					fd = new FormData(),
					action = form.attr('action');
				fd.append('file', form.children('input[type=file]')[0].files[0]);
				if ("rack" === action) {
					action += '/' + form.children('select[name=to]').val();
				}
				ui.hideLightbox();
				$.ajax({
					url: action + '?' + form.serialize(),
					type: 'POST',
					data: fd,
					cache: false,
					contentType: false,
					processData: false
				}).done(function(data) {
					location.reload(true);
				}).error(function(data) {app.notify.error(data)});
			},

			getFolders : function(hierarchical, relativePath, action) {
				var url = "folders?";
				if (hierarchical === true) {
					url += "hierarchical=true&";
				}
				if (typeof relativePath != 'undefined') {
					url += "relativePath=" + relativePath;
				}
				$.get(url)
				.done(action)
				.error(function(data) {app.notify.error(data)});
			},
			moveTrash : function(o) {
				if(navigation.currentUrl().indexOf('trash') !== -1){
					this.remove(o);
					return;
				}
				var files = [];
				$("#list :checkbox:checked").each(function(i) {
					var obj = $(this);
					$.ajax({
						url : o.url + "/" + obj.val(),
						type: "PUT",
						success: function() {
							var parentLine = obj.parents("tr");
							parentLine.next().remove();
							parentLine.remove();

							var parentCell = obj.parents("li");
							parentCell.remove();
						},
						error: function(data) {
							app.notify.error(data);
						}
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
				$.post(url, data)
				.done(function (data) {
					that.documents({url: navigation.currentUrl()}, function(){
						var targetFile = url.split('/')[2];
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
				$(o.target).parent().find('ul, h2').slideDown();
				messenger.requireResize();
			},
			hideComment: function(o){
				$(o.target).parent().parent().find('ul, h2').slideUp();
				messenger.requireResize();
			},
			remove : function (o) {
				var files = [];

				$("#list :checkbox:checked").each(function(i) {
					var obj = $(this);
					$.ajax({
						url : obj.val(),
						type: "DELETE",
						success: function() {
							obj.parents("tr").remove();
						},
						error: function(data) {
							app.notify.error(data);
						}
					});
				});
			},

			move : function(o) {
				workspace.action.getFolders(true, undefined, function(data) {
					var folders = _.reject(data, function(item){ return item === 'Trash' });
					$('#form-window').html(app.template.render("moveDocuments", { action : o.url, folders: folders }));
					ui.showLightbox();
				});

				messenger.requireResize();
				$('.lightbox-backdrop').one('click', function(){
					ui.hideLightbox();
				})
			},
			copy : function(o) {
				workspace.action.getFolders(true, undefined, function(data) {
					var folders = _.reject(data, function(item){ return item === 'Trash' });
					$('#form-window').html(app.template.render("copyDocuments", { action : o.url, folders: folders }));

					ui.showLightbox();
				});

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
					method = "POST";
				} else {
					method = "PUT";
				}

				$("#list :checkbox:checked").each(function(i) {
					ids += "," + $(this).val();
				});
				if (ids != "") {
					ids = ids.substring(1);
				}

				$('.folders :checkbox:checked, .folders :radio:checked').each(function(){
					$.ajax({
						url : action + "/" + ids + "/" + $(this).parents('tr').find('.folderPath').text(),
						type: method,
						success: function() {
							location.reload(true);
						},
						error: function(data) {
							app.notify.error(data);
						}
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
		var html = "";
		for (var i = 0; i < data.length; i++) {
			if (data[i] === "Trash") continue;
			html += '<li><a call="documents" href="/documents/' + data[i] + '?hierarchical=true">' + data[i] + "</a></li>";
		}
		$(".base-folders").html(html);
		navigation.redirect('documents?hierarchical=true');
	});

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

	$('.workspace').on('mousedown', '.editable', function(){
		$(this).html(' ');
		$(this).focus();
		$(this).parent().find('input').prop('checked', true);
	});
});

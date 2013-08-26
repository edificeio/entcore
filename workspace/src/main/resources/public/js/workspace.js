var tools = (function(){
	return {
		roleFromFileType: function(fileType){
			var types = {
				'doc': function(type){
					return type.indexOf('officedocument') !== -1 && type.indexOf('wordprocessing') !== -1;
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
	var updater = {
		redirect: function(action){
			$('nav.vertical a, nav.vertical li').removeClass('selected');
			var current = $('nav.vertical a[call=' + action + ']');
			current.addClass('selected');
			current.parent().addClass('selected');
		}
	};

	$(document).ready(function(){
		$('nav.vertical a').on('click', function(){
			updater.redirect($(this).attr('call'));
		})
	});

	updater.redirect('documents');

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
										<input type="checkbox" call="allCheckbox" />\
									</th>\
									<th scope="col">{{#i18n}}type{{/i18n}}</th>\
									<th scope="col">{{#i18n}}name{{/i18n}}</th>\
									<th scope="col">{{#i18n}}modified{{/i18n}}</th>\
									<th scope="col"></th>\
								</tr>\
							</thead>\
							<tbody>\
								{{#folders}}\
								<tr>\
									<td></td>\
									<td><i role="folder"></i></td>\
									<td><a call="documents" href="/documents/{{path}}?hierarchical=true">{{name}}</a></td>\
									<td></td>\
									<td></td>\
								</tr>\
								{{/folders}}\
								{{#documents}}\
								<tr class="overline">\
									<td><input class="select-file" type="checkbox" name="files[]" value="{{_id}}" /></td>\
									<td><i role="{{#metadata}}{{content-type}}{{/metadata}}"></i></td>\
									<td><a href="/document/{{_id}}">{{name}}</a></td>\
									<td>{{#formatDate}}{{modified}}{{/formatDate}}</td>\
									<td>\
										<a href="/share?id={{_id}}" class="magnet-right">{{#i18n}}workspace.share{{/i18n}}</a>\
									</td>\
								</tr>\
								<tr class="comments{{_id}} underline">\
									<td colspan="5" class="container-cell">\
										<a call="comment" href="{{_id}}" class="button cell">{{#i18n}}workspace.document.comment{{/i18n}}</a>\
										<a call="showComment" href=".comments{{_id}}" class="cell right-magnet action-cell">{{#i18n}}workspace.document.comment.show{{/i18n}}</a>\
										<h2><span>{{#i18n}}workspace.comments{{/i18n}}</span><i class="right-magnet" call="hideComment">X</i></h2>\
										<ul class="row">\
										{{#comments}}\
											<li class="twelve cell">{{author}} - {{#formatDate}}{{posted}}{{/formatDate}} - <span>{{{comment}}}</span></li>\
										{{/comments}}\
										</ul>\
									</td>\
								</tr>\
								{{/documents}}\
							</tbody>\
						</table>\
						<header></header>\
						<ul>\
						{{#folders}}\
						<li><i role="folder-large"></i><a>{{name}}</a></li>\
						{{/folders}}\
						{{#documents}}\
						<li><i role="{{#metadata}}{{content-type}}{{/metadata}}-large"></i><a>{{name}}</a></li>\
						{{/documents}}\
						<div class="clear"></div>\
						</ul>',

			rack : '<table class="striped alternate" summary="">\
						<thead>\
							<tr>\
								<th scope="col">\
									<input type="checkbox" call="allCheckbox" />\
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
								<td><a href="/rack/{{_id}}">{{name}}</a></td>\
								<td>{{from}}</td>\
								<td>{{to}}</a></td>\
								<td>{{sent}}</td>\
							</tr>\
							{{/.}}\
						</tbody>\
					</table>',

			trash :'<div>\
					<a call="remove" href="">{{#i18n}}workspace.delete{{/i18n}}</a>\
					</div>\
					<table class="striped alternate" summary="">\
						<thead>\
							<tr>\
								<th scope="col">\
									<input type="checkbox" call="allCheckbox" />\
								</th>\
								<th scope="col">{{#i18n}}type{{/i18n}}</th>\
								<th scope="col">{{#i18n}}name{{/i18n}}</th>\
								<th scope="col">{{#i18n}}modified{{/i18n}}</th>\
							</tr>\
						</thead>\
						<tbody>\
							{{#documents}}\
							<tr>\
								<td><input class="select-file" type="checkbox" name="files[]" value="/document/{{_id}}" /></td>\
								<td><i role="{{#metadata}}{{content-type}}{{/metadata}}"></i></td>\
								<td><a href="/document/{{_id}}">{{name}}</a></td>\
								<td>{{modified}}</td>\
							</tr>\
							{{/documents}}\
							{{#rack}}\
							<tr>\
								<td><input class="select-file" type="checkbox" name="files[]" value="/rack/{{_id}}" /></td>\
								<td>{{#metadata}}{{content-type}}{{/metadata}}</td>\
								<td><a href="/rack/{{_id}}">{{name}}</a></td>\
								<td>{{#formatDate}}{{modified}}{{/formatDate}}</td>\
							</tr>\
							{{/rack}}\
						</tbody>\
					</table>',

			addDocument : '<form id="upload-form" method="post" action="/document" enctype="multipart/form-data">\
							<label>{{#i18n}}workspace.document.name{{/i18n}}</label>\
							<input type="text" name="name" />\
							<label>{{#i18n}}workspace.document.file{{/i18n}}</label>\
							<input type="file" name="file" />\
							<input call="sendFile" type="button" value="{{#i18n}}upload{{/i18n}}" />\
							</form>',

			sendRack : '<form id="upload-form" method="post" action="/rack" enctype="multipart/form-data">\
						<label>{{#i18n}}workspace.rack.name{{/i18n}}</label>\
						<input type="text" name="name" />\
						<label>{{#i18n}}workspace.rack.to{{/i18n}}</label>\
						<input type="text" name="to" />\
						<label>{{#i18n}}workspace.rack.file{{/i18n}}</label>\
						<input type="file" name="file" />\
						<input call="sendFile" type="button" value="{{#i18n}}upload{{/i18n}}" />\
						</form>',

			comment : '<form method="post" action="/document/{{id}}/comment">\
							<label>{{#i18n}}workspace.leave.comment{{/i18n}}</label>\
							<textarea name="comment"></textarea>\
							<input call="sendComment" type="button" value="{{#i18n}}send{{/i18n}}" />\
						</form>',

			moveOrCopyDocuments : '<form action="{{action}}">\
								<label>{{#i18n}}workspace.move.path{{/i18n}}</label>\
								<input type="text" name="folder" />\
								<input call="moveOrCopyDocuments" type="button" value="{{#i18n}}workspace.valid{{/i18n}}" />\
							</form>'
		},
		action : {
			documents : function (o) {
				var relativePath = undefined,
					that = this,
					directories;
				$.get(o.url).done(function(response){
					if (o.url.match(/^\/documents\/.*?/g)) {
						relativePath = o.url.substring(o.url.indexOf("/", 10) + 1, o.url.lastIndexOf("?"));
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
						messenger.requireResize();
					});
				});
			},
			iconsView: function(o){
				$('.list').removeClass('list-view').addClass('icons-view');
			},
			listView: function(o){
				$('.list').removeClass('icons-view').addClass('list-view');
			},
			rack : function (o) {
				$.get(o.url).done(function(response){
					response = tools.formatResponse(response);
					$('#list').html(app.template.render("rack", response));
					messenger.requireResize();
				});
			},
			trash : function (o) {
				$.get("/documents/Trash").done(function(documents) {
					$.get("/rack/documents/Trash").done(function(rack) {
						$('#list').html(app.template.render("trash",
								{ documents : documents, rack : rack }));
						messenger.requireResize();
					});
				});
			},

			addDocument : function (o) {
				$('#form-window').html(app.template.render("addDocument", {}));
				messenger.requireResize();
			},

			sendRack : function(o){
				$('#form-window').html(app.template.render("sendRack", {}));
				messenger.requireResize();
			},

			sendFile : function(o) {
				var form = $('#upload-form'),
					fd = new FormData(),
					action = form.attr('action');
				fd.append('file', form.children('input[type=file]')[0].files[0]);
				if ("/rack" === action) {
					action += '/' + form.children('input[name=to]').val();
				}
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
				var url = "/folders?";
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

			allCheckbox : function(o) {
				var selected = o.target.checked;
				$(":checkbox").each(function() {
					this.checked = selected;
				});

				//o.target.checked = !o.target.checked;
			},

			moveTrash : function(o) {
				var files = [];
				$(":checkbox:checked").each(function(i) {
					var obj = $(this);
					$.ajax({
						url : o.url + "/" + obj.val(),
						type: "PUT",
						success: function() {
							obj.parents("tr").remove();
						},
						error: function(data) {
							app.notify.error(data);
						}
					});
				});
			},

			comment : function(o) {
				$('#form-window').html(app.template.render("comment", { id : o.url }));
				messenger.requireResize();
			},

			sendComment : function(o) {
				var form = $(o.target).parents("form"),
					data = encodeURI(form.serialize()).replace(/(%0D%0A|%250D%250A)/gi, "<br />");
				console.log(data);
				$.post(form.attr("action"), data)
				.done(function (data) {
					location.reload(true);
				})
				.error(function (data) {
					app.notify.error(data);
				});
			},

			showComment : function(o) {
				$(o.target).parent().find('ul, h2').show();
				messenger.requestResize();
			},
			hideComment: function(o){
				$(o.target).parent().parent().find('ul, h2').hide();
				messenger.requestResize();
			},
			remove : function (o) {
				var files = [];
				$(":checkbox:checked").each(function(i) {
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

			moveOrCopy : function(o) {
				$('#form-window').html(app.template.render("moveOrCopyDocuments", { action : o.url}));
				messenger.requireResize();
			},

			moveOrCopyDocuments : function(o) {
				var ids = "",
					form = $(o.target).parents("form"),
					action = form.attr("action"),
					folder = form.children("input[name=folder]").val(),
					method;

				if (action.match(/copy$/g)) {
					method = "POST";
				} else {
					method = "PUT";
				}

				$(":checkbox:checked").each(function(i) {
					ids += "," + $(this).val();
				});
				if (ids != "") {
					ids = ids.substring(1);
				}

				$.ajax({
					url : action + "/" + ids + "/" + folder,
					type: method,
					success: function() {
						location.reload(true);
					},
					error: function(data) {
						app.notify.error(data);
					}
				});
			}

		}
	});
	return app;
}();

$(document).ready(function(){
	workspace.init();
	workspace.action.documents({url : "/documents?hierarchical=true"});
	workspace.action.getFolders(true, undefined, function(data) {
		var html = "";
		for (var i = 0; i < data.length; i++) {
			if (data[i] === "Trash") continue;
			html += '<li><a call="documents" href="/documents/' + data[i] + '">' + data[i] + "</a></li>";
		}
		$(".base-folders").html(html);
});
});

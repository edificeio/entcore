var workspace = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : {
			documents : '<table class="striped alternate" summary="">\
							<thead>\
								<tr>\
									<th scope="col">{{#i18n}}type{{/i18n}}</th>\
									<th scope="col">{{#i18n}}name{{/i18n}}</th>\
									<th scope="col">{{#i18n}}modified{{/i18n}}</th>\
								</tr>\
							</thead>\
							<tbody>\
								{{#.}}\
								<tr>\
									<td>{{#metadata}}{{content-type}}{{/metadata}}</td>\
									<td><a href="/document/{{_id}}">{{name}}</a></td>\
									<td>{{modified}}</td>\
								</tr>\
								{{/.}}\
							</tbody>\
						</table>',

			rack : '<table class="striped alternate" summary="">\
						<thead>\
							<tr>\
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
								<td>{{#metadata}}{{content-type}}{{/metadata}}</td>\
								<td><a href="/rack/{{_id}}">{{name}}</a></td>\
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
								<th scope="col">{{#i18n}}type{{/i18n}}</th>\
								<th scope="col">{{#i18n}}name{{/i18n}}</th>\
								<th scope="col">{{#i18n}}modified{{/i18n}}</th>\
							</tr>\
						</thead>\
						<tbody>\
							{{#.}}\
							<tr>\
								<td>{{#metadata}}{{content-type}}{{/metadata}}</td>\
								<td>{{name}}</td>\
								<td>{{modified}}</td>\
							</tr>\
							{{/.}}\
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

		},
		action : {
			documents : function (o) {
				$.get(o.url).done(function(response){
					$('#list').html(app.template.render("documents", response));
				});
			},

			rack : function (o) {
				$.get(o.url).done(function(response){
					$('#list').html(app.template.render("rack", response));
				});
			},

			trash : function (o) {
				$.get(o.url).done(function(response){
					$('#list').html(app.template.render("trash", response));
				});
			},

			addDocument : function (o) {
				$('#form-window').html(app.template.render("addDocument", {}));
			},

			sendRack : function(o){
				$('#form-window').html(app.template.render("sendRack", {}));
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
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	workspace.init();
});

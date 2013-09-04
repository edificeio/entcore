var admin = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };
	var personDataAdaptor = function (d) {
		var activated = [];
		var nonActivated = [];
		for (obj in d.result){
			if (d.result[obj].code === ''){
				activated.push(d.result[obj]);
			} else {
				nonActivated.push(d.result[obj]);
			}
		}
		return {activated :activated, nonActivated: nonActivated}; 
	};

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			ecole : "\
				{{#list}}<h3>{{name}}</h3>\
				<a call='classes' href='api/classes?id={{id}}'>\
				{{#i18n}}directory.admin.see-classes{{/i18n}}</a> - \
				<a href='api/export?id={{id}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a>\
				<div id='classes-{{id}}'></div>{{/list}}"
			,
			groupes : "\
				{{#list}}<h3>{{name}}</h3>\
				<a call='membres' href='api/membres?data={{people}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>{{/list}}"
			,
			classes: "\
				{{#list}}<h4><a>{{name}}</a></h4>\
				<a call='personnes' href='api/personnes?id={{classId}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>\
				 - <a href='api/enseignants?id={{classId}}' call='enseignants'>\
				{{#i18n}}directory.admin.add-teacher{{/i18n}}</a>\
				 - <a href='api/export?id={{classId}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a><br />\
				<div id='people-{{classId}}'></div>{{/list}}"
			,
			personnes: "\
				<br /><span>{{#activated}}<a call='personne' href='api/details?id={{userId}}'>\
				{{lastName}} {{firstName}}</a> - {{/activated}}{{#nonActivated}}\
				<a call='personne' href='api/details?id={{userId}}' style='background-color:yellow;'>\
				{{lastName}} {{firstName}}</a> - {{/nonActivated}}</span><div id='details'></div>"
			,
			enseignants : "\
				<br /><span>{{#list}}\
				<a call='personne' href='api/link?class={{classId}}&id={{userId}}'>\
				{{lastName}} {{firstName}}</a> - {{/list}}</span>"
			,
			membres : "\
				<span>{{#list}}{{lastName}} {{firstName}} - {{/list}}</span>"
			,
			personne : '\
				{{#list}}{{#i18n}}directory.admin.login{{/i18n}} : {{login}} / {{code}} - \
				{{#i18n}}directory.admin.address{{/i18n}} : {{address}}{{/list}}'
			,
			exportAuth : 'Nom,Prénom,Login,Mot de passe\n'
				+ '{{#list}}{{lastName}},{{firstName}},{{login}},{{activationCode}}\n'
				+ '{{/list}}'
			,
			personnesEcole :'\
				{{#list}}<input type="checkbox" name="{{userId}}" value="{{userId}}"/>\
				{{lastName}} {{firstName}} - {{/list}}'
		},
		action : {
			ecole : function(o) {
				if (!!$('#schools').children().length) {
					$('#schools').html('');
					return;
				}
				app.template.getAndRender(o.url, 'ecole','#schools', dataExtractor);
			},
			classes : function(o) {
				$.get(o.url)
				.done(function(data){
					if(undefined !== data.result[0]){
						if (!!$("#classes-" + data.result[0]["schoolId"]).children().length) {
							$("#classes-" + data.result[0]["schoolId"]).html('');
							return;
						}
						$("#classes-" + data.result[0]["schoolId"]).html(app.template.render('classes', dataExtractor(data)));
					} else {
						app.notify.info("no resulst");
					}
				})
				.error(function(data){app.notify.error(data);})
			},
			groupes : function(o) {
				if (!!$('#groups').children().length) {
					$('#groups').html('');
					return;
				}
				app.template.getAndRender(o.url, 'groupes', '#groups', dataExtractor);
			},
			personnesEcole : function(o) {
				app.template.getAndRender(o.url, 'personnesEcole', '#users', dataExtractor);
			},
			personnes : function(o) {
				$.get(o.url)
				.done(function(data){
					if (!!$('#people-' + data.result[0]["classId"]).children().length) {
						$('#people-' + data.result[0]["classId"]).html('');
						return;
					}
					$("#people-" + data.result[0]["classId"]).html(app.template.render('personnes', personDataAdaptor(data)));
				})
				.error(function(data){app.notify.error(data)})
			},
			membres : function(o) {
				if (!!$('#members').children('form').length) {
					$('#members').html('');
					return;
				}
				app.template.getAndRender(o.url, 'membres', '#members', dataExtractor);
			},
			personne : function(o) {
				if (!!$('#details').children('form').length) {
					$('#details').html('');
					return;
				}
				app.template.getAndRender(o.url, 'personne', '#details', dataExtractor);
			},
			enseignants : function(o) {
				$.get(o.url)
				.done(function(data){
					if (!!$('#people-' + data.result[0]["classId"]).children().length) {
						$('#people-' + data.result[0]["classId"]).html('');
						return;
					}
					$("#people-" + data.result[0]["schoolId"]).html(app.template.render('enseignants', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data)})
			},
			exportAuth : function(o) {
				$.get(o.url)
				.done(function(data){
					document.location = 'data:Application/octet-stream,'
					+ encodeURIComponent(app.template.render('exportAuth', dataExtractor(data)));
					app.notify.info("{{#i18n}}directory.admin.ok{{/i18n}}");
				})
				.error(function(data){app.notify.error(data)})
			},
			createUser : function(o) {
				var url = o.target.form.action + '?' + $('#create-user').serialize()
					+ '&ENTPersonProfils=' + $('#profile').val()
					+ '&ENTPersonStructRattach=' + $('#groupe').val();
				$.get(url)
				.done(function(data){
					if (data.result === "error"){
						$('label').removeAttr('style');
						for (obj in data){
							if (obj !== "result"){
								$('#' + obj).attr("style", "color:red");
							}
						}
						app.notify.error("{{#i18n}}directory.admin.form-error{{/i18n}}");
					} else {
						app.notify.info("{{#i18n}}directory.admin.ok{{/i18n}}");
						$('label').removeAttr('style');
					}
				})
				.error(function(data){app.notify.error(data.status);})
			},
			createAdmin : function(o) {
				var url = o.target.form.action + '?' + $('#create-admin').serialize()
					+ '&ENTPerson=' + $('#choice').val();
				$.get(url)
				.done(function(data){app.notify.done(data.status);})
				.error(function(data){app.notify.error(data.status);})
			},
			createGroup : function(o) {
				var url = o.target.form.action + '?' + $('#create-group').serialize()
					+ '&type=' + $('#type').val() + '&ENTGroupStructRattach=' + $('#parent').val();
				$.get(url)
				.done(function(data){app.notify.done(data.status);})
				.error(function(data){app.notify.error(data.status);})
			},
			createSchool : function(o) {
				var url = o.target.form.action + '?' + $('#create-school').serialize();
				$.get(url)
				.done(function(data){app.notify.done(data.status);})
				.error(function(data){app.notify.error(data.status);})
			},
			view: function(o) {
				switch(o.target.id){
					case 'disp':
						$('#creation').attr('hidden', '');
						$('#display').removeAttr('hidden');
						$('#export').attr('hidden', '');
						break;
					case 'exports':
						$('#creation').attr('hidden', '');
						$('#display').attr('hidden');
						$('#export').removeAttr('hidden');
						break;
					case 'create':
						$('#creation').removeAttr('hidden');
						$('#display').attr('hidden', '');
						$('#export').attr('hidden', '');
						break;
				}
			},
			testbe1d : function(o) {
				$.get(o.url)
				.done(function(data){app.notify.info(data);})
				.error(function(data){app.notify.error(data);});
			},
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init(); 
});

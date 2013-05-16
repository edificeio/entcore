var admin = function(){

	// TMP
	dataExtractor = function (d) { return {list : _.values(d.result)}; };

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			ecole : "\
				{{#list}}\
				<h3>{{name}}</h3>\
				<a call='classes' href='/api/classes?id={{id}}'>\
				{{#i18n}}directory.admin.see-classes{{/i18n}}</a> - \
				<a href='/api/export?id={{id}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a>\
				<div id='classes-{{id}}'></div>\
				{{/list}}"
			,
			groupes : "\
				<h3>{{nENTGroupeNom}}</h3>\
				<a call='membres' href='/api/membres?data=\
				{{nENTPeople}}'>{{#i18n}}directory.admin.see-people{{/i18n}}</a>"
			,
			classes: "\
				{{#list}}<h4><a>{{name}}</a></h4>\
				<a call='personnes' href='/api/personnes?id={{classId}}'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>\
				 - <a href='/api/enseignants?id={{classId}}' call='enseignants'>\
				{{#i18n}}directory.admin.see-people{{/i18n}}</a>\
				 - <a href='/api/export?id={{classId}}' call='exportAuth'>\
				{{#i18n}}directory.admin.exports{{/i18n}}</a><br />\
				<div id='people-{{classId}}'></div>{{/list}}"
			,
			personnes : function(data) {
				var htmlString='<br /><span>';
				var jdata = jQuery.parseJSON(data);
				if (jdata.result != ""){
					if (!!$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).children().length) {
						$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html('');
						return;
					}
					for (obj in jdata.result){
						htmlString +="<a call='personne' href='/api/details?id="
							+ jdata.result[obj]['m.id'] +"'>" + jdata.result[obj]['m.ENTPersonNom']
							+ " " +jdata.result[obj]['m.ENTPersonPrenom'] + "</a> - ";
					}
					htmlString += "</span><div id='details'></div>";
					$("#people-" + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html(htmlString);
				}
			},
			enseignants : function(data) {
				var htmlString='<br /><span>';
				var jdata = jQuery.parseJSON(data);
				if (jdata.result[0] !== undefined){
					if (!!$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).children().length) {
						$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html('');
						return;
					}
					for (obj in jdata.result){
						htmlString +="<a call='personne' href='/api/link?"
							+ "class=" + jdata.result[obj]['n.id']
							+ "&id=" + jdata.result[obj]['m.id'] +"'>" + jdata.result[obj]['m.ENTPersonNom']
							+ " " +jdata.result[obj]['m.ENTPersonPrenom'] + "</a> - ";
					}
					htmlString += "</span><div id='details'></div>";
					$("#people-" + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html(htmlString);
				}
			},
			membres : "\
				<span>{{#list}}{{lastName}} {{firstName}} - {{/list}}</span>"
			,
			personne : '\
				{{#i18n}}directory.admin.lastname{{/i18n}}{{lastName}} - \
				{{#i18n}}directory.admin.firstname{{/i18n}}{{firstName}} - \
				{{#i18n}}directory.admin.address{{/i18n}}{{address}}'
			,
			exportAuth : '\
				Nom,Prénom,Login,Mot de passe\n{{#list}}\
				{{lastName}},{{firstName}},{{login}},{{password}}\n{{/list}}'
			,
			personnesEcole :'\
				{{#list}}<input type="checkbox" name="{{userId}}" value="{{userId}}"/>\
				{{lastName}} {{firstName}} - {{/list}}'
			,
			createUser : function(data) {
				if (data.result === "error"){
					console.log(data);
					$('label').removeAttr('style');
					for (obj in data){
						if (obj !== "result"){
							$('#' + obj).attr("style", "color:red");
						}
					}
					$('#confirm').html("<span style='color:red'>ERROR !</span>");
				} else {
					$('#confirm').html("OK");
					$('label').removeAttr('style');

				}
			},
			createAdmin : function(data) {
				console.log(data);
				var jdata = jQuery.parseJSON(data);
				if (jdata.status === 'ok'){
					$('#confirm').html("OK");
				} else {
					$('#confirm').html("ERREUR !");
				}
			}
		},
		action : {
			ecole : function(o) {
				if (!!$('#schools').children().length) {
					$('#schools').html('');
					return;
				}
				app.template.getAndRender(o.url, 'ecole','#schools');
			},
			classes : function(o) {
				$.get(o.url)
				.done(function(data){
					var jo = jQuery.parseJSON(data);
					if (!!$("#classes-" + jo.result[0]["schoolId"]).children().length) {
						$("#classes-" + jo.result[0]["schoolId"]).html('');
						return;
					}
					$("#classes-" + jo.result[0]["schoolId"]).html(app.template.render('classes', dataExtractor(jo)));
				})
				.error(function(data){
					app.notify.error(data);
				})
			},
			groupes : function(o) {
				if (!!$('#groups').children().length) {
					$('#groups').html('');
					return;
				}
				app.template.getAndRender(o.url, 'groupes', '#groups');
			},
			personnesEcole : function(o) {
				app.template.getAndRender(o.url, 'personnesEcole', '#users');
			},
			personnes : function(o) {
				getAndRender(o.url, "personnes");
			},
			membres : function(o) {
				if (!!$('#members').children('form').length) {
					$('#members').html('');
					return;
				}
				app.template.getAndRender(o.url, 'membres', '#members');
			},
			personne : function(o) {
				if (!!$('#details').children('form').length) {
					$('#details').html('');
					return;
				}
				app.template.getAndRender(o.url, 'personne', '#details');
			},
			enseignants : function(o) {
				getAndRender(o.url, "enseignants");
			},
			exportAuth : function(o) {
				document.location = 'data:Application/octet-stream,'
					+ encodeURIComponent(app.template.getAndRender(o.url, 'exportAuth'));
			},
			createUser : function(o) {
				var url = o.target.form.action + '?'
					+ $('#create-user').serialize()
					+ '&ENTPersonProfils=' + $('#profile').val()
					+ '&ENTPersonStructRattach=' + $('#groupe').val().replace(/ /g,'-');
				getAndRender(url, "createUser");
			},
			createAdmin : function(o) {
				var url = o.target.form.action + '?'
					+ $('#create-admin').serialize()
					+ '&ENTPerson=' + $('#choice').val();
				getAndRender(url, "createAdmin");
			},
			createGroup : function(o) {
				var url = o.target.form.action + '?'
					+ $('#create-group').serialize()
					+ '&type=' + $('#type').val()
					+ '&ENTGroupStructRattach=' + $('#parent').val();
				$.get(url)
				.done(function(data){
					app.notify.done(data.status);
				})
				.error(function(data){
					app.notify.error(data.status);
				})
			},
			createSchool : function(o) {
				var url = o.target.form.action + '?'
					+ $('#create-school').serialize();
				$.get(url)
				.done(function(data){
					app.notify.done(data.status);
				})
				.error(function(data){
					app.notify.error(data.status);
				})
			},
			view: function(o) {
				app.notify.done("switch");
				switch(o.target.id){
					case 'disp':
						$('#creation').attr('hidden', '');
						$('#display').removeAttr('hidden');
						$('#export').attr('hidden', '');
						break;
					case 'exports':
						$('#creation').attr('hidden', '');
						$('#display').removeAttr('hidden');
						$('#export').removeAttr('hidden');
						break;
					case 'create':
						$('#creation').removeAttr('hidden');
						$('#display').attr('hidden', '');
						$('#export').attr('hidden', '');
						break;
				}
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init(); 
});
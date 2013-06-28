var admin = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			personnes: '\
				{{#list}}<div id="person-small">\
				<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span><a href="/api?name={{displayName}}" call="searchPerson">{{lastName}} {{firstName}}</a></span>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<span class="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				<img src="/public/img/carnet.png" alt="carnet"/>\
				<img src="/public/img/files.png" alt="files"/>\
				</span></div>{{/list}}',
			personne: '\
				{{#list}}<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span class="name">{{displayName}}</span>\
				<span class="address">{{address}}</span>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<div class="clear"></div>\
				<span id="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				{{#i18n}}userBook.class.write-message{{/i18n}}<div class="clear"></div>\
				<img src="/public/img/carnet.png" alt="carnet"/>{{#i18n}}userBook.class.edit-notebook{{/i18n}}\
				<div class="clear"></div><img src="/public/img/files.png" alt="files"/>\
				{{#i18n}}userBook.class.see-portfolio{{/i18n}}\
				</span><div>{{#i18n}}userBook.class.mood{{/i18n}} : {{mood}} \
				{{#i18n}}userBook.class.health{{/i18n}} : {{health}}</div>{{/list}}'
		},
		action : {
			searchPerson : function(o){
				var url = o.target.form.action + '?' + $('#search-form').serialize();
				$.get(url)
				.done(function(data){
					console.log(data.result);
					$("#people").addClass('single').removeClass('all');
					$("#person").html(app.template.render('personne', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data.status);})
			},
			searchClass : function(o) {
				$.get(o)
				.done(function(data){
					$("#people").addClass('all').removeClass('single');
					$('#person').html('');
					$("#people").html(app.template.render('personnes', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data)})
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init();
	admin.action.searchClass("/api?class=4400000002$ORDINAIRE$CM2%20de%20Mme%20Rousseau");
});
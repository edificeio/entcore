var account = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };

	var app = Object.create(oneApp);
	app.scope = "#userBook";
	app.define ({
		template : {
			personne: '\
				{{#list}}<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span class="name">{{displayName}}</span>\
				<span class="address">{{address}}</span>\
				<span class="motto">{{motto}}</span>\
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
			profile : function(url) {
				$.get(url)
				.done(function(data){
					$('#person').html(app.template.render('personne', dataExtractor(data)));
				})
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	account.init();
	account.action.profile("/api/person?id=Vlnny020130624161244366")
});
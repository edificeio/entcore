var admin = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			personnes: '\
				{{#list}}<div id="person-small">\
				<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span><a href="/person?id={{userId}}" call="personne">{{lastName}} {{firstName}}</a></span>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<span class="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				<img src="/public/img/carnet.png" alt="carnet"/>\
				<img src="/public/img/files.png" alt="files"/>\
				</span></div>{{/list}}',
			personne: '\
				{{#list}}<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span class="name">{{lastName}} {{firstName}}</span>\
				<span class="address">{{address}}</span>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<div class="clear"></div>\
				<span id="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				{{#i18n}}userBook.class.write-message{{/i18n}}<div class="clear"></div>\
				<img src="/public/img/carnet.png" alt="carnet"/>{{#i18n}}userBook.class.edit-notebook{{/i18n}}\
				<div class="clear"></div><img src="/public/img/files.png" alt="files"/>\
				{{#i18n}}userBook.class.see-portfolio{{/i18n}}\
				</span>{{/list}}'
		},
		action : {
			personnes : function(o) {
				$.get(o.url)
				.done(function(data){
					$("#people").addClass('all').removeClass('single');
					$('#person').html('');
					$("#people").html(app.template.render('personnes', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data)})
			},
			personne : function(o){
				$.get(o.url)
				.done(function(data){
					$("#people").addClass('single').removeClass('all');
					$("#person").html(app.template.render('personne', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data)})
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init();
});
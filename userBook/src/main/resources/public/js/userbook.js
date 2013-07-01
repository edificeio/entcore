var userbook = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			searchResults: '\
				{{#list}}<div id="person-small">\
				<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span><a href="/api/person?id={{id}}" call="person">{{displayName}}</a></span>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<span class="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				<img src="/public/img/carnet.png" alt="carnet"/>\
				<img src="/public/img/files.png" alt="files"/>\
				</span></div>{{/list}}',
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
			search : function(o){
				var url = o.target.form.action + '?' + $('#search-form').serialize();
				$.get(url)
				.done(function(data){
					$("#people").addClass('all').removeClass('single');
					$("#person").html('');
					$("#people").html(app.template.render('searchResults', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data.status);})
			},
			person : function(o){
				$.get(o.url)
				.done(function(data){
					$("#people").addClass('single').removeClass('all');
					$("#person").html(app.template.render('personne', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data.status);})
			},
			searchClass : function(url) {
				$.get(url)
				.done(function(data){
					$("#people").addClass('all').removeClass('single');
					$('#person').html('');
					$("#people").html(app.template.render('searchResults', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data)})
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	userbook.init();
	userbook.action.searchClass("/api/class?name=CM2%20de%20Mme%20Rousseau");
});
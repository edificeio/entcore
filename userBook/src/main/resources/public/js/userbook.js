var userbook = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };
	var personDataExtractor = function(d) {
		var jo = {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"]};
		var hobbies = [];
		for (obj in d.result){
			if (d.result[obj].category !== ""){
				hobbies.push({"category":d.result[obj].category,"values":d.result[obj].values});
			}
			if (d.result[obj].mood !== ""){
				jo['mood'] = d.result[obj].mood;
				jo['health'] = d.result[obj].health;
				jo['motto'] = d.result[obj].motto;
			}
		}
		jo['list'] = hobbies;
		return jo;
	};

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			searchResults: '\
				{{#list}}<div class="person-small" id={{id}}>\
				<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span><a href="/api/person?id={{id}}" call="person">{{displayName}}</a></span>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<span class="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				<img src="/public/img/carnet.png" alt="carnet"/>\
				<img src="/public/img/files.png" alt="files"/>\
				</span></div>{{/list}}',
			personne: '\
				<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<p class="name">{{displayName}}</p>\
				<p class="address">{{address}}</p>\
				<p class="motto">{{#i18n}}userBook.profile.motto{{/i18n}} : {{motto}}</p>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<div class="clear"></div>\
				<span id="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				{{#i18n}}userBook.class.write-message{{/i18n}}\
				<img src="/public/img/carnet.png" alt="carnet"/>{{#i18n}}userBook.class.edit-notebook{{/i18n}}\
				<img src="/public/img/files.png" alt="files"/>\
				{{#i18n}}userBook.class.see-portfolio{{/i18n}}</span>\
				<h3>{{#i18n}}userBook.profile.health{{/i18n}}</h3><p>{{health}}</p>\
				<h2>{{#i18n}}userBook.interests{{/i18n}}</h2>\
				{{#list}}<h3>{{category}}</h3><p><span class="{{category}}">\
				{{values}}</span></p>{{/list}}'
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
					$("div.person-small").removeClass('highlight');
					$('#' + data.result[0].id).addClass('highlight');
					$("#person").html(app.template.render('personne', personDataExtractor(data)));
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
	var className = location.search.split('class=')[1];
	userbook.action.searchClass("/api/class?name=" + className);
});
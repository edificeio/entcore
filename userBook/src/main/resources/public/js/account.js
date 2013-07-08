var account = function(){

	var personDataExtractor = function(d) {
		return {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"],
			"health":d.result[0]["health"],"mood":d.result[0]["mood"],
			"motto":d.result[0]["motto"],list:_.values(d.result) };
	};

	var app = Object.create(oneApp);
	app.scope = "#userBook";
	app.define ({
		template : {
			personne: '\
				<img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<p class="name">{{displayName}}</p>\
				<p class="address">{{address}}</p>\
				<p class="motto">{{#i18n}}userBook.profile.motto{{/i18n}} : {{motto}}</p>\
				<img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				<div class="clear"></div>\
				<span id="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				{{#i18n}}userBook.class.write-message{{/i18n}}<div class="clear"></div>\
				<img src="/public/img/carnet.png" alt="carnet"/>{{#i18n}}userBook.class.edit-notebook{{/i18n}}\
				<div class="clear"></div><img src="/public/img/files.png" alt="files"/>\
				{{#i18n}}userBook.class.see-portfolio{{/i18n}}\
				<h3>{{#i18n}}userBook.profile.health{{/i18n}}</h3><p>{{health}}</p></div>\
				<h2>{{#i18n}}userBook.interests{{/i18n}}</h2>\
				{{#list}}<h3>{{category}}</h3><p>{{#values}}<span id="{{category}}" contenteditable="true">\
				{{value}}</span>{{/values}}<span>more</span></p>{{/list}}'
		},
		action : {
			profile : function(url) {
				$.get(url)
				.done(function(data){
					$('#person').html(app.template.render('personne', personDataExtractor(data)));
					manageEditable();
				})
			},
			editHobbies : function(url){
				$.get(url)
				.done(function(data){
					console.log(data);
					app.notify.info("modif ok");
				})
			}
		}
	});
	return app;
}();

function manageEditable(){
	console.log("hello");
	$('#places').focus(function(){document.designMode = 'on';});
	$('#places').blur(function(){document.designMode = 'off';console.log(this.innerHTML);});
}

$(document).ready(function(){
	account.init();
	account.action.profile("/api/person?id=Vxxrg020130624161244358")
});
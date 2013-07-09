var userId = location.search.split('id=')[1];

var account = function(){

	var personDataExtractor = function(d) {
		var hobbies = [];
		if (d.result[0].values !== ""){
			for (obj in d.result){
				var vals = [];
				for (val in obj.values.split("_")){
					vals.push({"value":obj.values.split("_")[val]});
				}
				hobbies.push({"category":obj["category"],values:vals});
			}
		}
		var jo = {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"],
			"health":d.result[0]["health"],"mood":d.result[0]["mood"],
			"motto":d.result[0]["motto"],list:hobbies };
		return jo;
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
				{{#list}}<h3>{{category}}</h3><p>{{#values}}<span class="{{category}}" contenteditable="true">\
				{{value}}</span>{{/values}}<span>more</span></p>\
				<form method="GET" action="/api/set-visibility" id="visible">\
				<select><option>public</option><option>private</option>\
				<input type="submit" value="ok" call="setVisibility"/></form>{{/list}}'
		},
		action : {
			profile : function(url) {
				$.get(url)
				.done(function(data){
					console.log(data);
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
			},
			setVisibility : function(o){
				$.get(o.url)
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
	$('span[contenteditable="true"]').focus(function(){document.designMode = 'on';});
	$('span[contenteditable="true"]').blur(function(){
		document.designMode = 'off';
		var values = "";
		for (val in this.parentNode.children){
			values += val.innerHTML + "_";

		}
		account.action.editHobbies("/api/edit-hobbies?id=Vaojs020130709130703897"
			+ "&category=" + this.classList[0] + "&values=" + values);
	});
}

$(document).ready(function(){
	account.init();
	account.action.profile("/api/person?id=Vnzwx020130709163939432");
});
var userId = location.search.split('id=')[1];

var account = function(){

	var personDataExtractor = function(d) {
		var jo = {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"]};
		var hobbies = [];
		for (obj in d.result){
			if (d.result[obj].category !== ""){
				var vals = [];
				for (val in d.result[obj].values.split("_")){
					vals.push({"value":d.result[obj].values.split("_")[val]});
				}
				hobbies.push({
					"category":d.result[obj].category,
					values:vals, 
					"visibility":d.result[obj].relation.split(",")[1].slice(0,-1).trim()
				});
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
	app.scope = "#person";
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
				{{value}}</span>{{/values}}</p>\
				<form method="GET" action="/api/set-visibility?&category={{category}}" id="visibility-form">\
				<select id="visible"><option>PUBLIC</option><option>PRIVE</option>\
				<input type="submit" value="ok" call="setVisibility"/></form>\
				CURRENT : <span id="current-visibility">{{visibility}}</span>{{/list}}'
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
			},
			setVisibility : function(o){
				var url = o.target.form.action + '&value=' + $('#visible').val()
					+ '&id=Vjsrc020130710175022472';
				$('#current-visibility').html = $('#visible').val();
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
	$('span[contenteditable="true"]').focus(function(){document.designMode = 'on';});
	$('span[contenteditable="true"]').blur(function(){
		document.designMode = 'off';
		var values = "";
		var siblings = this.parentNode.childNodes;
		for (val in siblings){
			if (typeof(siblings[val]) === "object"){
				values += siblings[val].innerHTML + "_";
			}
		}
		account.action.editHobbies("/api/edit-hobbies?id=Vaojs020130709130703897"
			+ "&category=" + this.classList[0] + "&values=" + values);
	});
}

$(document).ready(function(){
	account.init();
	account.action.profile("/api/person?id=Vnzwx020130709163939432");
});
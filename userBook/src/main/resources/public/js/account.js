var userId = location.search.split('id=')[1];

var account = function(){

	var personDataExtractor = function(d) {
		var jo = {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"]};
		var hobbies = [];
		for (obj in d.result){
			if (d.result[obj].category !== ""){
				hobbies.push({
					"category":d.result[obj].category,
					"values":d.result[obj].values,
					"visibility":d.result[obj].relation[1]
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
				<p class="motto">{{#i18n}}userBook.profile.motto{{/i18n}} :\
				<span contenteditable="true"> {{motto}}</span></p>\
				<select class="mood">\
				<option value="volvo" style="background-image:url(/public/img/reveur.png);">Rêveur</option></select>\
				<div class="clear"></div>\
				<span id="actions"><img src="/public/img/mailto.png" alt="mailto"/>\
				{{#i18n}}userBook.class.write-message{{/i18n}}<div class="clear"></div>\
				<img src="/public/img/carnet.png" alt="carnet"/>{{#i18n}}userBook.class.edit-notebook{{/i18n}}\
				<div class="clear"></div><img src="/public/img/files.png" alt="files"/>\
				{{#i18n}}userBook.class.see-portfolio{{/i18n}}\
				<h3>{{#i18n}}userBook.profile.health{{/i18n}}</h3><p>\
				<span contenteditable="true"> {{health}}</span></p></div>\
				<h2>{{#i18n}}userBook.interests{{/i18n}}</h2>\
				{{#list}}<h3>{{category}}</h3><p id="category"><span class="{{category}}" contenteditable="true">\
				{{values}}</span></p>\
				<form method="GET" action="/api/set-visibility?&category={{category}}" id="visibility-form">\
				<select id="visible"><option>PUBLIC</option><option selected>PRIVE</option>\
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
			editUserBookInfo : function(url){
				$.get(url)
				.done(function(data){
					console.log(data);
					app.notify.info("modif ok");
				})
			},
			setVisibility : function(o){
				var url = o.target.form.action + '&value=' + $('#visible').val()
					+ '&id=' + userId;
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
	$('span[contenteditable="true"]').onfocus(function(){document.designMode = 'on';});
	$('span[contenteditable="true"]').onblur(function(){
		document.designMode = 'off';
		var parameters = "?id=" + userId;
		if (this.parentNode.id === "category"){
			parameters += "&category=" + this.classList[0] + "&values=" + this.innerHTML;
		} else {
			parameters += "&prop=" + this.parentNode.classList[0] + "&value=" + this.innerHTML;
		}
		account.action.editUserBookInfo("/api/edit-userbook-info" + parameters);
	});
}

$(document).ready(function(){
	account.init();
	account.action.profile("/api/person?id=" + userId);
});
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
				<form id="upload-form" method="post" action="/document" enctype="multipart/form-data">\n\
				<label>Changer l\'image</label><input type="file" name="file" value="Changer l\'image"/>\
				<input call="sendPhoto" type="button" value="ok" /></form>\
				<p class="name">{{displayName}}</p>\
				<p class="address">{{address}}</p>\
				<p class="motto">{{#i18n}}userBook.profile.motto{{/i18n}} :\
				<span contenteditable="true"> {{motto}}</span></p>\
				<form id="mood" action="/api/edit-userbook-info">\
				<input type="radio" name="mood" checked value="default" /> Default\
				<input type="radio" name="mood" value="love" /><img src="/public/img/amoureux.jpg" alt="amoureux"/> Amoureux\
				<input type="radio" name="mood" value="angry" /><img src="/public/img/colere.jpg" alt="colere"/> En colère\
				<input type="radio" name="mood" value="happy" /><img src="/public/img/content.jpg" alt="content"/> Content\
				<input type="radio" name="mood" value="worried" /><img src="/public/img/embete.jpg" alt="embete"/> Embêté\
				<input type="radio" name="mood" value="joker" /><img src="/public/img/farceur.jpg" alt="farceur"/> Farceur\
				<input type="radio" name="mood" value="tired" /><img src="/public/img/fatigue.jpg" alt="fatigue"/> Fatigué\
				<input type="radio" name="mood" value="proud" /><img src="/public/img/fier.jpg" alt="fier"/> Fier\
				<input type="radio" name="mood" value="sick" /><img src="/public/img/malade.jpg" alt="malade"/> Malade\
				<input type="radio" name="mood" value="dreamy" /><img src="/public/img/reveur.jpg" alt="reveur"/> Rêveur\
				<input type="radio" name="mood" value="sad" /><img src="/public/img/triste.jpg" alt="triste"/> Triste</form>\
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
					account.action.getPhoto(data.result[0].photo);
				})
			},
			editUserBookInfo : function(url){
				$.get(url)
				.done(function(data){
					app.notify.info("modif ok");
				})
			},
			setVisibility : function(o){
				var url = o.target.form.action + '&value=' + $('#visible').val()
					+ '&id=' + userId;
				$('#current-visibility').html = $('#visible').val();
				$.get(url)
				.done(function(data){
					app.notify.info("modif ok");
				})
			},
			sendPhoto : function(elem, files) {
				var form = new FormData();
				form.append("image", $('#upload-form').children('input[type="file"]')[0].files[0]);
				form.append("name","blablabla");
				$.ajax({
					url: "/document",
					type: 'POST',
					data: form,
					cache: false,
					contentType: false,
					processData: false
				}).done(function (data) {
					if (data.status == "ok") {
						account.action.editUserBookInfo("/api/edit-userbook-info?id=" + userId + "&prop=picture&value=" + data._id);
						$('img[class="avatar"]')[0].setAttribute("src", "http://localhost:8011/document/" + data._id);
					}
				}).error(function (data) { console.log(data); });
			},
			getPhoto : function(photoId) {
				$.ajax({
					url: "document/" + photoId,
					type: 'GET'
				}).done(function (data) {
					if (data !== "") {
						$('img[class="avatar"]')[0].setAttribute("src", "http://localhost:8011/document/" + photoId);
					}
				}).error(function (data) { console.log(data); });
			}
		}
	});
	return app;
}();

function manageEditable(){
	$('span[contenteditable="true"]').blur(function(){
		var parameters = "?id=" + userId;
		if (this.parentNode.id === "category"){
			parameters += "&category=" + this.classList[0] + "&values=" + this.innerHTML;
		} else {
			parameters += "&prop=" + this.parentNode.classList[0] + "&value=" + this.innerHTML;
		}
		account.action.editUserBookInfo("/api/edit-userbook-info" + parameters);
	});
	$('input[type="radio"][name="mood"]').click(function(){
		var parameters = "?id=" + userId + "&prop=mood&value=" + this.value;
		account.action.editUserBookInfo("/api/edit-userbook-info" + parameters);
	});
}

$(document).ready(function(){
	account.init();
	account.action.profile("/api/person?id=" + userId);
});
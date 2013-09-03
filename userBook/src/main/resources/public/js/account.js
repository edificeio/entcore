var account = function(){

	var personDataExtractor = function(d) {
		var jo = {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"]};
		var hobbies = [];
		for (obj in d.result){
			if (d.result[obj].category !== ""){
				hobbies.push({
					"category":d.result[obj].category,
					"values":d.result[obj].values
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
				<div class="row box">\
					<div class="avatar cell four">\
						<img src="public/img/no-avatar.jpg" alt="user" class="avatar"/>\
					</div>\
					<article class="cell eight text-container right-magnet">\
						<h2>{{displayName}}</h2>\
						<div class="row">\
							<div class="four cell">{{#i18n}}userBook.profile.address{{/i18n}}</div>\
							<em class="six cell">{{address}}</em>\
						</div>\
						<div class="row">\
							<div class="four cell">{{#i18n}}userBook.profile.motto{{/i18n}}</div>\
							<em class="six cell" contenteditable="true" data-property="motto">{{motto}}</em>\
						</div>\
					</article>\
					<article class="eight cell text-container right-magnet">\
						<h2>Ma photo</h2>\
						<form id="upload-form" method="post" action="/document" enctype="multipart/form-data" class="search">\
							<input type="file" name="file" value="Changer l\'image"/>\
							<input call="sendPhoto" type="button" class="clear" value="ok" />\
						</form>\
					</article>\
				<div class="cell eight enhanced-select right-magnet" data-selected="{{mood}}">\
					<div class="current"><i role="{{mood}}-panda" class="small-panda"></i><span>{{mood}}</span></div>\
					<div class="options-list">\
						<div class="option" data-value="default">\
							<i role="default-panda" class="small-panda"></i>\
							<span>Default</span>\
						</div>\
						<div class="option" data-value="love">\
							<i role="love-panda" class="small-panda"></i>\
							<span>Amoureux</span>\
						</div>\
						<div class="option" data-value="angry">\
							<i role="angry-panda" class="small-panda"></i>\
							<span>En colère</span>\
						</div>\
						<div class="option" data-value="happy">\
							<i role="happy-panda" class="small-panda"></i>\
							<span>Content</span>\
						</div>\
						<div class="option" data-value="worried">\
							<i role="worried-panda" class="small-panda"></i>\
							<span>Embêté</span>\
						</div>\
						<div class="option" data-value="tired">\
							<i role="tired-panda" class="small-panda"></i> \
							<span>Fatigué</span>\
						</div>\
						<div class="option" data-value="proud">\
							<i role="proud-panda" class="small-panda"></i><span>Fier</span>\
						</div>\
						<div class="option" data-value="sick">\
							<i role="sick-panda" class="small-panda"></i>\
							<span>Malade</span>\
						</div>\
						<div class="option" data-value="dreamy">\
							<i role="dreamy-panda" class="small-panda"></i>\
							<span>Rêveur</span>\
						</div>\
						<div class="option" data-value="sad">\
							<i role="sad-panda" class="small-panda"></i>\
							<span>Triste</span>\
						</div>\
						<div class="option" data-value="joker">\
							<i role="joker-panda" class="small-panda"></i>\
							<span>Farceur</span>\
						</div>\
					</div>\
				</div>\
				</form>\
				</div>\
				<div class="clear"></div>\
				<h1>{{#i18n}}userBook.profile.health{{/i18n}}</h3>\
				<article class="text-container">\
					<em contenteditable="true" data-property="health"> {{health}}</em>\
				</article>\
				<h1>{{#i18n}}userBook.interests{{/i18n}}</h1>\
				<article class="text-container">\
				{{#list}}\
					<div class="row line" data-category="{{category}}">\
						<div class="three cell"><span>{{category}}</span></div>\
						<div class="eight cell"><em contenteditable="true">{{values}}</em></div>\
						<div class="one cell"><i role="{{visibility}}" href="api/set-visibility?&category={{category}}" call="changeVisibility"></i></div>\
						<div class="clear"></div>\
					</div>\
				{{/list}}\
				</article>\
				'
		},
		action : {
			profile : function(url) {
				$.get(url)
				.done(function(data){
					$('#person').html(app.template.render('personne', personDataExtractor(data)));
					manageEditable();
					account.action.getPhoto(data.result[0].photo);
					messenger.requireResize();
				})
			},
			editUserBookInfo : function(url){
				$.get(url)
				.done(function(data){
				})
			},
			setVisibility : function(o){
				var url = o.target.form.action + '&value=' + $('#visible').val();
				$('#current-visibility').html = $('#visible').val();
				$.get(url)
				.done(function(data){
					app.notify.info("modif ok");
				})
			},
			changeVisibility: function(o){
				var newRole = 'public';
				if($(o.target).attr('role') === 'public'){
					newRole = 'private';
				}
				var url = o.url + '&value=' + newRole;
				$.get(url)
					.done(function(data){
						$(o.target).attr('role', newRole);
					})
			},
			sendPhoto : function(elem, files) {
				var form = new FormData();
				form.append("image", $('#upload-form').children('input[type="file"]')[0].files[0]);
				form.append("name","blablabla");
				$.ajax({
					url: "workspace/document",
					type: 'POST',
					data: form,
					cache: false,
					contentType: false,
					processData: false
				}).done(function (data) {
					if (data.status == "ok") {
						account.action.editUserBookInfo("api/edit-userbook-info?prop=picture&value=" + data._id);
						$('img[class="avatar"]')[0].setAttribute("src", "workspace/document/" + data._id);
					}
				}).error(function (data) { console.log(data); });
			},
			getPhoto : function(photoId) {
				$.ajax({
					url: "document/" + photoId,
					type: 'GET'
				}).done(function (data) {
					if (data !== "") {
						$('img[class="avatar"]')[0].setAttribute("src", "workspace/document/" + photoId);
					}
				}).error(function (data) { console.log(data); });
			}
		}
	});
	return app;
}();

function manageEditable(){
	$('em[contenteditable="true"]').blur(function(){
		var parameters = "";
		var parentLine = $(this).parent().parent();
		if (parentLine.data('category')){
			parameters += "?category=" + parentLine.data('category') + "&values=" + $(this).text();
		} else {
			parameters += "?prop=" + $(this).data('property') + "&value=" + $(this).text();
		}
		account.action.editUserBookInfo("api/edit-userbook-info" + parameters);
	});

	$('.enhanced-select').on('change', function(){
		var parameters = "?prop=mood&value=" + $(this).data('selected');
		account.action.editUserBookInfo("api/edit-userbook-info" + parameters);
	})
}

$(document).ready(function(){
	account.init();
	account.action.profile("api/account");
});

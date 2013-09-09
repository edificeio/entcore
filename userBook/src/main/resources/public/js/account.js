var account = function(){

	var personDataExtractor = function(d) {
		var jo = {"displayName":d.result[0]["displayName"],
			"address":d.result[0]["address"],
			"mood":d.result[0]["mood"],
			"motto":d.result[0]["motto"],
			"health":d.result[0]["health"],
			"photo" : d.result[0]["photo"]};
		var hobbies = [];
		for (obj in d.result){
			if (d.result[obj].category !== ""){
				hobbies.push({
					"category":d.result[obj].category,
					"values":d.result[obj].values,
					"visibility":d.result[obj].visibility.toLowerCase()
				});
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
						<img src="document/{{photo}}?userbook-dimg=public%2Fimg%2Fno-avatar.jpg" alt="user" class="avatar"/>\
					</div>\
					<div class="eight cell text-container">\
						<article class="cell twelve text-container right-magnet">\
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
						<article class="twelve cell text-container right-magnet">\
							<h2>Ma photo</h2>\
							<form id="upload-form" method="post" action="workspace/document" enctype="multipart/form-data">\
								<!-- styling file inputs is currently impossible, so we hide them and display a replacement. -->\
								<div class="hidden-content">\
									<input type="file" name="file" value="Changer l\'image" id="avatar"/>\
								</div>\
								<button class="file-button" data-linked="avatar">Changer ma photo</button>\
							</form>\
						</article>\
					</div>\
				</div>\
				<div class="text-container clear">\
					<div class="enhanced-select" data-selected="{{mood}}">\
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
					<h1>{{#i18n}}userBook.interests{{/i18n}}</h1>\
					<article class="text-container">\
					{{#list}}\
						<div class="row line" data-category="{{category}}">\
							<div class="three cell"><span>{{category}}</span></div>\
							<div class="eight cell"><em contenteditable="true">{{values}}</em></div>\
							<div class="one cell"><i role="{{visibility}}" href="api/set-visibility?&category={{category}}" call="changeVisibility" class="right-magnet"></i></div>\
							<div class="clear"></div>\
						</div>\
					{{/list}}\
					</article>\
					<h1>{{#i18n}}userBook.profile.health{{/i18n}}</h3>\
					<article class="text-container">\
						<em contenteditable="true" data-property="health"> {{health}}</em>\
					</article>\
				</div>\
				'
		},
		action : {
			profile : function(url) {
				One.get(url)
				.done(function(data){
					$('#person').html(app.template.render('personne', personDataExtractor(data)));
					manageEditable();
					messenger.requireResize();
				})
			},
			editUserBookInfo : function(url){
				One.get(url)
				.done(function(data){
				})
			},
			setVisibility : function(o){
				$('#current-visibility').html = $('#visible').val();
				One.get(o.target.form.action, { value:  $('#visible').val() })
				.done(function(data){
					app.notify.info("modif ok");
				})
			},
			changeVisibility: function(o){
				var newRole = 'public';
				if($(o.target).attr('role') === 'public'){
					newRole = 'private';
				}

				One.get(o.url, {value: newRole })
					.done(function(data){
						$(o.target).attr('role', newRole);
					})
			},
			sendPhoto : function(elem, files) {
				var form = new FormData();
				form.append("image", $('#upload-form').find('input[type="file"]')[0].files[0]);
				form.append("name","blablabla");


				One.postFile("document?application=userbook&protected=true", form)
					.done(function (data) {
						if (data.status == "ok") {
							account.action.editUserBookInfo("api/edit-userbook-info?prop=picture&value=" + data._id);
							$('img[class="avatar"]')[0].setAttribute("src", "workspace/document/" + data._id);
						}
					});
			},
			getPhoto : function(photoId) {
				One.get("workspace/document/" + photoId)
					.done(function (data) {
						if (data !== "") {
							$('img[class="avatar"]')[0].setAttribute("src", "workspace/document/" + photoId);
						}
					});
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
	});

	$('#avatar').on('change', function(){
		account.action.sendPhoto(this);
	})
}

$(document).ready(function(){
	account.init();
	account.action.profile("api/account");
});

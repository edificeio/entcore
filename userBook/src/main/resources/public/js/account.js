// TODO : merge or mutualzie with userbook.js
var account = function(){

	var personDataExtractor = function(d) {
		var person = d.result[0];

		person['hobbies'] = [];
		d.result[0].category.forEach(function(c,index){
			person['hobbies'].push({
				"category" : c,
				"values" : d.result[0].values[index],
				"visibility" : d.result[0].visibility[index].toLowerCase()
			});
		});

		person['relations'] = [];
		_.values(d.result).forEach(function(o){
			person['relations'].push(_.pick(o, 'relatedId', 'relatedName','relatedType'));
		});

		// TODO : extract in conf system
		person['moods'] = ['default','happy','proud','dreamy','love','tired','angry','worried','sick','joker','sad'];
		return person;
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
					<div class="eight cell text-container right-magnet">\
						<article class="cell twelve text-container right-magnet">\
							<h2>{{displayName}}</h2>\
							<div class="row">\
								<div class="four cell"><label>{{#i18n}}userBook.profile.address{{/i18n}}</label></div>\
								<em class="six cell">{{address}}</em>\
							</div>\
							<div class="row">\
								<div class="four cell"><label>{{#i18n}}userBook.profile.motto{{/i18n}}</label></div>\
								<em class="six cell monoline" contenteditable="true" data-property="motto">{{motto}}</em>\
							</div>\
						</article>\
						<article class="twelve cell text-container right-magnet">\
							<h2>Ma photo</h2>\
							<form id="upload-form" method="post" action="document" enctype="multipart/form-data">\
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
						<div class="current"><i role="{{mood}}-panda" class="small-panda"></i><span>{{#i18n}}userBook.mood.{{mood}}{{/i18n}}</span></div>\
						<div class="options-list">\
							{{#moods}}\
							<div class="option" data-value="{{.}}">\
								<i role="{{.}}-panda" class="small-panda"></i>\
								<span>{{#i18n}}userBook.mood.{{.}}{{/i18n}}</span>\
							</div>\
							{{/moods}}\
						</div>\
					</div>\
					<h1>{{#i18n}}userBook.interests{{/i18n}}</h1>\
					<article class="text-container">\
					{{#hobbies}}\
						<div class="row line" data-category="{{category}}">\
							<div class="three cell"><span>{{#i18n}}userBook.hobby.{{category}}{{/i18n}}</span></div>\
							<div class="eight cell"><em contenteditable="true" class="monoline">{{values}}</em></div>\
							<div class="one cell"><i role="{{visibility}}" href="api/set-visibility?category={{category}}" call="changeVisibility" class="right-magnet"></i></div>\
							<div class="clear"></div>\
						</div>\
					{{/hobbies}}\
					</article>\
					<h1>{{#i18n}}userBook.profile.health{{/i18n}}</h3>\
					<article class="text-container">\
						<textarea data-property="health">{{health}}</textarea>\
					</article>\
				</div>\
				'
		},
		action : {
			profile : function() {
				One.get("api/person")
				.done(function(data){
					$('#person').html(app.template.render('personne', personDataExtractor(data)));
					manageEditable();
					messenger.requireResize();
				});
			},
			editUserBookInfo : function(url){
				One.get(url)
				.done(function(data){
				})
			},
			changeVisibility: function(o){
				var newRole = 'PUBLIC';
				if($(o.target).attr('role') === 'public'){
					newRole = 'PRIVE';
				}

				One.get(o.url, { value: newRole })
					.done(function(data){
						$(o.target).attr('role', newRole.toLowerCase());
					})
			},
			sendPhoto : function(elem, files) {
				var form = new FormData();
				form.append("image", $('#upload-form').find('input[type="file"]')[0].files[0]);
				form.append("name","blablabla");


				One.postFile("document?application=userbook&protected=true", form, {})
					.done(function (data) {
						if (data.status == "ok") {
							account.action.editUserBookInfo("api/edit-userbook-info?prop=picture&value=" + data._id);
							$('img[class="avatar"]')[0].setAttribute("src", "document/" + data._id);
						}
					});
			}
		}
	});
	return app;
}();

function manageEditable(){
	$('em[contenteditable="true"], textarea').blur(function(){
		var parameters = "";
		var parentLine = $(this).parent().parent();
		if (parentLine.data('category')){
			parameters += "?category=" + parentLine.data('category') + "&values=" + $(this).text();
		} else {
			var value = $(this).text();
			if($(this).prop('tagName').toLowerCase() === 'textarea'){
				value = $(this).val();
			}
			parameters += "?prop=" + $(this).data('property') + "&value=" + value;
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
	account.action.profile();

	var enterKey = 13;
	$(document).on('keypress', ".monoline", function(e){
		return e.which != enterKey;
	});

	$(document).on('keypress', ".multiline", function(e){
		messenger.requireResize();
	});
});

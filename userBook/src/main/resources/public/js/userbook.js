var userbook = function(){

	var classDataAdaptor = function (d) {
		var listStudents = [];
		var listTeachers = [];
		for (obj in d.result){
			if (d.result[obj].userId !== d.result[obj].id){
				d.result[obj].mood= 'default';
				d.result[obj].photo= '';
			}
			if (d.result[obj].type === 'ELEVE'){
				listStudents.push(d.result[obj]);
			} else if (d.result[obj].type === 'ENSEIGNANT'){
				listTeachers.push(d.result[obj]);
			}
		}
		return {students: listStudents, teachers: listTeachers}; 
	};
	var searchDataAdaptor = function (d) {
		var listStudents = [];
		var listTeachers = [];
		var listParents = [];
		for (obj in d.result){
			if (d.result[obj].photo ==='' && d.result[obj].mood ==='' && d.result[obj].userId === ''){
				d.result[obj].mood= 'default';
			}
			if (d.result[obj].type === 'ELEVE'){
				listStudents.push(d.result[obj]);
			} else if (d.result[obj].type === 'ENSEIGNANT'){
				listTeachers.push(d.result[obj]);
			} else {
				listParents.push(d.result[obj]);
			}
		}
		return {students: listStudents, teachers: listTeachers, parents: listParents}; 
	};
	var personDataAdaptor = function(d) {
		var jo = {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"]};
		var hobbies = [];
		var related = [];
		for (obj in d.result){
			if (d.result[obj].userId !== d.result[obj].id && d.result[obj].relatedType === 'USERBOOK'){
				jo['mood'] = 'default'; jo['health']=''; jo['photo']='';  jo['motto']='';
			} else if (d.result[obj].userId === d.result[obj].id){
				jo['mood'] = d.result[obj].mood; jo['health']=d.result[obj].health; 
				jo['photo']=d.result[obj].photo;  jo['motto']=d.result[obj].motto;
			}
			if (d.result[obj].category !== ""){
				hobbies.push({"category":d.result[obj].category,"values":d.result[obj].values});
			}
			if (d.result[obj].relatedType !== "USERBOOK"){
				related.push({"relatedName":d.result[obj].relatedName, "relatedId":d.result[obj].relatedId,"relatedType":d.result[obj].relatedType});
			}
		}
		jo['list'] = hobbies;
		jo['relations'] = related;
		return jo;
	};

	var app = Object.create(oneApp);
	app.scope = ".annuaire";
	app.define({
		template : {
			searchResults: '\
				{{#teachers}}\
				<div class="cell four text-container" >\
					<article class="box row person" id={{id}}>\
							<div class="four cell avatar"><img src="document/{{photo}}?userbook-dimg=public%2Fimg%2Fno-avatar.jpg" alt="user" /></div>\
							<div class="six cell">\
								<div class="row">\
									<h4 href="api/person?id={{id}}&type={{type}}" call="person">{{displayName}}</h4>\
								</div>\
								<div class="row bottom-locked">\
									<span class="actions">\
									<i role="send-mail"></i>\
									<i role="view-book"></i>\
									<i role="view-folder"></i>\
								</span>\
								</div>\
							</div>\
							<div class="two cell"><i role="{{mood}}-panda" class="liquid"></i></div>\
					</article>\
				</div>\
				{{/teachers}}\
				<div class="clear"></div>\
				{{#parents}}\
				<div class="cell four text-container" >\
					<article class="box row person" id={{id}}>\
							<div class="four cell avatar"><img src="document/{{photo}}?userbook-dimg=public%2Fimg%2Fno-avatar.jpg" alt="user" /></div>\
							<div class="six cell">\
								<div class="row">\
									<h4 href="api/person?id={{id}}&type={{type}}" call="person">{{displayName}}</h4>\
								</div>\
								<div class="row bottom-locked">\
									<span class="actions">\
									<i role="send-mail"></i>\
									<i role="view-book"></i>\
									<i role="view-folder"></i>\
								</span>\
								</div>\
							</div>\
							<div class="two cell"><i role="{{mood}}-panda" class="liquid"></i></div>\
					</article>\
				</div>\
				{{/parents}}\
				<div class="clear"></div>\
				{{#students}}\
				<div class="cell four text-container" >\
					<article class="box row person" id={{id}}>\
							<div class="four cell avatar"><img src="document/{{photo}}?userbook-dimg=public%2Fimg%2Fno-avatar.jpg" alt="user" /></div>\
							<div class="six cell">\
								<div class="row">\
									<h4 href="api/person?id={{id}}&type={{type}}" call="person">{{displayName}}</h4>\
								</div>\
								<div class="row bottom-locked">\
									<span class="actions">\
									<i role="send-mail"></i>\
									<i role="view-book"></i>\
									<i role="view-folder"></i>\
								</span>\
								</div>\
							</div>\
							<div class="two cell"><i role="{{mood}}-panda" class="liquid"></i></div>\
					</article>\
				</div>\
				{{/students}}',
			personne: '\
				<div class="row box">\
					<div class="avatar cell four">\
						<img src="document/{{photo}}?userbook-dimg=public%2Fimg%2Fno-avatar.jpg" alt="user" />\
					</div>\
					<article class="cell eight text-container right-magnet">\
						<h2>{{displayName}}</h2>\
						<div class="row">\
							<div class="four cell">{{#i18n}}userBook.profile.address{{/i18n}}</div>\
							<em class="six cell">{{address}}</em>\
						</div>\
						<div class="row">\
							<div class="four cell">{{#i18n}}userBook.profile.motto{{/i18n}}</div>\
							<em class="six cell">{{motto}}</em>\
						</div>\
						<div class="row mini-box">\
							<div class="two cell avatar"><i role="{{mood}}-panda"></i></div>\
							<em class="ten cell text-container mood">Je suis rêveuse</em>\
						</div>\
					</article>\
				</div>\
				<article id="actions" class="row text-container">\
					<div class="row mini-box">\
						<div class="cell one"><i role="send-mail"></i></div>\
						<a class="cell ten">{{#i18n}}userBook.class.write-message{{/i18n}}</a>\
					</div>\
					<div class="row mini-box">\
						<div class="cell one"><i role="view-book"></i></div>\
						<a class="cell ten">{{#i18n}}userBook.class.edit-notebook{{/i18n}}</a>\
					</div>\
					<div class="row mini-box">\
						<div class="cell one"><i role="view-folder"></i></div>\
						<a class="cell ten">{{#i18n}}userBook.class.see-portfolio{{/i18n}}</a>\
					</div>\
					<div class="clear"></div>\
				</article>\
				<h1 class="clear mini-box">{{#i18n}}userBook.profile.family{{/i18n}}</h1>\
				<article class="text-container">\
					{{#relations}}\
						<p>\
							<a href="api/person?id={{relatedId}}&type={{relatedType}}" call="person">{{relatedName}}</a>\
						</p>\
					{{/relations}}\
				</article>\
				<h1>{{#i18n}}userBook.interests{{/i18n}}</h1>\
				<article class="text-container">\
					{{#list}}\
					<div class="row line">\
						<div class="three cell">{{category}}</div>\
						<div class="eight cell"><em>{{values}}</em></div>\
						<div class="one cell"></div>\
						<div class="clear"></div>\
					</div>\
					{{/list}}\
					<div class="clear"></div>\
				</article>\
				<h1>{{#i18n}}userBook.profile.health{{/i18n}}</h1>\
				<article class="text-container">{{health}}</article>\
	'
		},
		action : {
			search : function(o){
				var url = o.target.form.action + '?' + $('#search-form').serialize();
				One.get(url)
				.done(function(data){
					$("#people").removeClass('four').addClass('twelve');
					$("#person").html('');
					if (data.result[0] === undefined) {
						app.notify.info("no results !");
						$("#people").html('');
						$("#person").html('');
					} else {
						$("#people").html(app.template.render('searchResults', searchDataAdaptor(data)));
					}

					messenger.requireResize();
				})
				.error(function(data){app.notify.error(data.status);});
			},
			showList: function(){
				this.refreshClassList();
			},
			showPerson: function(data){
				$("#people .text-container.four").addClass('twelve').removeClass('four');
				$("#people").addClass('four').removeClass('twelve');
				$("#people .selected").removeClass('selected');
				if (data.result[0].type !== 'PERSRELELEVE'){
					$('#' + data.result[0].id).addClass('selected');
				}
				$("#person").html(app.template.render('personne', personDataAdaptor(data)));
				$('i[role=show-icons]').removeClass('selected');
				$('i[role=show-details]').addClass('selected');
			},
			showFirstPerson: function(){
				var that = this;
				One.get($('.person').first().find('h4').attr('href'))
					.done(function(data){
						that.showPerson(data);
						messenger.requireResize();
					});
			},
			person : function(o){
				var that = this;
				One.get(o.url)
					.done(function(data){
						that.showPerson(data);
						messenger.requireResize();
						$('em.mood').html(moods[data.result[0].mood]);
					});
			},
			refreshClassList: function(){
				if (location.search.substring(1,8) === 'myClass'){
					userbook.action.searchClass("api/class");
				}
			},

			getPhoto : function(photoId, userId) {
				if (photoId === ''){
					return;
				}
				One.get("document/" + photoId)
					.done(function (data) {
						if (data !== "") {
							if (userId !== ''){
								$('article#'+ userId +' div.avatar img')[0].setAttribute('src',"document/" + photoId);
							} else {
								$('div#person div.avatar img')[0].setAttribute('src',"document/" + photoId);
							}
						}
					});
			},
			searchClass : function(url) {
				One.get(url)
				.done(function(data){
					$("#people .text-container.twelve").addClass('four').removeClass('twelve');
					$("#people").removeClass('four').addClass('twelve');
					$("#people .selected").removeClass('selected');
					$('#person').html('');
					if (data.result[0] === undefined) {
						app.notify.info("no results !");
						$("#people").html('');
						$("#person").html('');
					} else {
						$("#people").html(app.template.render('searchResults', classDataAdaptor(data)));
					}
					messenger.requireResize();
				});
			}
		}
	});
	return app;
}();


$(document).ready(function(){
	userbook.init();
	userbook.action.refreshClassList();
	if (location.href.indexOf("#",0)!==-1){
		var id = location.href.split("#")[1];
		userbook.action.person(
			{"url":"api/person?id="+ location.href.split("#")[1] + "&type=" + location.href.split("#")[2]}
		);
	}
});

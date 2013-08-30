var userbook = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };
	var personDataExtractor = function(d) {
		var jo = {"displayName":d.result[0]["displayName"],"address":d.result[0]["address"]};
		var hobbies = [];
		var related = [];
		for (obj in d.result){
			if (d.result[obj].category !== ""){
				hobbies.push({"category":d.result[obj].category,"values":d.result[obj].values});
			}
			if (d.result[obj].mood !== ""){
				jo['mood'] = d.result[obj].mood;
				jo['health'] = d.result[obj].health;
				jo['motto'] = d.result[obj].motto;
				related.push({"relatedName":d.result[obj].relatedName, "relatedId":d.result[obj].relatedId,"relatedType":d.result[obj].relatedType});
			}
			if (d.result[obj].relatedName !== ""){
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
				{{#list}}\
				<div class="cell four text-container" >\
					<article class="box row person" id={{id}}>\
							<div class="four cell avatar"><img src="/public/img/no-avatar.jpg" alt="user" /></div>\
							<div class="six cell">\
								<div class="row">\
									<h4 href="/api/person?id={{id}}&type={{type}}" call="person">{{displayName}}</h4>\
								</div>\
								<div class="row bottom-locked">\
									<span class="actions">\
									<i role="send-mail"></i>\
									<i role="view-book"></i>\
									<i role="view-folder"></i>\
								</span>\
								</div>\
							</div>\
							<div class="two cell"><img src="/public/img/reveur.png" alt="panda" /></div>\
					</article>\
				</div>\
				{{/list}}',
			personne: '\
				<div class="row box">\
					<div class="avatar cell four">\
						<img src="/public/img/no-avatar.jpg" alt="user" />\
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
							<div class="two cell avatar"><img src="/public/img/reveur.png" alt="panda" /></div>\
							<em class="ten cell text-container">Je suis rêveuse</em>\
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
							<a href="/api/person?id={{relatedId}}&type={{relatedType}}" call="person">{{relatedName}}</a>\
						</p>\
					{{/relations}}\
				</article>\
				<h1>{{#i18n}}userBook.profile.health{{/i18n}}</h1><p>{{health}}</p>\
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
				</article>'
		},
		action : {
			search : function(o){
				var url = o.target.form.action + '?' + $('#search-form').serialize();
				$.get(url)
				.done(function(data){
					$("#people").addClass('all').removeClass('single');
					$("#person").html('');
					if (data.result[0] === undefined) {
						app.notify.info("no results !");
						$("#people").html('');
						$("#person").html('');
					} else {
						$("#people").html(app.template.render('searchResults', dataExtractor(data)))
					}

					messenger.requireResize();
				})
				.error(function(data){app.notify.error(data.status);})
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
				$("#person").html(app.template.render('personne', personDataExtractor(data)));
			},
			showFirstPerson: function(){
				var that = this;
				$.get($('.person').first().find('h4').attr('href'))
					.done(function(data){
						that.showPerson(data)
						messenger.requireResize();
					})
					.error(function(data){app.notify.error(data.status);})
			},
			person : function(o){
				var that = this;
				$.get(o.url)
				.done(function(data){
					that.showPerson(data)
					messenger.requireResize();
				})
				.error(function(data){app.notify.error(data.status);})
			},
			refreshClassList: function(){
				if (location.search.substring(1,6) === 'query'){
					var className = location.search.split('class=')[1];
					userbook.action.searchClass("/api/class?name=" + className);
				}
			},
			searchClass : function(url) {
				$.get(url)
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
						$("#people").html(app.template.render('searchResults', dataExtractor(data)));
					}

					messenger.requireResize();
				})
				.error(function(data){app.notify.error(data)})
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	userbook.init();
	userbook.action.refreshClassList();
});

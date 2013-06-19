var admin = function(){

	var dataExtractor = function (d) { return {list : _.values(d.result)}; };

	var app = Object.create(oneApp);
	app.scope = "#annuaire";
	app.define({
		template : {
			personnes: '\
				<br /><span>{{#list}}<div id="person"><img src="/public/img/no-avatar.jpg" alt="user" class="avatar"/>\
				<span><a href="">{{lastName}} {{firstName}}</a></span><img src="/public/img/reveur.png" alt="panda" class="mood"/>\
				</div>{{/list}}</span>'
			},
		action : {
			personnes : function(o) {
				$.get(o.url)
				.done(function(data){
					$("#people").html(app.template.render('personnes', dataExtractor(data)));
				})
				.error(function(data){app.notify.error(data)})
			}
		}
	})
	return app;
}();


$(document).ready(function(){
	admin.init();
});
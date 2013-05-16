var admin = function(){
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.define ({
		template : { 
			test : "<dl>\
					<dt>{{#i18n}}sync.admin.time{{/i18n}} :</dt><dd>{{temps}}</dd>\
					<dt>{{#i18n}}sync.admin.operation{{/i18n}} :</dt><dd>{{operations}}</dd>\
					<dt>{{#i18n}}sync.admin.rejects{{/i18n}} :</dt><dd>{{rejets}}</dd>\
					</dl>"
		},
		action : {
			test : function (o) {
				$.get(o.url).done(function(response){
					$('#test').html(app.template.render("test",response.result));
					app.notify.done(app.i18n.bundle["sync.admin.endMsg"]);
				})
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	admin.init();
});
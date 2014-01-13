var admin = function() {
	var app = Object.create(protoApp);
	app.scope = "#main";
	app.define ({
		template : {
			logs : '<li>\
				<span class="history-level title">Type</span>\
				<span class="history-message title">Message</span>\
				<span class="history-date title">Date</span></li>\
				{{#records}}<li>\
					<span class="history-level">{{level}}</span>\
					<span class="history-message">\
						<span class="history-app label history-badge-warning">{{app}}</span>\
						{{message}}</span>\
					<span class="history-date">{{#formatDate}}{{date}}{{/formatDate}}</span>\
				</li>{{/records}}'
		},
		action : {
			logs : function(o) {
				$.get(o.url).done(function(data) {
					$('#log').html(app.template.render("logs", {"records" : data.records}));
				});
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	admin.init();
});

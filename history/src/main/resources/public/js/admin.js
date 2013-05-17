var admin = function() {
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.start = function() {
		this.init();
		window.addEventListener('message', this.action.styleApp, false);
	};
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
				var formatDate = function() {
						return function(str) {
							var tab = Mustache.render(str, this).split(" ");
							var dt = new Date(tab[5] + '-' + tab[2] + '-' + tab[1] + ' ' + tab[3]).toLocaleDateString();
							return dt;
						};
					};

				$.get(o.url)
				.done(function(data) {
					$('#log').html(app.template.render("logs", _.extend({"records" : data.records}, {"formatDate" : formatDate})));
				})

			},
			styleApp : function(e) {
				if (event.origin == "http://localhost:8008") {
					$("head").append("<link rel='stylesheet' href='" + e.data + "' media='all' />");
				}
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	admin.start();
});

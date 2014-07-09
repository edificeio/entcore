// Copyright © WebServices pour l'Éducation, 2014
//
// This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as
// published by the Free Software Foundation (version 3 of the License).
//
// For the sake of explanation, any module that communicate over native
// Web protocols, such as HTTP, with ENT Core is outside the scope of this
// license and could be license under its own terms. This is merely considered
// normal use of ENT Core, and does not fall under the heading of "covered work".
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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

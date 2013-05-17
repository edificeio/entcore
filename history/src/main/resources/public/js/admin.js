var admin = function() {
	var app = Object.create(oneApp);
	app.scope = "#main";
	app.start = function() {
		this.init();
		window.addEventListener('message', this.action.styleApp, false);
	};
	app.define ({
		template : {
			list : '<li><span class="history-level title">Type</span><span class="history-message title">Message</span><span class="history-date title">Date</span></li>{{#list}}<li><span class="history-level">{{level}}</span><span class="history-message"><span class="history-app label history-badge-warning">{{app}}</span>{{message}}</span><span class="history-date">{{date}}</span></li>{{/list}}',
			getAndRenderHistory : function (pathUrl, templateName, elem, dataExtractor){
				var that = this;
				if (_.isUndefined(dataExtractor)) {
					dataExtractor = function (d) { return {list : _.values(d.records)}; };
				}
				$.get(pathUrl)
				.done(function(data) {
					$(elem).html(that.render(templateName, dataExtractor(data)));
				})
				.error(function(data) {
					oneApp.notify.error(data);
				});
			}
		},
		action : {
			logs : function(o) {
				app.template.getAndRenderHistory(o.url, "list", "#log", function(data) {
					var transformDate = function(str) {
						var tab = str.split(" ");
						var mois = ["janvier", "février", "mars", "avril", "mai", "juin", "juillet", "aout", "septembre", "octobre", "novembre", "décembre"];
						var jours = ["dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"];
						var dt = new Date(tab[5] + '-' + tab[2] + '-' + tab[1] + ' ' + tab[3]);
						return (jours[dt.getDay()] + ' ' + dt.getDay() + ' ' + mois[dt.getMonth()] + ' ' + dt.getFullYear() + ' - ' + tab[3] + ' ' + tab[4]);
					};
					var list = {list: []};
					for (var i = 0, nb = data.records.length; i < nb; i++) {
						list.list.push({
							level : data.records[i].level,
							app : data.records[i].app,
							message : data.records[i].message,
							date : transformDate(data.records[i].date)
						});
					}
					return list;
				});

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

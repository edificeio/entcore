	var admin = function(){
		var getAndRender = function (pathUrl, templateName){
			$.get(pathUrl)
				.done(function(data) {
					template.render(templateName, data);
				})
				.error(function() { // TODO: Manage error message
					template.render("error");
				});
		};

		var template = {
			render : function (nom, data) {
				template[nom](data);
			},
			error: function() {
				$('#log').html("ERREUR !");
			},
			logs : function (data) {
				var logs = data.records;
				var htmlString = '<li><span class="history-level title">Type</span>'
					+ '<span class="history-message title">Message</span>'
					+ '<span class="history-date title">Date</span>'
					+ '</li>';
				for (i = 0; i < logs.length; i++){
					htmlString +=
						'<li>'
						+ '<span class="history-level"> ' + logs[i].level + '</span>'
						+ '<span class="history-message">'+ '<span class="history-app label history-badge-warning">' + logs[i].app + '</span>' + logs[i].message + '</span>'
						+ '<span class="history-date">' + transformDate(logs[i].date) + '</span>'
						+ '</li>';
				}
				$('#log').html(htmlString);
			}
		};

		var transformDate = function(str) {
			var tab = str.split(" ");
			var mois = ["janvier", "février", "mars", "avril", "mai", "juin", "juillet", "aout", "septembre", "octobre", "novembre", "décembre"];
			var jours = ["dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"];
			var dt = new Date(tab[5] + '-' + tab[2] + '-' + tab[1] + ' ' + tab[3]);
			return (jours[dt.getDay()] + ' ' + dt.getDay() + ' ' + mois[dt.getMonth()] + ' ' + dt.getFullYear() + ' - ' + tab[3] + ' ' + tab[4]);
		};

		var receiver = function(event) {
			if (event.origin == "http://localhost:8008") {
				$("head").append("<link rel='stylesheet' href='" + event.data + "' media='all' />");
			}
		};

		return {
			init : function() {
				$('body').delegate('#historique', 'click',function(event) {
					event.preventDefault();
					if (!event.target.getAttribute('call')) return;
					var call = event.target.getAttribute('call');
					admin[call]({url : event.target.getAttribute('href'), id: event.id});
				});
				window.addEventListener('message', receiver, false);
			},
			logs : function(o) {
				getAndRender(o.url, "logs");
			}
		}
	}();

	$(document).ready(function(){
		admin.init(); 
	});
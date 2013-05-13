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
				var htmlString = "";
				var logs = data.records;
				for (i = 0; i < logs.length; i++){
					htmlString +=
						'<li>'
						+ '<span class="history-level"> ' + logs[i].level + '</span>'
						+ '<span class="history-message">'+ '<span class="history-app label history-badge-warning">' + logs[i].app + '</span>' + logs[i].message + '</span>'
						+ '<span class="history-date">' + logs[i].date + '</span>'
						+ '</li>';
						console.log(transformDate(logs[i].date));
				}
				$('#log').html(htmlString);
			}
		};

		var transformDate = function(str) {
			var tab = str.split(" ");
			var months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
			
			var dt = tab[2] + '-' + tab[1] + '-' + tab[5];
			return (dt);
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
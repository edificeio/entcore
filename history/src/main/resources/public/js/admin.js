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
				var htmlString = "<table><thead><tr><th>Type</th><th>Application</th><th>Date</th><th>Message</th></tr></thead><tbody>";
				var logs = data.records;
				for (i = 0; i < logs.length; i++){
					htmlString +=
						'<tr><td>' + logs[i].level + '</td>'
						+ '<td>' + logs[i].app + '</td>'
						+ '<td>' + logs[i].date + '</td>'
						+ '<td>' + logs[i].message + '</td></tr>';
				}
				htmlString += "</tbody></table>";
				$('#log').html(htmlString);
			}
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
				admin[$('#select').attr('call')]({url: $('#select').val()});
				$('#select').on('change', function() {
					var call = this.getAttribute('call');
					admin[call]({url : this.value});
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
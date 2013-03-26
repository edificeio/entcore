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
				$('#test').html("ERREUR !");
			},
			test : function (data) {
				var htmlString =
						'<dl>'
						+ '<dt>Temps :</dt><dd>' + data.result.temps + '</dd>'
						+ '<dt>Opération :</dt><dd>' + data.result.operations + '</dd>';
						+ '</dl>'
				$('#test').html(htmlString);
			}
		};

		return {
			init : function() {
				$('body').delegate('#main', 'click',function(event) {
					event.preventDefault();
					if (!event.target.getAttribute('call')) return;
					var call = event.target.getAttribute('call');
					admin[call]({url : event.target.getAttribute('href'), id: event.id});
				});
			},
			test : function(o) {
				getAndRender(o.url, "test");
			}
		}
	}();

	$(document).ready(function(){
		admin.init();
	});
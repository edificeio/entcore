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
			}
		};

		return {
			init : function() {
				$('body').delegate('#menu', 'click',function(event) {
					event.preventDefault();
					if (!event.target.getAttribute('call')) return;
					var call = event.target.getAttribute('call');
					admin[call]({url : event.target.getAttribute('href'), id: event.target.id});

					$('#appIframe').load(function() {
						this.contentWindow.postMessage("<style>h1 {color: red;}</style>", this.src);
					});
				});
			},
			displayApp : function(o) {
				$('#' + o.id).siblings().removeClass('active');
				$('#' + o.id).addClass('active');
				$('#appIframe').attr('src', o.url);
			}
		};
	}();

	$(document).ready(function(){
		admin.init(); 
	});

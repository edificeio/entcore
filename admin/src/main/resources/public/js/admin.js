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
                                        $('#menu li').siblings().removeClass("active");
					event.target.parentNode.className += " active";
					admin[call]({url : event.target.getAttribute('href'), id: event.id});
				});
			},
			displayApp : function(o) {
				$('#appIframe').attr('src', o.url);
                                $('#appIframe').load(o.url, function() {
                                    var iframe = document.getElementById("appIframe");
                                    $('#appIframe').ready(function(){
                                            iframe.contentWindow.postMessage("<style>h1 {color: red;}</style>", o.url);
                                    });
                                });
			}
		}
	}();

	$(document).ready(function(){
		admin.init(); 
	});

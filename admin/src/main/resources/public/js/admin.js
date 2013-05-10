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
					admin[call]({url: event.target.getAttribute('href'), id: event.target.id});
				});
				this.displayApp({url: $("#directory").attr("href"), id: "directory"});
			},
			displayApp : function(o) {
				var iframe = document.getElementById(o.id + "-frame");
				var style = 'http://localhost:8008/public/css/test.css';
				if (!iframe) {
					$('<iframe />', {
						id: o.id + '-frame',
						src: o.url,
						frameBorder: '0'
					}).load(function() {
						this.contentWindow.postMessage(style, o.url);
					}).appendTo('#main');
				}
				$('#' + o.id).parent().addClass('active');
				$('#' + o.id).parent().siblings().removeClass('active');
				$('#' + o.id + '-frame').siblings().css('display', 'none');
				$('#' + o.id + '-frame').css('display', 'inline');
			}
		};
	}();

	$(document).ready(function(){
		admin.init(); 
	});

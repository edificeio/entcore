var admin = function() {
	var app = Object.create(oneApp);
	app.scope = "#menu";
	app.start = function(){
		this.init();
		this.action.displayApp({url: $("#directory").attr("href"), id: "directory"});
	};
	app.define ({
		action : {
			displayApp : function(o) {
				var style = 'http://localhost:8008/public/css/test.css';
				if ($('#iframe' + '-frame')) {
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
		}
	});
	return app;
}();

$(document).ready(function(){
	admin.start();
});
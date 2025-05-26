var admin = function() {
	var app = Object.create(protoApp);
	app.scope = "#menu";
	app.start = function(){
		this.init();
		this.action.displayApp({url: $("#directory").attr("href"), target: $("#directory")[0]});
	};
	app.define ({
		action : {
			displayApp : function(o) {
				var style = '/public/css/test.css';
				if ($('#iframe' + '-frame')) {
					$('<iframe />', {
						id: o.target.id + '-frame',
						src: o.url,
						frameBorder: '0'
					}).load(function() {

						$('head', this.contentWindow.document).prepend($('<link>', {
							rel: 'stylesheet',
							type: 'text/css',
							href: '/public/css//admin/font.css'
						}))
					}).appendTo('#main');
				}
				$('#' + o.target.id).parent().addClass('active');
				$('#' + o.target.id).parent().siblings().removeClass('active');
				$('#' + o.target.id + '-frame').siblings().css('display', 'none');
				$('#' + o.target.id + '-frame').css('display', 'inline');
			}
		}
	});
	return app;
}();

$(document).ready(function(){
	admin.start();
});
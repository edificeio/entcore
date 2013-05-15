var oneApp = {
	scope : '#main',
	init : function() {
		var that = this;
		that.i18n.load();
		$('body').delegate(that.scope, 'click',function(event) {
			event.preventDefault();
			if (!event.target.getAttribute('call')) return;
			var call = event.target.getAttribute('call');
			that.action[call]({url : event.target.getAttribute('href'), target: event.target});
		});
	},
	action : {
	},
	template : {
		getAndRender : function (pathUrl, templateName, elem, dataExtractor){
			var that = this;
			if (_.isUndefined(dataExtractor)) {
				dataExtractor = function (d) { return {list : _.values(d.result)}; };
			}
			$.get(pathUrl)
			.done(function(data) {
				var jo = jQuery.parseJSON(data)
				$(elem).html(that.render(templateName, dataExtractor(jo)));
			})
			.error(function(data) {
				oneApp.notify.error(data);
			});
		},
		render : function (name, data) {
			_.extend(data, {i18n : oneApp.i18n.i18n});
			return Mustache.render(this[name], data);
		}
	},
	notify : {
		done : function (msg) { this.instance('success')(msg);},
		error : function (msg) { this.instance('error')(msg); },
		warn : function (msg) {},
		info : function (msg) { this.instance('info')(msg); },
		instance : function(level) {
			return humane.spawn({ addnCls: 'humane-original-' + level, timeout: 3000 });
		}
	},
	i18n : {
		load : function () {
			var that = this;
			$.get('/i18n').done(function(data) {
				that.bundle = data;
			});
		},
		bundle : {},
		i18n : function() {
			return function(key) { return oneApp.i18n.bundle[key] ; };
		}
	},
	define : function (o) {
		var props = { template : {}, action:{}};
		for (prop in props) {
			for (key in o[prop]) {
				props[[prop]][key] = {'value' : o[[prop]][key]};
			}
			Object.defineProperties(this[prop], props[[prop]]);
		}
	}
};
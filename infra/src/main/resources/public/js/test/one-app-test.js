
var assert = buster.assert;

buster.testCase("one-app.js test", {
	setUp: function () {
		var app = Object.create(oneApp);
		this.app = app;
		if (oneApp.action.simple !== undefined) return; // hack : oneApp is in global namespace
		app.define({
			template : {
				simple : "My name is {{fullname}}",
				collection : "{{#peoples}}{{fullname}} et {{/peoples}}",
				i18n : "{{#i18n}}test.key.one{{/i18n}}",
				date : "date : {{#formatDate}}{{myDate}}{{/formatDate}}"
			},
			action : {
				simple : function(data) {
					return app.template.render("simple",data);
				}
			}
		});
		app.i18n.bundle = {
			"test.key.one" : "test i18n un",
			"test.key.two" : "test i18n deux"
		};
	},
	"template render :" : {
		"simple": function () {
			assert.equals(this.app.action.simple({"fullname":"Bob"}), "My name is Bob");
			assert.equals(this.app.template.render("simple", {"fullname":"Bob"}), "My name is Bob");
		},
		"direct": function () {
			assert.equals(this.app.template.render("My name is {{fullname}}", {"fullname":"Bob"}), "My name is Bob");
		},
		"collection": function () {
			var data = {"peoples" : [{"fullname":"Bob"}, {"fullname":"Gary"}]};
			assert.equals(this.app.template.render("collection", data), "Bob et Gary et ");
		},
		"i18n": function () {
			assert.equals(this.app.template.render("i18n", {}), "test i18n un");
		},
		"date": function () {
			assert.equals(this.app.template.render("date", {"myDate":"2013-05-21"}), "date : 21/5/2013");
			assert.equals(this.app.template.render("date", {"myDate":"Tue May 21 2013 GMT+0200 (CEST)"}), "date : 21/5/2013");
		}
	}

});

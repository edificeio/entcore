import { idiom as lang, idiom as idiom } from './idiom';

var humane = require('humane-js');

export var notify = {
	message: function(type, message){
		message = lang.translate(message);
		humane.spawn({ addnCls: 'humane-original-' + type })(message);
	},
	error: function(message){
		this.message('error', message);
	},
	info: function(message){
		this.message('info', message)
	},
	success: function(message){
		this.message('success', message);
	}
};

if(!(window as any).entcore){
	(window as any).entcore = {};
}
(window as any).entcore.notify = notify;
(window as any).notify = notify;
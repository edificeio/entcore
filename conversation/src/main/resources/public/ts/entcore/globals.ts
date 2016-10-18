import { idiom } from './idiom';

require('core.js');
var _ = require('underscore');
(window as any)._ = _;
(window as any).lang = idiom;

declare let moment: any;

if(!(window as any).entcore){
	(window as any).entcore = {};
}

if((window as any).appPrefix === undefined){
    if(!(window as any).entcore){
        (window as any).entcore = {};
    }
	if(window.location.pathname.split('/').length > 0){
		(window as any).appPrefix = window.location.pathname.split('/')[1];
        (window as any).entcore.appPrefix = (window as any).appPrefix;
	}
}

var xsrfCookie;
if(document.cookie){
    var cookies = _.map(document.cookie.split(';'), function(c){
        return {
            name: c.split('=')[0].trim(), 
            val: c.split('=')[1].trim()
        };
    });
    xsrfCookie = _.findWhere(cookies, { name: 'XSRF-TOKEN' });
}

export var appPrefix: string = (window as any).appPrefix;

if((window as any).infraPrefix === undefined){
	(window as any).infraPrefix = 'infra';
}

export var infraPrefix: string = (window as any).infraPrefix;

export var currentLanguage = '';
(function(){

    // User preferences language
    var preferencesRequest = new XMLHttpRequest();
	preferencesRequest.open('GET', '/userbook/preference/language');
    if(xsrfCookie){
        preferencesRequest.setRequestHeader('X-XSRF-TOKEN', xsrfCookie.val);
    }
	(preferencesRequest as any).async = false;

	preferencesRequest.onload = function(){
        var fallBack = function(){
            // Fallback : navigator language
            var request = new XMLHttpRequest();
            request.open('GET', '/locale');
            if(xsrfCookie){
                request.setRequestHeader('X-XSRF-TOKEN', xsrfCookie.val);
            }
            (request as any).async = false;
            request.onload = function(){
                if(request.status === 200){
                    currentLanguage = JSON.parse(request.responseText).locale;
                    if((window as any).moment){
                        if (currentLanguage === 'fr') {
                            moment.updateLocale(currentLanguage, {
                                calendar: {
                                    lastDay: '[Hier à] HH[h]mm',
                                    sameDay: '[Aujourd\'hui à] HH[h]mm',
                                    nextDay: '[Demain à] HH[h]mm',
                                    lastWeek: 'dddd [dernier à] HH[h]mm',
                                    nextWeek: 'dddd [prochain à] HH[h]mm',
                                    sameElse: 'dddd LL'
                                }
                            });
                        }
                        else {
                            moment.lang(currentLanguage);
                        }
                    }
                }
            };
            request.send(null);
        }

        if(preferencesRequest.status === 200){
            try{
    			currentLanguage = JSON.parse(JSON.parse(preferencesRequest.responseText).preference)['default-domain'];
    		} catch(e) {
    			fallBack();
    		}
        }

        if(!currentLanguage)
            fallBack();
    };
    preferencesRequest.send(null);

}());

if(document.addEventListener){
	document.addEventListener('DOMContentLoaded', function(){
		document.getElementsByTagName('body')[0].style.display = 'none';
	});
}

export let routes:any = {
	define: function(routing){
		this.routing = routing;
	}
};

export let cleanJSON = (obj) => {
    if (!obj) {
        return obj;
    }
    
    if(obj instanceof Array){
        return obj.map((e) => cleanJSON(e));
    }

    let dup = {};
    
    if (obj.toJSON) {
        dup = obj.toJSON();
        return dup;
    }
    for (let prop in obj) {
        if (typeof obj[prop] === 'object' && !(obj[prop] instanceof Array)) {
            dup[prop] = cleanJSON(obj[prop])
        }
        else {
            if (obj[prop] instanceof Array) {
                dup[prop] = [];
                for (let el of obj[prop]) {
                    dup[prop].push(cleanJSON(el));
                }
            }
            else if (obj.hasOwnProperty(prop) && prop !== 'callbacks' && prop !== 'data' && prop !== '$$hashKey') {
                dup[prop] = obj[prop];
            }
        }
    }
    return dup;
}

(window as any).entcore.routes = routes;
(window as any).entcore.cleanJSON = cleanJSON;

if(!Array.prototype.forEach){
	window.location.href = "/auth/upgrade";
}

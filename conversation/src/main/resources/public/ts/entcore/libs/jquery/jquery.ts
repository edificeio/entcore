export let $ = require('jquery');
(window as any).jQuery = $;
if(!(window as any).entcore){
    (window as any).entcore = {};
}
(window as any).entcore.jQuery = $;
(window as any).entcore.$ = $;
(window as any).$ = $;
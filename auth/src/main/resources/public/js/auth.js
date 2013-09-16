window.addEventListener('load', function(e){
	[].forEach.call(document.querySelectorAll('.remove-fout'), function(item){
		item.className = item.className.replace('remove-fout','');
	});
	if(parent !== window){
		var url = window.location.href.split('callback=');
		var callback = escape('/#app=' + url[1]);
		var base = url[0];
		parent.location.href = base + 'callback=' + callback;
	}
})


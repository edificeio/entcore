var admin = function(){

	var getAndRender = function (pathUrl, templateName){
		$.get(pathUrl)
			.done(function(data) {
				template.render(templateName, data);
			})
	};
	var postAndRender = function (form, templateName){
		$.post(form.getAttribute('action'), $('#edit').serialize())
			.done(function(data){
				console.log(data);
				template.render(templateName, data);
			})
	};

	var template = {
		render : function (nom, data) {
			template[nom](data);
		},
		ecole : function (data) {
			var htmlString = '';
			for (i=0; i<data.length;i++){
				htmlString +="<h2>" + data[i].nom + "</h2>";
			}
			$("#main").html(htmlString + "<a call='classes' href='/api/classes'>Voir les classes</a>");
		},
		classes: function(data) {
			if (!!$('#classes').children().length) {
				$('#classes').html('');
				$('#people').html('');
				return;
			}
			var htmlString = '';
			for (i=0; i<data.length;i++){
				htmlString +="<h3><a>" + data[i].nom + "</a></h3>";
			}
			$("#classes").html(htmlString + "<a call='personnes' href='/api/personnes'>Voir les personnes</a>");
		},
		personnes : function(data) {
			if (!!$('#people').children().length) {
				$('#people').html('');
				return;
			}
			var htmlString='';
			for (i=0; i<data.length;i++){
				htmlString +="<h4><a call='personne' href='/api/details?id="
					+ data[i].id +"'>" + data[i].prenom + " " + data[i].nom + "</a></h3>"
					+ "<div id='details"+ data[i].id +"'></div>";
			}
			$("#people").html(htmlString);
		},
		personne : function(data) {
			if (!!$('#details' + data.id).children('form').length) {
				$('#details' + data.id).html('');
				return;
			}
			htmlString = "<form id='edit' action='/api/edit' method='post'>";
			for (element in data){
				if (element != "id"){
				htmlString += "<label>" + element + "</label>"
						+ "<input name='" + element + "' type='text' value='" + data[element] + "'/> ";
				}
			}
			htmlString += "<input type='submit' call='edit' action='/api/edit' value='Modifier' /></form>";
			$('#details' + data.id).html(htmlString);
		},
		edit : function(data) {
			var form = $("#edit");
			console.log(form);
			console.log(form.parent("div").prev());
			form.parent("div").prev().children().html(form[0]["prenom"].value + " " + form[0]["nom"].value);

		},
		testload : function(data){
			$('#test').html(data);
		}
	};

	return {
		init : function() {
			admin.ecole('/api/ecole');
			$('body').delegate('#annuaire', 'click',function(event) {
				event.preventDefault();
				console.log(event.target);
				if (event.target.getAttribute('call')){
					var call = event.target.getAttribute('call');
					admin[call](
						{url : event.target, id: event.id}
					);
				}

			});
		},
		ecole : function(url) {
			getAndRender(url, 'ecole');
		},
		classes : function(o) {
			getAndRender(o.url, "classes");
		},
		personnes : function(o) {
			getAndRender(o.url, "personnes");
		},
		personne : function(o) {
			getAndRender(o.url, "personne");
		},
		edit : function(o) {
			postAndRender(o.url, 'edit');
		},
		testload : function(o){
			getAndRender(o.url, 'testload');
		}
	}
}();


$(document).ready(function(){
	admin.init(); 
});
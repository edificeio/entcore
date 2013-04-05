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
			var jdata = jQuery.parseJSON(data);
			for (obj in jdata.result){
				htmlString +="<h2>" + jdata.result[obj]["n.ENTStructureNomCourant"] + "</h2>"
					+ "<a call='classes' href='/api/classes?id=" + jdata.result[obj]["n.id"]
					+ "'>Voir les classes</a>"
					+ "<div id='classes-"+ jdata.result[obj]["n.id"] +"'></div>";
			}
			$("#schools").html(htmlString);
		},
		classes: function(data) {
			var htmlString = '';
			var jdata = jQuery.parseJSON(data);
			if (jdata.result != ""){
				if (!!$("#classes-" + jdata.result[0]["n.id"]).children().length) {
					$("#classes-" + jdata.result[0]["n.id"]).html('');
					return;
				}
				for (obj in jdata.result){
					htmlString +="<h4><a>" + jdata.result[obj]['m.ENTGroupeNom'] + "</a></h4>"
						+ "<a call='personnes' href='/api/personnes?id=" + jdata.result[obj]["m.id"].replace(/\$/g, '_').replace(/ /g,'-') 
						+ "'>Voir les élèves</a>"
						+ " - <a href=''>Ajouter un enseignant</a><br />"
						+ "<div id='people-" + jdata.result[obj]["m.id"].replace(/\$/g, '_').replace(/ /g,'-') + "'></div>";
				}
				$("#classes-" + jdata.result[0]["n.id"]).html(htmlString);
			}
		},
		personnes : function(data) {
			var htmlString='<br /><span>';
			var jdata = jQuery.parseJSON(data);
			if (jdata.result != ""){
				if (!!$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).children().length) {
					$('#people-' + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html('');
					return;
				}
				for (obj in jdata.result){
					htmlString +="<a call='personne' href='/api/details?id="
						+ jdata.result[obj]['m.id'] +"'>" + jdata.result[obj]['m.ENTPersonNom']
						+ " " +jdata.result[obj]['m.ENTPersonPrenom'] + "</a> - ";
				}
				htmlString += "</span><div id='details'></div>";
				$("#people-" + jdata.result[0]["n.id"].replace(/\$/g, '_').replace(/ /g,'-')).html(htmlString);
			}
		},
		personne : function(data) {
			if (!!$('#details').children('form').length) {
				$('#details').html('');
				return;
			}
			var jdata = jQuery.parseJSON(data);
			var htmlString = "Nom : " + jdata.result[0]['n.ENTPersonNom']
				+ " - Prénom : " + jdata.result[0]['n.ENTPersonPrenom']
				+ " - Adresse : " + jdata.result[0]['n.ENTPersonAdresse'];
			$('#details').html(htmlString);
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
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
					+ "'>{{#i18n}}directory.admin.classes{{/i18n}}</a> - "
					+ "<a href='/api/export?id=" + jdata.result[obj]["n.id"]
					+ "' call='exportAuth'>{{#i18n}}directory.admin.export{{/i18n}}</a>"
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
						+ "'>{{#i18n}}directory.admin.people{{/i18n}}</a>"
						+ " - <a href='/api/enseignants?id=" + jdata.result[obj]["m.id"].replace(/\$/g, '_').replace(/ /g,'-') + "' call='enseignants'>Ajouter un enseignant</a>" 
						+ " - <a href='/api/export?id=" + jdata.result[obj]["m.id"]
						+ "' call='exportAuth'>{{#i18n}}directory.admin.export{{/i18n}}</a><br />"
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
		exportAuth : function(data) {
			var jdata = jQuery.parseJSON(data);
			var textString = "DONNÉES D'AUTHENTIFICATION\n\n";
			for (obj in jdata.result){
				textString += "----------------------------\n"
					+ "Nom : " + jdata.result[obj]['m.ENTPersonNom'] + "\n"
					+ "Prénom : " + jdata.result[obj]['m.ENTPersonPrenom'] + "\n"
					+ "----------------------------\n";
			}
			document.location = 'data:Application/octet-stream,' + encodeURIComponent(textString);
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
		enseignants : function(o) {
			getAndRender(o.url, "personnes");
		},
		exportAuth : function(o) {
			getAndRender(o.url, "exportAuth");
		}
	}
}();


$(document).ready(function(){
	admin.init(); 
});
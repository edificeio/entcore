function WizardController($scope, $rootScope, model, template, route, date, lang) {
	$scope.template = template;
	$scope.lang = lang;
	$scope.identity = angular.identity;
	$scope.structures =  model.structures;
	$scope.details = { "transition" : false, "preDelete" : false, "filesFormat" : false };
	$scope.disabledButtons = { "validate" : false, "import" : false, "back" : false };
	$scope.wizard = new Wizard();
	$scope.wizard.expectedFields = [];
	$scope.wizard.csvHeader = {};
	$scope.selected = "0";
	$scope.currentProfile = ""; //profile actually mapped
	$scope.mappingAssociation = {}; // association between fields in csv and expected fields, made by user
	$scope.wizard.loadAvailableFeeders(function(conf) {
		if (conf.feeders && conf.feeders.length > 1) {
			$scope.feeders = conf.feeders;
		} else {
			$scope.feeders = ['CSV'];
			$scope.wizard.type = 'CSV';
		}
		$scope.$apply();
	});

    template.open('wizard-container', 'wizard-step1');

	$scope.displayMappingProfile = function(profileName, fileName) {
		$scope.wizard.mapping(profileName, fileName, function(data) {
			$scope.wizard.expectedFields = []; //data.profileFields;
			$scope.wizard.expectedFields2 = [];
			//$scope.wizard.csvHeader = data.csvHeader;
			$scope.wizard.csvHeader = [];

			for (var field in data.csvHeader) {
				$scope.wizard.csvHeader.push({"id" : field, "name" : data.csvHeader[field]});
			}

			$scope.mappingAssociation = {};

			// building list of possible fields for profile, including the indication that the field is required

			//push empty line
			$scope.wizard.expectedFields.push('');
			for (var field in data.profileFields.required) {
				$scope.wizard.expectedFields.push( {"translated":'*' + lang.translate('profile.' + data.profileFields.required[field]),"id":data.profileFields.required[field]});
				$scope.wizard.expectedFields2.push('*' + lang.translate('profile.' + data.profileFields.required[field]));
			}

			/*for (var field in data.profileFields.validate) {
				if( $scope.wizard.expectedFields.indexOf('*' + lang.translate('profile.' + data.profileFields.validate[field])) == -1 ){
					$scope.wizard.expectedFields.push(lang.translate('profile.' + data.profileFields.validate[field]));
				}
			}*/
			for (var field in data.profileFields.validate) {
				if ($scope.wizard.expectedFields2.indexOf('*' + lang.translate('profile.' + field)) == -1) {
					$scope.wizard.expectedFields.push({"translated":lang.translate('profile.' + field), "id":field});
				}
			}

			template.open('wizard-container', 'wizard-mapping');
			$scope.currentProfile = profileName;
			$scope.currentFileName = fileName;
			$scope.$apply();
		});
	};

	$scope.displayMapping = function(data) {
		// TODO : implement for multiple files import
		$scope.profileError = '';
		if (data.errors['error.Student']) {
			$scope.displayMappingProfile('Student', $scope.wizard.Student.name);
		}

		if (data.errors['error.Teacher']) {
			$scope.displayMappingProfile('Teacher', $scope.wizard.Teacher.name);
		}

		if (data.errors['error.Relative']) {
			$scope.displayMappingProfile('Relative', $scope.wizard.Relative.name);
		}

		if (data.errors['error.Personnel']) {
			$scope.displayMappingProfile('Personnel', $scope.wizard.Personnel.name);
		}

		if (data.errors['error.Guest']) {
			$scope.displayMappingProfile('Guest', $scope.wizard.Guest.name);
		}
	};

		/*
		$scope.errors = _.map($scope.errors, function (errors, file) {
			return {"title" : lang.translate(file), "elements" : errors };
		});*/
		//template.open('wizard-container', 'wizard-errors');

	$scope.displayErrors = function(data) {
		if (data.error) {
			$scope.errors = {};
			$scope.errors['error.global'] = [];
			$scope.errors['error.global'].push(lang.translate(data.error));
		} else {
			$scope.errors = data.errors;
		}
		$scope.errors = _.map($scope.errors, function (errors, file) {
			return {"title" : lang.translate(file), "elements" : errors };
		});
		template.open('wizard-container', 'wizard-mapping');
		//template.open('wizard-container', 'wizard-errors');
	};

	$scope.validate = function(wizard) {
		$scope.disabledButtons.validate = true;
		if (wizard.structure) {
			wizard.structureId = wizard.structure.id;
			wizard.structureExternalId = wizard.structure.externalId;
			wizard.structureName = wizard.structure.name;
		} else if ($scope.isADMC()) {
			wizard.structureName = wizard.newStructureName;
			wizard.UAI = wizard.newStructureUAI;
		} else {
			return;
		}
		for (var attr in wizard.files) {
			if (wizard.files[attr].length === 1) {
				if (wizard.type === 'BE1D') {
					wizard[('CSVExtraction-' + attr + '.csv')] = wizard.files[attr][0];
				} else {
					wizard[attr] = wizard.files[attr][0];
				}
			}
		}
		wizard.validate(function(data) {
			console.log(data);
			if (data.error || data.errors) {
				$scope.displayMapping(data);
			} else {
				wizard.valid = true;
				$scope.validatedUsers = [];
            	$scope.userOrder = 'lastName';
            	$scope.userFilter = {};
            	$scope.classes = [];
            	$scope.states = [];
            	$scope.profiles = [];
            	var uniqueClasses = {};
            	var uniqueStates = {};
            	var uniqueProfiles = {};
				for (var attr in data) {
					for (var i = 0; i < data[attr].length; i++) {
						$scope.validatedUsers.push(data[attr][i]);
						if (data[attr][i]['classesStr']) {
							var classesSplit = data[attr][i]['classesStr'].split(', ');
							for (var j = 0; j < classesSplit.length; j++) {
								if (!uniqueClasses[classesSplit[j]]) {
									uniqueClasses[classesSplit[j]] = true;
									$scope.classes.push(classesSplit[j]);
								}
							}
						}
						if (!uniqueStates[data[attr][i]['state']]) {
							uniqueStates[data[attr][i]['state']] = true;
							$scope.states.push(data[attr][i]['state']);
						}
						if (!uniqueProfiles[data[attr][i]['translatedProfile']]) {
							uniqueProfiles[data[attr][i]['translatedProfile']] = true;
							$scope.profiles.push(data[attr][i]['translatedProfile']);
						}
					}
 				}
 				$scope.states = $scope.states.sort();
 				$scope.profiles = $scope.profiles.sort();
 				template.open('wizard-container', 'wizard-step2');
			}
			$scope.disabledButtons.validate = false;
			$scope.$apply();
		});
	};

	$scope.setUserOrder = function(order) {
		$scope.userOrder = $scope.userOrder === order ? '-' + order : order;
	};

	$scope.returnStep1 = function(wizard) {
		if (wizard && wizard.files) {
			for (var attr in wizard.files) {
				if (wizard.files[attr].length === 1) {
					if (wizard.type === 'BE1D') {
						delete wizard[('CSVExtraction-' + attr + '.csv')];
					} else {
						delete wizard[attr];
					}
				}
			}
			delete wizard.files;
			delete wizard.check;
		}
		template.open('wizard-container', 'wizard-step1');
	};

	$scope.nextMappingStep = function(wizard) {
		$scope.wizard.validateMapping($scope.currentProfile, $scope.mappingAssociation, $scope.currentFileName, function(data) {
			if( data.errors ) {
				notify.error(lang.translate(data.errors));
			}
		});
	};

	$scope.updateFieldsAssociation = function( fileIndex, expectedIndex ) {
		$scope.mappingAssociation[fileIndex] = expectedIndex;
	};

	$scope.launchImport = function(wizard) {
		$scope.disabledButtons.import = true;
		$scope.disabledButtons.back = true;
		if (wizard.valid) {
			wizard.import(function(data) {
				if (data.error || data.errors) {
					$scope.displayErrors(data);
				} else {
					template.open('wizard-container', 'wizard-step3');
				}
				$scope.disabledButtons.import = false;
				$scope.disabledButtons.back = false;
				$scope.$apply();
			});
		}
	};

	$scope.close = function() {
		window.location = "/admin";
	};

	$scope.authorizeCreateStructure = function(wizard) {
		return !wizard.structure && $scope.isADMC();
	};

	$scope.enableListFeeders = function() {
		return _.contains($scope.feeders, 'BE1D');
	};

	$scope.isADMC = function() {
		return model.me.functions['SUPER_ADMIN'];
	};

	$scope.clearUserFilter = function(filter) {
		if (!$scope.userFilter[filter]) {
			delete $scope.userFilter[filter];
		}
	};

	$scope.exportCSV = function(wizard) {
		var csvHeader = "";
		var bom = "\ufeff";
		var i18nArray = ["directory.admin.name", "directory.firstName", "directory.admin.profile", "wizard.classes", "wizard.state"];
		for (var i = 0; i < i18nArray.length; i++) {
			if (i > 0) {
				csvHeader += ";";
			}
			csvHeader += lang.translate(i18nArray[i]);
		}
		var csvString = bom + csvHeader + _.map($scope.validatedUsers, function(u) {
			return "\r\n" + u.lastName + ";" + u.firstName + ";" + u.translatedProfile + ";" + u.classesStr + ";" + u.state;
		}).join("");
		ajaxDownload(new Blob([csvString]), wizard.structureName + ".csv");
	};

	var downloadAnchor = null;
	var downloadObjectUrl = null;

	var createDownloadAnchor = function() {
		downloadAnchor = document.createElement('a');
		downloadAnchor.style = "display: none";
		document.body.appendChild(downloadAnchor);
	};

	var ajaxDownload = function(blob, filename) {
		if(window.navigator.msSaveOrOpenBlob) {
			//IE specific
			window.navigator.msSaveOrOpenBlob(blob, filename);
		} else {
			//Other browsers
			if(downloadAnchor === null) {
				createDownloadAnchor();
			}
			if(downloadObjectUrl !== null) {
				window.URL.revokeObjectURL(downloadObjectUrl);
			}
			downloadObjectUrl = window.URL.createObjectURL(blob)
			var anchor = downloadAnchor;
			anchor.href = downloadObjectUrl;
			anchor.download = filename;
			anchor.click();
		}
	};

}

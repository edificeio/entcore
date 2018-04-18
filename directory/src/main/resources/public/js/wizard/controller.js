function WizardController($scope, $rootScope, model, template, route, date, lang) {
	$scope.template = template;
	$scope.lang = lang;
	$scope.identity = angular.identity;
	$scope.structures =  model.structures;
	$scope.details = { "transition" : false, "preDelete" : false, "filesFormat" : false };
	$scope.disabledButtons = { "validate" : false, "import" : false, "back" : false };
	$scope.wizard = new Wizard();
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
		template.open('wizard-container', 'wizard-errors');
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
				$scope.displayErrors(data);
			} else {
				wizard.valid = true;
				$scope.softErrors = {};
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
					if ("softErrors" === attr) {
						$scope.softErrors = _.map(data[attr], function (errors, file) {
							return {"title" : lang.translate('error.' + file), "elements" : errors };
						});
						continue;
					}
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
		window.location = "/directory/admin-console";
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

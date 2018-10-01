function CalendarController($scope, model, template, lang, $rootScope) {
		$scope.template = template;
		$scope.lang = lang;
		$scope.identity = angular.identity;
		$scope.structures =  model.structures;
		$scope.component = new Component();

		$scope.slotProfiles = [];

		$scope.initProfil = function() {
			$scope.createdSlotProfile = {
				name: "",
				schoolId : "",
				slots : []
			};
		};

		$scope.initProfil();

    $scope.currentLeaf = ""
    $scope.getCurrentLeaf = function(){
        return _.findWhere($scope.leafMenu, { name: $scope.currentLeaf })
    }
    $scope.leafMenu = [
        {
            name: "slotprofilTab",
            text: lang.translate("directory.calendarOps"),
            templateName: 'admin-slotprofil-tab',
            onClick: function(){
                $scope.scrollOpts.reset()
                $scope.initExportData()

            },
            onStructureClick: function(structure){
                $scope.structure = structure
                $scope.component.slotprofiles(structure.id, function(){
                    $scope.$apply();
                });
                $scope.selectedStructureSlotProfile = structure;
            }
        }
    ];
    _.forEach($scope.leafMenu, function(leaf){
        var temp = leaf.onClick
        leaf.onClick = function(){
            $scope.currentLeaf = leaf.name
            if(leaf.onStructureClick && $scope.structure){
                leaf.onStructureClick($scope.structure)
            }
            temp()
        }
    })

    $scope.scrollOpts = {
        SCROLL_INCREMENT: 250,
        scrollLimit: 250,
        increment: function(){
            this.scrollLimit = this.scrollLimit + this.SCROLL_INCREMENT
        },
        reset: function(){
            this.scrollLimit = this.SCROLL_INCREMENT
        }
    }

    $scope.filterTopStructures = function(structure){
        return !structure.parents
    };

    $scope.filterChildren = function(struct){
        if(struct === $scope.structure)
            return struct.children

        var parents = []

        var recursivelyTagParents = function(struct){
            if(typeof struct.parents === 'undefined')
                return

            _.forEach(struct.parents, function(p){
                if(parents.indexOf(p) < 0)
                    parents.push(p)
                recursivelyTagParents(p)
            })
        }
        recursivelyTagParents(struct)

        //Prevents infinite loops when parents are contained inside children.
        return _.filter(struct.children, function(child){
            return !child.selected || !_.findWhere(parents, {id: child.id})
        })
    }

    $scope.selectOnly = function(structure, structureList){
        $scope.structures.forEach(function(structure){
            structure.selected = false
        })

        var recursivelySelectParents = function(structure){
            //Prevent infinite loops
            if(structure.selected)
                return;

            structure.selected = true;

            $scope.selectedStructure = structure;

            if(!structure.parents)
                return;

            _.forEach(structure.parents, function(parent){
                var parentLocated = $scope.structures.findWhere({id: parent.id })
                if(parentLocated)
                    recursivelySelectParents(parentLocated)
            })
        }
        recursivelySelectParents(structure)
    }

    $scope.isCentralAdmin = function(){
        return _.findWhere(model.me.functions, {code: "SUPER_ADMIN"}) !== undefined
    }

    $scope.initExportData = function(){
        $scope.exportData = {
            export_mode: "structureId",
            classId : "",
            structureId : "",
            filterFormat: function(format){
                return !format.show || format.show()
            },
            filterProfiles: function(profile){
                var format = this.findFormat(this.params.type)
                return !format.profiles || format.profiles.indexOf(profile) > -1
            },
            findFormat: function(key){
                return _.find($scope.formats, function(item){ return key === item.key })
            },
            onFormatChange: function(){
                var format = this.findFormat(this.params.type)
                if(format.profiles)
                    this.params.profile = format.profiles[0]
            },
            params: {
                type: "",
                profile: ""
            }
        }
    }

    $scope.setShowWhat = function(what){
        $scope.showWhat = what
				$scope.profileSelected = undefined;
				$scope.newSlot = undefined;
    };

    $scope.saveSlotProfile = function() {
        $scope.createdSlotProfile.schoolId = $scope.selectedStructureSlotProfile.id;
        $scope.component.save($scope.createdSlotProfile, function() {
					$scope.initProfil();
					$scope.component.slotprofiles($scope.selectedStructureSlotProfile.id, function() {
						$scope.$apply();
					});
				});
    };

    $scope.updateSlotProfile = function (profile) {
        $scope.profileUpdate = {
            name : profile.name,
            schoolId : profile.schoolId,
            slots : profile.slots,
            _id : profile._id
        };
    }


    $scope.updateSlotProfileSave = function () {
        $scope.component.updateSlotProfile($scope.profileUpdate, function () {
            $scope.component.slotprofiles ($scope.profileUpdate.schoolId, function (data) {
                $scope.component.profilesList = data;
                $scope.profileSelected = $scope.profileUpdate;
                $scope.profileUpdate = undefined;
                $scope.$apply();
            });
        });
    }

    $scope.updateSlotProfileCancel = function () {
        $scope.profileUpdate = undefined;
    }

    $scope.showProfilDetails = function(profile) {
        $scope.showWhat = undefined;
        $scope.profileSelected = profile;
        if($scope.profileSelected.slots.length === 0) {
            $scope.newSlot = {
                name: "",
                startHour : moment(),
                endHour : moment().add(1, 'hour')
            };
        } else {
            $scope.newSlot = undefined;
        }
    };

    $scope.addSequenceSlotProfil = function() {
        $scope.selectedSlot = undefined;
        $scope.newSlot = {
            name: "",
            startHour : moment(),
            endHour : moment().add(1, 'hour')
        };
    }
    $scope.cancelAddSlotProfil = function() {
        $scope.newSlot = undefined;
    };

    $scope.saveSlot = function() {
        var newSlotObj = {
            name : $scope.newSlot.name,
            startHour : $scope.newSlot.startHour.format('LT'),
            endHour : $scope.newSlot.endHour.format('LT')
        }
        $scope.component.saveSlot($scope.profileSelected._id, newSlotObj, function() {
            $scope.component.slots ($scope.profileSelected._id, function(data) {
                $scope.profileSelected.slots = data.slots;
                $scope.addSequenceSlotProfil();
                $scope.$apply();
            });
        });
    }

    $scope.updateSlot = function(slot) {
        $scope.newSlot = undefined;
        $scope.selectedSlot = {
            name : slot.name,
            startHour : slot.startHour,
            endHour : slot.endHour,
            id : slot.id
        };
        $scope.oldSlot = slot;
        var heureDebut = slot.startHour.split(":")[0];
        var minuteDebut = slot.startHour.split(":")[1];
        $scope.selectedSlot.startHour = moment();
        $scope.selectedSlot.startHour.hour(heureDebut);
        $scope.selectedSlot.startHour.minute(minuteDebut);

        var heureFin = slot.endHour.split(":")[0];
        var minuteFin = slot.endHour.split(":")[1];
        $scope.selectedSlot.endHour = moment();
        $scope.selectedSlot.endHour.hour(heureFin);
        $scope.selectedSlot.endHour.minute(minuteFin);
    }

    $scope.updateSlotSave = function (slot) {
        var updateSlotObj = {
            name : slot.name,
            startHour : slot.startHour.format('LT'),
            endHour : slot.endHour.format('LT'),
            id : slot.id
        }
        $scope.selectedSlot = undefined;
        $scope.component.updateSlot ($scope.profileSelected._id, updateSlotObj, function () {
            $scope.removeSlot ($scope.oldSlot);
            $scope.profileSelected.slots.push(updateSlotObj);
            $scope.oldSlot = undefined;
            $scope.$apply();
        });
    }


    $scope.updateSlotCancel = function () {
        $scope.selectedSlot = undefined;
    }

    $scope.deleteSlot = function (slotProfileId, slot) {
        $scope.component.deleteSlot (slotProfileId, slot.id, function() {
            $scope.removeSlot (slot);
            $scope.$apply();
        });
    }

    $scope.removeSlot = function(slot) {
        var index = $scope.profileSelected.slots.indexOf(slot);
        $scope.profileSelected.slots.splice(index, 1);
    }

    $scope.themes = [
        {
            name: "pink",
            path: "default"
        },
        {
            name: "orange",
            path: "orange"
        },
        {
            name: "blue",
            path: "blue"
        },
        {
            name: "purple",
            path: "purple"
        },
        {
            name: "red",
            path: "red"
        },
        {
            name: "green",
            path: "green"
        },
        {
            name: "grey",
            path: "grey"
        }
    ]
    $scope.setTheme = function(theme){
        ui.setStyle('/public/admin/'+theme.path+'/')
        http().putJson('/userbook/preference/admin', {
            name: theme.name,
            path: theme.path
        })
    }
}

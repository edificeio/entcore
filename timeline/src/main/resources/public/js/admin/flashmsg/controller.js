function AdminFlashMsgController($scope) {

    $scope.lang = lang
    $scope.messages = model.flashMsgs
    $scope.languages = model.languages
    $scope.edited = model.edited
    model.syncLanguages()

    if(!$.spectrum){
        http().get('/infra/public/spectrum/spectrum.js').done(function(data){
            eval(data);
            var stylesheet = $('<link rel="stylesheet" type="text/css" href="/infra/public/spectrum/spectrum.css" />');
            $('head').prepend(stylesheet);
        })
    }
    $scope.setSpectrum = function(){
        var inputColors = $('input[type=color]')
        inputColors.each(function(index, input){
            if(input.type === "text"){
                $(input).spectrum({
                    color: $scope.edited.message.customColor,
                    move: function(color) {
                        $scope.edited.message.customColor = color.toHexString()
                        $scope.edited.message.color = null
                        $scope.$apply()
                    },
                    change: function(color) {
                        $scope.edited.message.customColor = color.toHexString()
                        $scope.edited.message.color = null
                        $scope.$apply()
                    }
                })
            }
        })
    }

    $scope.formatDate = function(date) {
        return moment(date).format('L')
    }
    $scope.formatEndDate = function(message){
        message.endDate = moment(message.endDate).endOf('day').toDate()
    }
    $scope.formatContent = function(lang, contents) {
        return $(contents[currentLanguage]).text()
    }

    $scope.changeFilters = function($timeout) {
        $scope.filters.none = _.all(_.pairs($scope.filters), function(p) {
            if(p[0] !== 'none')
                return p[1]
            else
                return true
         })
         if($scope.filters.none) {
            $scope.initFilters()
         }
    }
    $scope.initFilters = function() {
        $scope.filters = {
            none: true,
            current: false,
            pending: false,
            obsolete: false
        }
    }
    $scope.initFilters()
    $scope.filterList = function(item) {
        var now = new Date()

        var noneClause = $scope.filters.none
        var currentClause = $scope.filters.current &&
            item.startDate <= now && item.endDate > now
        var pendingClause = $scope.filters.pending &&
            item.startDate > now
        var obsoleteClause = $scope.filters.obsolete &&
            item.endDate < now

        return noneClause || currentClause || pendingClause || obsoleteClause
    }
    $scope.orderBy = function(order) {
        if($scope.orderList === order) {
            $scope.orderList = '-' + order
        } else {
            $scope.orderList = order
        }
    }
    $scope.selection = {
        filteredMessages: [],
        all: false,
        toggleAll: function() {
            if(this.all) {
                $scope.messages.deselectAll()
                this.filteredMessages.forEach(function(mess) {
                    mess.selected = true
                })
            } else {
                $scope.messages.deselectAll()
            }
        }
    }

    template.open('main', 'flashmsg/admin.list')

    $scope.createMessage = function() {
        $scope.edited.message = new FlashMsg()
        $scope.profiles.selected = []
        template.open('main', 'flashmsg/admin.edit')
    }
    $scope.editMessage = function(message) {
        $scope.edited.message = message
        $scope.profiles.initSelected()
        template.open('main', 'flashmsg/admin.edit')
    }
    $scope.cancelEdit = function() {
        template.open('main', 'flashmsg/admin.list')
    }

    $scope.profiles = {
        profileList: [ 'Teacher', 'Student', 'Relative', 'Personnel', 'Guest' ],
        list: [],
        selected: [],
        deselect: function(item) {
            this.selected = this.selected.filter(function(i){ return i !== item })
            this.modify()
        },
        modify: function() {
            $scope.edited.message.profiles = _.map($scope.profiles.selected, function(p){ return p.value })
        },
        initProfiles: function() {
            this.profileList.forEach(function(profile) {
                $scope.profiles.list.push({
                    toString: function(){ return lang.translate(profile) },
                    name: lang.translate(profile),
                    value: profile
                })
            })
        },
        initSelected: function() {
            this.selected = _.map($scope.edited.message.profiles, function(profile) {
                return _.findWhere($scope.profiles.list, { value: profile })
            })
        }
    }
    $scope.profiles.initProfiles()

    $scope.comboLabels = {
		options: lang.translate('options'),
		searchPlaceholder: lang.translate('search'),
		selectAll: lang.translate('select.all'),
		deselectAll: lang.translate('deselect.all')
	}

    $scope.banner = {
        colors: ['red', 'orange', 'green', 'blue']
    }
    $scope.setColor = function(color) {
        $scope.edited.message.color = color
        $scope.edited.message.customColor = null
    }

    $scope.validateMessage = function(message) {
        var checks = [
            {
                check : function() {
                    return message.title &&
                        message.contents &&
                        _.any(message.contents, function(item){
                            return item.trim()
                        }) &&
                        message.title.trim()
                },
                message: "missing.or.empty.required.field"
            },
            {
                check: function(){ return (message.color || message.customColor) },
                message: "missing.color"
            },
            {
                check: function() {
                    return message.profiles &&
                        message.profiles.length > 0
                },
                message: "missing.profiles"
            },
            {
                check: function() {
                    return message.startDate &&
                        message.endDate &&
                        message.endDate > message.startDate
                },
                message: "endDate.lower.than.startDate"
            }
        ]
        for(var i = 0; i < checks.length; i++) {
            if(!checks[i].check()) {
                $scope.validateError = checks[i].message
                return false
            }
        }
        return true
    }

    $scope.refreshMessages = function() {
        $scope.messages.sync()
        $scope.messages.one('sync', $scope.$apply)
    }

    $scope.saveMessage = function(message) {
        message.save()
        if(!message.id) {
            message.one('change',$scope.refreshMessages)
        }
        template.open('main', 'flashmsg/admin.list')
    }

    $scope.deleteMessages = function(messages) {
        $scope.messages.delete(messages).done(function() {
            $scope.refreshMessages()
        })
    }

    $scope.duplicateMessage = function(message) {
        var duplicate = new FlashMsg()
        duplicate.contents = message.contents
        duplicate.title = message.title + lang.translate('timeline.flashmsg.duplicate.append.title')
        duplicate.color = message.color
        duplicate.customColor = message.customColor
        message.selected = false
        $scope.editMessage(duplicate)
        // message.duplicate(function(duplicateMessage) {
        //     $scope.refreshMessages()
        // })
    }
}

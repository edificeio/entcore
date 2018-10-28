function EmbedController($scope) {
    $scope.defaultEmbeds = model.defaultEmbeds
    $scope.customEmbeds = model.customEmbeds

    /////// TOP NOTIFICATIONS ///////
    $scope.topNotification = {
        show: false,
        message: "",
        confirm: null,
        additional: []
    }
    $scope.notifyTop = function(text, action){
        $scope.topNotification.message = "<p>"+text+"</p>"
        $scope.topNotification.confirm = action
        $scope.topNotification.show = true
    }
    $scope.colourText = function(text){
        return '<span class="colored">'+text+'</span>'
    }

    $scope.newEmbed = function() {
        $scope.embed = new CustomEmbed()
    }

    $scope.viewEmbed = function(embed) {
        $scope.embed = embed
	    if($scope.embed.url.constructor === Array) {
            $scope.embed.url = $scope.embed.url.join(" , ");
        }
    }

    $scope.mergeEmbeds = function() {
        return $scope.defaultEmbeds.all.concat($scope.customEmbeds.all)
    }

    $scope.validateEmbed = function() {
        return $scope.embed && $scope.embed.name && $scope.embed.displayName &&
                $scope.embed.url && $scope.embed.logo && $scope.embed.embed &&
                $scope.embed.example
    }

    $scope.loadLogo = function(inputElt) {
        if(inputElt.files.length > 0){
            var img = new Image;
            var canvas = document.createElement('canvas')
            var stepCanvas = document.createElement('canvas')
            canvas.width = 100
            canvas.height = 100
            var ctx = canvas.getContext("2d")
            var sCtx = stepCanvas.getContext("2d")

            img.onload = function() {
                var scale = 100 / img.height
                if(scale < 1 && img.height > 200) {
                    stepCanvas.width = img.width / 2
                    stepCanvas.height = img.height / 2
                    sCtx.drawImage(img, 0, 0, img.width / 2, img.height / 2)
                    ctx.drawImage(stepCanvas, 0, 0, scale * img.width, 100)
                } else {
                    ctx.drawImage(img, 0, 0, scale * img.width, 100)
                }
                $scope.embed.logo = canvas.toDataURL()
                $scope.$apply()
            }

            img.src = URL.createObjectURL(inputElt.files[0])
        }
    }

    $scope.save = function() {
	    $scope.embed.url = $scope.embed.url.split(" , ");
        var fresh = !$scope.embed._id
        $scope.embed.save().done(function(data) {
            if(fresh) {
                $scope.embed._id = data._id
                $scope.customEmbeds.push($scope.embed)
                $scope.newEmbed()
                $scope.$apply()
                notify.info('embed.creation.notify')
            } else {
		        $scope.embed.url = $scope.embed.url.join(" , ")
		        $scope.$apply()
                notify.info('embed.modification.notify')
            }
        })
    }

    $scope.delete = function() {
        var target = $scope.embed
        var action = function() {
            target.remove().done(function() {
                if($scope.embed === target) {
                    $scope.newEmbed()
                }
                $scope.customEmbeds.all = $scope.customEmbeds.reject(function(e){
                    return e._id === target._id
                })
                $scope.$apply()
            })
        }
        $scope.notifyTop(lang.translate('embed.confirm.deletion') + ' ' + $scope.colourText($scope.embed.displayName) + '.', action)
    }

    $scope.newEmbed()
}
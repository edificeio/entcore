function EmbedController($scope) {
    $scope.defaultEmbeds = model.defaultEmbeds
    $scope.customEmbeds = model.customEmbeds

    $scope.newEmbed = function() {
        $scope.embed = new CustomEmbed()
    }

    $scope.viewEmbed = function(embed) {
        $scope.embed = embed
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
        var fresh = !$scope.embed._id
        $scope.embed.save().done(function(data) {
            if(fresh) {
                $scope.embed._id = data._id
                $scope.customEmbeds.push($scope.embed)
                $scope.newEmbed()
                $scope.$apply()
            }
        })
    }

    $scope.delete = function() {
        var id = $scope.embed._id
        $scope.embed.remove().done(function() {
            $scope.newEmbed()
            $scope.customEmbeds.all = $scope.customEmbeds.reject(function(e){
                return e._id === id
            })
            $scope.$apply()
        })
    }

    $scope.newEmbed()
}

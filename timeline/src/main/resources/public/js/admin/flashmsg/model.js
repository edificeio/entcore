function FlashMsg(){
    var that = this
    this.contents = this.contents || {}
    model.languages.forEach(function(language){
        if(!that.contents[language])
            that.contents[language] = ""
    })
    this.startDate = this.startDate ? moment(this.startDate).toDate() : new Date()
    this.endDate = this.endDate ? moment(this.endDate).toDate() : moment(new Date()).endOf('day').toDate()
    this.lang = this.lang || currentLanguage
    this.author = this.author || ""
    this.lastModifier = this.lastModifier || ""
}
FlashMsg.prototype.api = {
    post:   "/timeline/flashmsg",
    put:    "/timeline/flashmsg/:id",
    delete: "/timeline/flashmsg/:id"
}
FlashMsg.prototype.duplicate = function(cb){
    http().put("/timeline/flashmsg/" + this.id + "/duplicate").done(function(data){
        var duplicate = new FlashMsg()
        duplicate.updateData(data)
        if(cb)
            cb(duplicate)
    })
}
FlashMsg.prototype.toJSON = function(){
    return {
        title :         this.title,
        startDate:      this.startDate,
        endDate:        this.endDate,
        profiles:       this.profiles,
        color:          this.color,
        customColor:    this.customColor,
        contents:       this.contents
    }
}

model.build = function () {
    model.makeModel(FlashMsg)

    RTE.baseToolbarConf.options.all = _.filter(RTE.baseToolbarConf.options.all, function(option){
        var keep = [
            "undo",
            "redo",
            "bold",
            "italic",
            "underline",
            "justifyLeft",
            "justifyRight",
            "justifyCenter",
            "justifyFull",
            "font",
            "fontSize",
            "color",
            "linker",
            "unlink" ]

        return keep.indexOf(option.name) > -1
    })

    this.edited = {}
    this.languages = []
    this.syncLanguages = function(){
        http().get('/languages').done(function (langs) {
            langs.forEach(function(lang){
                this.languages.push(lang)
            }.bind(this))
        }.bind(this))
    }

    this.collection(FlashMsg, {
        sync: "/timeline/flashmsg/listadmin",
        delete: function(flashMsgs) {
            if(flashMsgs.length < 1){
                return
            }
            var queryString = ""
            flashMsgs.forEach(function(msg){
                if(!queryString)
                    queryString += "?id=" + msg.id
                else
                    queryString += "&id=" + msg.id
            })
            return http().delete("/timeline/flashmsg" + queryString)
        }
    })
}

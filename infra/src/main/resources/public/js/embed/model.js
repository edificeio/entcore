function DefaultEmbed() {
    this.default = true
}
function CustomEmbed() {}
CustomEmbed.prototype.save = function(cb) {
    if(this._id) {
        return http().putJson("/infra/embed/custom/" + this._id, this)
    }
    else{
        return http().postJson("/infra/embed/custom", this)
    }
}
CustomEmbed.prototype.remove = function(cb) {
    return http().delete("/infra/embed/custom/" + this._id)
}
CustomEmbed.prototype.toJSON = function() {
    return {
        name: this.name,
        displayName: this.displayName,
        url: this.url,
        logo: this.logo,
        embed: this.embed,
        example: this.example
    }
}

model.build = function () {
    model.makeModels([DefaultEmbed, CustomEmbed])

    this.collection(DefaultEmbed, {
        sync: "/infra/embed/default"
    })
    this.collection(CustomEmbed, {
        sync: "/infra/embed/custom"
    })
}

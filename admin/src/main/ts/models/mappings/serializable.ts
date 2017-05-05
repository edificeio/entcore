export abstract class Serializable {

    protected abstract _json_fields : string[]

    toJSON() {
        let json = {}
        for(let i = 0; i < this._json_fields.length; i++) {
            json[this._json_fields[i]] = this[this._json_fields[i]]
        }
        return json
    }

}
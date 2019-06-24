import { Model } from 'entcore-toolkit';
import { UserModel } from './user.model';

export class FlashMessageModel extends Model<FlashMessageModel> {



    _id?: string;
    title?: string;
    contents?: Object;
    startDate?: string;
    endDate?: string;
    readCount?: number;
    author?: string;
    profiles?: string[];
    _color?: string;
    _customColor?: string;
    lastModifier?: string;
    structureId?: string;

    subStructures?: string[];

    constructor() {
        super({});
        this.profiles = [];
        this.subStructures = [];
        this.contents = {};
    }

    get id(){ return this._id };
    set id(id) {
        this._id = id;
    };

    get color() { return this._color }
    set color(color: string) {
        this._customColor = null;
        this._color = color;
    }

    get customColor() { return this._customColor }
    set customColor(customColor: string) {
        this._color = null;
        this._customColor = customColor;
    }

}

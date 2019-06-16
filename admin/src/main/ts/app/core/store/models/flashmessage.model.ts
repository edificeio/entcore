import { Model } from 'entcore-toolkit';
import { UserModel } from './user.model';

export class FlashMessageModel extends Model<FlashMessageModel> {



    _id?: string;
    title?: string;
    contents?: string;
    startDate?: string;
    endDate?: string;
    readCount?: number;
    author?: string;
    profiles?: string[];
    color?: string;
    customColor?: string;
    lastModifier?: string;
    structureId?: string;

    constructor() {
        super({});
        this.profiles = [];
    }

    get id(){ return this._id };
    set id(id) {
        this._id = id;
    };

}

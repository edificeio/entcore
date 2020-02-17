import {Model} from 'entcore-toolkit';

export class SubjectModel extends Model<SubjectModel> {

    id?: string;
    label?: string;
    code?: string;
    structureId?: string;

    constructor() {
        super({});
    }

    toJSON() {
        return {
            id: this.id,
            label: this.label,
            code: this.code,
            structureId: this.structureId
        };
    }
}
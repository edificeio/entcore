import { AbstractStore, FlashMessageModel, StructureModel } from '../../core/store';

export class MessageFlashStore extends AbstractStore {

    constructor() {
        super(['structure', 'messages']);
    }

    structure: StructureModel;
    messages: FlashMessageModel[];

}
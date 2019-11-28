import { AbstractStore } from 'src/app/core/store/abstract.store';
import { StructureModel } from 'src/app/core/store/models/structure.model';
import { FlashMessageModel } from 'src/app/core/store/models/flashmessage.model';

export class MessageFlashStore extends AbstractStore {

    constructor() {
        super(['structure', 'messages']);
    }

    structure: StructureModel;
    messages: FlashMessageModel[];

}

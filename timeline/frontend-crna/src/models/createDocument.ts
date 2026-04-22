export type LoolDocTypeId = 'word' | 'powerpoint' | 'excel';

export interface LoolDocType {
  id: LoolDocTypeId;
  extension: 'docx' | 'pptx' | 'xlsx';
  label: string;
}

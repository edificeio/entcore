export type DocTypeId = 'word' | 'powerpoint' | 'excel';

export interface DocType {
  id: DocTypeId;
  extension: 'docx' | 'pptx' | 'xlsx';
  label: string;
}

export const DOC_TYPES: DocType[] = [
  { id: 'word', extension: 'docx', label: 'Document Texte' },
  { id: 'powerpoint', extension: 'pptx', label: 'Présentation' },
  { id: 'excel', extension: 'xlsx', label: 'Classeur' },
];

import { odeServices } from '@edifice.io/client';
import { useQuery } from '@tanstack/react-query';

export type LoolDocTypeId = 'word' | 'powerpoint' | 'excel';

export interface LoolDocType {
  id: LoolDocTypeId;
  extension: 'docx' | 'pptx' | 'xlsx';
  label: string;
}

interface RawProviderContext {
  provider: string;
  capabilities: Array<{ 'content-type': string; extension: string }>;
  templates: string[];
}

const TEMPLATE_TO_DOC_TYPE: Record<string, LoolDocType> = {
  docx: { id: 'word',        extension: 'docx', label: 'Document Texte' },
  pptx: { id: 'powerpoint',  extension: 'pptx', label: 'Présentation'   },
  xlsx: { id: 'excel',       extension: 'xlsx', label: 'Classeur'        },
};

function transformProviderContext(raw: RawProviderContext): LoolDocType[] {
  return raw.templates
    .map((tpl) => TEMPLATE_TO_DOC_TYPE[tpl])
    .filter(Boolean);
}

export function useLoolProviders() {
  return useQuery<RawProviderContext, Error, LoolDocType[]>({
    queryKey: ['lool-providers'],
    queryFn: () => odeServices.http().get<RawProviderContext>('/lool/providers/context'),
    select: transformProviderContext,
    staleTime: Infinity,
  });
}

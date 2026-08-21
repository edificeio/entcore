import { useLanguages } from '~/services/queries/customize';

export function useCustomizeForm() {
  const languagesQuery = useLanguages();

  return {
    languages: languagesQuery.data,
  };
}

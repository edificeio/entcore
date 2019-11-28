import { removeAccents } from './accents.helper';
export { removeAccents } from './accents.helper';

export function standardise(str: string): string
{
  return removeAccents(str != null ? str : "").toLowerCase().replace("\\s+", " ");
}
import type { Structure } from '~/models/carnetDeBord';

export type { Structure };

export async function fetchCarnetDeBord(): Promise<Structure[]> {
  const response = await fetch("/sso/pronote");
  if (!response.ok) throw new Error("carnet-de-bord.widget.pronote.access.error");
  const data = await response.json();
  if (!Array.isArray(data)) throw new Error("carnet-de-bord.widget.nodata");
  return data;
}

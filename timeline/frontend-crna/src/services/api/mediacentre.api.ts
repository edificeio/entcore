import { odeServices } from '@edifice.io/client';
import type { ListWidgetItem } from '~/models';
import type { MediacentreFavoritesResponse, MediacentreSignet } from '~/models/mediacentre';

function mapSignetToItem(signet: MediacentreSignet): ListWidgetItem {
  return {
    id: signet._id,
    label: signet.title,
    sublabel: signet.plain_text,
    href: signet.link,
    imageUrl: signet.image,
  };
}

function mapPinToItem(signet: MediacentreSignet): ListWidgetItem {
  return {
    id: signet._id,
    label: signet.pinned_title || signet.title,
    sublabel: signet.pinned_description || signet.plain_text,
    href: signet.link,
    imageUrl: signet.image,
  };
}

export async function fetchMediacentre(): Promise<ListWidgetItem[]> {
  const body = await odeServices.http().get<MediacentreFavoritesResponse>('/mediacentre/favorites');
  if (body.status !== 'ok' || !Array.isArray(body.data)) {
    throw new Error('mediacentre.widget.fetch.error');
  }
  return body.data.map(mapSignetToItem);
}

export async function fetchMediacentrePins(structureId: string): Promise<ListWidgetItem[]> {
  const body = await odeServices.http().get<MediacentreSignet[] | MediacentreFavoritesResponse>(
    `/mediacentre/structures/${structureId}/pins`
  );
  const data = Array.isArray(body) ? body : (body as MediacentreFavoritesResponse).data;
  if (!Array.isArray(data)) throw new Error('mediacentre.widget.pins.fetch.error');
  return data.map(mapPinToItem);
}

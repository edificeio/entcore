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

export async function fetchMediacentre(): Promise<ListWidgetItem[]> {
  const body = await odeServices.http().get<MediacentreFavoritesResponse>('/mediacentre/favorites');
  if (body.status !== 'ok' || !Array.isArray(body.data)) {
    throw new Error('mediacentre.widget.fetch.error');
  }
  return body.data.map(mapSignetToItem);
}

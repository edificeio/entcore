import { odeServices } from '@edifice.io/client';
import type { ListWidgetItem } from '~/models';
import type {
  MediacentreFavoritesResponse,
  MediacentreSignet,
} from '~/models/mediacentre';

function mapSignetToItem(signet: MediacentreSignet): ListWidgetItem {
  return {
    id: signet._id,
    label: signet.title,
    sublabel: signet.plain_text,
    href: signet.link || signet.url,
    imageUrl: signet.image,
  };
}

function mapPinToItem(signet: MediacentreSignet): ListWidgetItem {
  return {
    id: signet._id,
    label: signet.pinned_title || signet.title,
    sublabel: signet.pinned_description || signet.plain_text,
    href: signet.link || signet.url,
    imageUrl: signet.image,
  };
}

export async function fetchMediacentre(): Promise<ListWidgetItem[]> {
  const body = await odeServices
    .http()
    .get<MediacentreFavoritesResponse>('/mediacentre/favorites');
  if (body.status !== 'ok') {
    throw new Error('mediacentre.widget.fetch.error');
  }
  // A user who never selected a favorite yet gets `data: {}` (state: "initialization")
  // instead of an empty array — that's an empty list, not a connection error.
  if (!Array.isArray(body.data)) {
    return [];
  }
  return body.data.map(mapSignetToItem);
}

export async function fetchMediacentrePins(
  structureId: string,
): Promise<ListWidgetItem[]> {
  const body = await odeServices
    .http()
    .get<
      MediacentreSignet[] | MediacentreFavoritesResponse
    >(`/mediacentre/structures/${structureId}/pins`);
  if (Array.isArray(body)) {
    return body.map(mapPinToItem);
  }
  if (body.status !== 'ok') {
    throw new Error('mediacentre.widget.pins.fetch.error');
  }
  if (!Array.isArray(body.data)) {
    return [];
  }
  return body.data.map(mapPinToItem);
}

export async function fetchMediacentreHasUniversalis(): Promise<boolean> {
  try {
    const resource = await odeServices
      .http()
      .get('/mediacentre/resource/universalis');
    return !!resource;
  } catch {
    return false;
  }
}

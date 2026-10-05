import { odeServices } from '@edifice.io/client';
import type { LinkItem } from '~/models';
import type {
  MediacentreFavoritesResponse,
  MediacentrePublishedSignet,
  MediacentrePublishedSignetsResponse,
  MediacentreSignet,
} from '~/models/mediacentre';

const ORIENTATION_DOCUMENT_TYPE = 'orientation';

function mapSignetToItem(signet: MediacentreSignet): LinkItem {
  return {
    id: signet._id,
    label: signet.title,
    sublabel: signet.plain_text,
    // Mediacentre always returns one of them (`url` for personal signets).
    href: signet.link || signet.url || '',
    imageUrl: signet.image,
  };
}

function mapPinToItem(signet: MediacentreSignet): LinkItem {
  return {
    id: signet._id,
    label: signet.pinned_title || signet.title,
    sublabel: signet.pinned_description || signet.plain_text,
    // Mediacentre always returns one of them (`url` for personal signets).
    href: signet.link || signet.url || '',
    imageUrl: signet.image,
  };
}

function mapPublishedSignetToItem(
  signet: MediacentrePublishedSignet,
): LinkItem {
  return {
    id: signet.id,
    label: signet.title,
    sublabel: signet.plain_text.join(', '),
    href: signet.link,
    imageUrl: signet.image,
  };
}

// Same rule as the Mediacentre app's "Orientation" theme filter.
function isOrientationSignet(signet: MediacentrePublishedSignet): boolean {
  return signet.document_types.some((type) =>
    type.toLowerCase().includes(ORIENTATION_DOCUMENT_TYPE),
  );
}

export async function fetchMediacentre(): Promise<LinkItem[]> {
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
): Promise<LinkItem[]> {
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

export async function fetchMediacentreOrientation(): Promise<LinkItem[]> {
  const body = await odeServices
    .http()
    .get<MediacentrePublishedSignetsResponse>('/mediacentre/signets');
  if (body.status !== 'ok') {
    throw new Error('mediacentre.widget.orientation.fetch.error');
  }
  const signets = body.data?.signets?.resources ?? [];
  return signets.filter(isOrientationSignet).map(mapPublishedSignetToItem);
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

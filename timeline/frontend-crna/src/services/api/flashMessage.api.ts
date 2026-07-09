import { odeServices } from '@edifice.io/client';
import type { IFlashMessageModel } from '@edifice.io/client';

export async function fetchFlashMessageHistory(): Promise<IFlashMessageModel[]> {
  return odeServices
    .http()
    .get<IFlashMessageModel[]>('/timeline/flashmsg/listuser?includeRead=true');
}

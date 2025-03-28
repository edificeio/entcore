import { useRouteLoaderData } from 'react-router-dom';
import { RootLoaderData } from '~/routes/root';

export function useConfig() {
  const { config } = useRouteLoaderData('root') as RootLoaderData;
  return config ?? { maxDepth: 2, recallDelayMinutes: 60 };
}

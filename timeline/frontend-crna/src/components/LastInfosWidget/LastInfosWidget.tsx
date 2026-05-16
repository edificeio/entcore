import { LastInfosList } from '@edifice.io/react/homepage';
import type { LastInfosProps } from '@edifice.io/react/homepage';
import { WidgetSkeleton } from '../WidgetSkeleton';

interface Props {
  infos: LastInfosProps[];
  isLoading: boolean;
}

export function LastInfosWidget({ infos, isLoading }: Props) {
  if (isLoading) return <WidgetSkeleton />;

  return <LastInfosList infos={infos} />;
}

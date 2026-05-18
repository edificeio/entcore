import { LastInfosList } from '@edifice.io/react/homepage';
import { useLastInfos } from '~/hooks/useLastInfos';
import { WidgetSkeleton } from '../ui/WidgetSkeleton';

export function LastInfosWidget() {
  const { data: infos = [], isLoading } = useLastInfos();

  if (isLoading) return <WidgetSkeleton />;

  return <LastInfosList infos={infos} />;
}

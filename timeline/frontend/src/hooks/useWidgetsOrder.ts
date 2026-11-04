import { useQuery } from '@tanstack/react-query';
import widgetsDefaultOrder from '~/config/widgets.json';
import { WidgetOrderEntry, widgetsOrderService } from '~/services';

export const WIDGETS_ORDER_QUERY_KEY = ['widgets', 'order'];

/**
 * Display order of the homepage widgets: the project one
 * (`assets/widgets/widgets-order.json`), or the embedded default
 * (`config/widgets.json`) when the project provides none.
 */
export function useWidgetsOrder(): {
  order: WidgetOrderEntry[];
  isLoading: boolean;
} {
  const { data, isLoading } = useQuery({
    queryKey: WIDGETS_ORDER_QUERY_KEY,
    queryFn: widgetsOrderService.getOrder,
    // Static file, only changed by a deployment
    staleTime: Infinity,
  });

  return {
    order: data ?? (widgetsDefaultOrder as WidgetOrderEntry[]),
    isLoading,
  };
}

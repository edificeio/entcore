import { InvalidateQueryFilters, QueryClient } from '@tanstack/react-query';

export function invalidateQueriesWithFirstPage(
  queryClient: QueryClient,
  options: InvalidateQueryFilters,
) {
  if (!options.queryKey) return;

  queryClient.setQueriesData({ queryKey: options.queryKey }, (oldData: any) => {
    if (!oldData?.pages) return oldData;
    return {
      ...oldData,
      pages: [oldData.pages[0]],
      pageParams: [oldData.pageParams[0]],
    };
  });

  return queryClient.invalidateQueries(options);
}

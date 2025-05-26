import { IAction, odeServices } from '@edifice.io/client';
import { queryOptions } from '@tanstack/react-query';

/**
 * actionsQueryOptions: check action availability depending on workflow right
 * @param actions (expects an array of actions)
 * @returns queryOptions with key, fn, and selected data
 */
export const actionsQueryOptions = (actions: IAction[]) => {
  /** we get a new array with all workflow */
  const workflows = actions.map((action) => action.workflow);
  /** we remove duplicate workflows */
  const actionsWorkflows = new Set(workflows);
  return queryOptions({
    queryKey: [...actionsWorkflows],
    queryFn: async () =>
      await odeServices
        .rights()
        .sessionHasWorkflowRights([...actionsWorkflows]),
    select: (data: Record<string, boolean>) => {
      return actions
        .filter((action: IAction) => data[action.workflow])
        .map((action) => ({
          ...action,
          available: true,
        })) as IAction[];
    },
    staleTime: Infinity,
  });
};

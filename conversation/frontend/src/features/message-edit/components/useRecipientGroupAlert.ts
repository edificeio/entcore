import { Group, Recipients } from '~/models';

export interface RecipientGroupAlertProps {
  to: Recipients;
  cc: Recipients;
}

export function useRecipientGroupAlert({ to, cc }: RecipientGroupAlertProps) {
  const alertOnGroupsFilter = (group: Group) =>
    group.subType &&
    group.type == 'ProfileGroup' &&
    ['Student', 'Relative', 'ClassGroup'].includes(group.subType);

  const hasAlertOnGroups =
    to.groups.findIndex(alertOnGroupsFilter) >= 0 ||
    cc.groups.findIndex(alertOnGroupsFilter) >= 0;

  return { hasAlertOnGroups };
}

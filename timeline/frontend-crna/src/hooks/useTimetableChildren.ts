import { useDirectory, useUser } from '@edifice.io/react';
import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import type { Child } from '~/models/child';
import { edtChildrenQueryOptions } from '~/services/queries/edt.queries';

export interface UseTimetableChildrenResult {
  children: Child[];
  isLoading: boolean;
}

/**
 * `/edt/user/children` only exists for "Relative" (parent) profiles. A
 * student viewing their own timetable has no "children" to select — they
 * *are* the child — so we build a single self-referencing entry from their
 * own session instead of calling that endpoint.
 */
export const useTimetableChildren = (
  enabled: boolean,
): UseTimetableChildrenResult => {
  const { user } = useUser();
  const isRelative = user?.type === 'PERSRELELEVE';
  const isStudent = user?.type === 'ELEVE';

  const { data, isLoading } = useQuery({
    ...edtChildrenQueryOptions,
    enabled: enabled && isRelative,
  });
  const { getAvatarURL } = useDirectory();

  const children = useMemo<Child[]>(() => {
    if (isStudent && user) {
      return [
        {
          id: user.userId,
          name: user.firstName,
          avatar: getAvatarURL(user.userId, 'user') ?? '',
          structureId: user.structures?.[0] ?? '',
          classIds: user.groupsIds ?? [],
          classNames: [...(user.classNames ?? []), ...(user.classes ?? [])],
        },
      ];
    }

    return (data ?? []).map((child) => ({
      id: child.id,
      name: child.firstName,
      avatar: getAvatarURL(child.id, 'user') ?? '',
      structureId: child.structures[0]?.id ?? '',
      classIds: child.idClasses ?? [],
      classNames: child.classes ?? [],
    }));
  }, [data, getAvatarURL, isStudent, user]);

  return { children, isLoading: isStudent ? false : isLoading };
};

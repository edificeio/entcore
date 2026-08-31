import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import type { Child } from '~/models/child';
import {
  buildTimetableDays,
  getCurrentWeekSchoolDays,
  toIsoDate,
} from '~/models/timetable';
import type { UseTimetableResult } from '~/models/timetable';
import type { EdtCourseFilter } from '~/services/api/edt.api';
import { mapCoursesToEntries } from '~/services/api/edt.api';
import {
  edtClassesQueryOptions,
  edtCoursesQueryOptions,
  edtSubjectsQueryOptions,
} from '~/services/queries/edt.queries';

export const useTimetable = (child: Child | undefined): UseTimetableResult => {
  const weekDays = useMemo(() => getCurrentWeekSchoolDays(new Date()), []);
  // Fetch a week further out than what's displayed (Monday + 7 days), so
  // next Monday's courses are already warm in cache when the user gets there.
  const endDate = useMemo(() => {
    const date = new Date(`${weekDays[0]}T00:00:00`);
    date.setDate(date.getDate() + 7);
    return toIsoDate(date);
  }, [weekDays]);

  const { data: subjects = [] } = useQuery(
    edtSubjectsQueryOptions(child?.structureId ?? ''),
  );

  // The EDT app resolves a child's classes against the structure's full
  // class list before filtering courses — the child's `idClasses` only
  // carries directory-level ids, while the courses filter also wants the
  // matching group externalId/name (`GET /viescolaire/classes`).
  const { data: classes = [] } = useQuery(
    edtClassesQueryOptions(child?.structureId ?? ''),
  );

  // Merge the resolved group id/externalId/name with the child's own raw
  // identifiers (deduplicated). `union: true` ORs every value together, so
  // this never loses a class the lookup above failed to resolve — it only
  // ever adds extra, harmless candidates.
  const filter: EdtCourseFilter = useMemo(() => {
    const matchedGroups = classes.filter((group) =>
      child?.classIds.includes(group.id),
    );
    const groupIds = new Set([
      ...matchedGroups.map((group) => group.id),
      ...(child?.classIds ?? []),
    ]);
    const groupNames = new Set([
      ...matchedGroups.map((group) => group.name),
      ...(child?.classNames ?? []),
    ]);
    const groupExternalIds = new Set([
      ...matchedGroups
        .map((group) => group.externalId)
        .filter((externalId): externalId is string => !!externalId),
      ...(child?.classNames ?? []),
    ]);
    return {
      teacherIds: [],
      groupIds: [...groupIds],
      groupExternalIds: [...groupExternalIds],
      groupNames: [...groupNames],
      union: true,
    };
  }, [classes, child]);

  const {
    data: courses = [],
    isLoading,
    isError,
  } = useQuery(
    edtCoursesQueryOptions(
      child?.structureId ?? '',
      weekDays[0],
      endDate,
      filter,
    ),
  );

  const days = useMemo(
    () => buildTimetableDays(mapCoursesToEntries(courses, subjects), weekDays),
    [courses, subjects, weekDays],
  );

  return { days, isLoading, isError };
};

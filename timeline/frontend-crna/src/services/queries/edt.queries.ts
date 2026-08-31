import { queryOptions } from '@tanstack/react-query';
import type { EdtCourseFilter } from '../api/edt.api';
import {
  fetchEdtChildren,
  fetchEdtClasses,
  fetchEdtCourses,
  fetchEdtSubjects,
  fetchEdtTimeSlots,
} from '../api/edt.api';

export const edtChildrenQueryOptions = queryOptions({
  queryKey: ['edt', 'children'],
  queryFn: fetchEdtChildren,
  staleTime: 5 * 60 * 1000,
});

// Subjects and time slots are structure-wide reference data (not
// per-student), so they can be cached for longer.
const REFERENCE_DATA_STALE_TIME = 30 * 60 * 1000;

export const edtSubjectsQueryOptions = (structureId: string) =>
  queryOptions({
    queryKey: ['edt', 'subjects', structureId],
    queryFn: () => fetchEdtSubjects(structureId),
    enabled: !!structureId,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

export const edtTimeSlotsQueryOptions = (structureId: string) =>
  queryOptions({
    queryKey: ['edt', 'time-slots', structureId],
    queryFn: () => fetchEdtTimeSlots(structureId),
    enabled: !!structureId,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

export const edtClassesQueryOptions = (structureId: string) =>
  queryOptions({
    queryKey: ['edt', 'classes', structureId],
    queryFn: () => fetchEdtClasses(structureId),
    enabled: !!structureId,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

export const edtCoursesQueryOptions = (
  structureId: string,
  startDate: string,
  endDate: string,
  filter: EdtCourseFilter,
) =>
  queryOptions({
    queryKey: [
      'edt',
      'courses',
      structureId,
      startDate,
      endDate,
      filter.groupIds,
      filter.groupExternalIds,
      filter.groupNames,
    ],
    queryFn: () => fetchEdtCourses(structureId, startDate, endDate, filter),
    enabled:
      !!structureId &&
      (filter.groupIds.length > 0 ||
        filter.groupExternalIds.length > 0 ||
        filter.groupNames.length > 0),
    staleTime: 5 * 60 * 1000,
  });

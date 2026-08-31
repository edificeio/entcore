import { odeServices } from '@edifice.io/client';
import type { TimetableColor, TimetableEntry } from '~/models/timetable';

export interface EdtChildStructure {
  id: string;
  name: string;
}

export interface EdtChild {
  id: string;
  firstName: string;
  lastName: string;
  displayName: string;
  classes: string[];
  idClasses: string[];
  structures: EdtChildStructure[];
}

export interface EdtSubject {
  subjectId: string;
  subjectCode: string;
  subjectLabel: string;
}

export interface EdtTimeSlot {
  id: string;
  name: string;
  startHour: string;
  endHour: string;
}

/** Children of the connected "Relative" user, as seen by the EDT/timetable app. */
export async function fetchEdtChildren(): Promise<EdtChild[]> {
  const children = await odeServices
    .http()
    .get<EdtChild[]>('/edt/user/children');
  if (!Array.isArray(children)) {
    throw new Error('timetable.widget.children.fetch.error');
  }
  return children;
}

export async function fetchEdtSubjects(
  structureId: string,
): Promise<EdtSubject[]> {
  const subjects = await odeServices
    .http()
    .get<EdtSubject[]>(`/directory/timetable/subjects/${structureId}`);
  if (!Array.isArray(subjects)) {
    throw new Error('timetable.widget.subjects.fetch.error');
  }
  return subjects;
}

export async function fetchEdtTimeSlots(
  structureId: string,
): Promise<EdtTimeSlot[]> {
  const timeSlots = await odeServices
    .http()
    .get<EdtTimeSlot[]>(`/edt/time-slots?structureId=${structureId}`);
  if (!Array.isArray(timeSlots)) {
    throw new Error('timetable.widget.time-slots.fetch.error');
  }
  return timeSlots;
}

export interface EdtGroup {
  id: string;
  name: string;
  color?: string;
  type_groupe?: number;
  externalId?: string;
}

/**
 * Classes/groups of a structure, as seen by the EDT app
 * (`GET /viescolaire/classes?idEtablissement=...&isEdt=true`). Used to
 * resolve a child's `idClasses` into the id/externalId/name triple the
 * courses filter expects.
 */
export async function fetchEdtClasses(
  structureId: string,
): Promise<EdtGroup[]> {
  const groups = await odeServices
    .http()
    .get<
      EdtGroup[]
    >(`/viescolaire/classes?idEtablissement=${structureId}&isEdt=true`);
  if (!Array.isArray(groups)) {
    throw new Error('timetable.widget.classes.fetch.error');
  }
  return groups;
}

export interface EdtCourseFilter {
  teacherIds: string[];
  groupIds: string[];
  groupExternalIds: string[];
  groupNames: string[];
  union: boolean;
}

export interface EdtCourseSubject {
  id: string;
  code?: string;
  externalId?: string;
  name: string;
  rank?: number;
}

/** A single scheduled lesson occurrence, as returned by the courses endpoint. */
export interface EdtCourse {
  _id: string;
  subjectId: string;
  /** Already embedded by the courses endpoint — no need to join against the subjects list. */
  subject?: EdtCourseSubject;
  roomLabels?: string[];
  teacherIds?: string[];
  startDate: string;
  endDate: string;
  dayOfWeek?: number;
  /** e.g. "pink-lighter" — already computed server-side. */
  color?: string;
}

/**
 * Courses (lesson occurrences) for a structure within `[startDate, endDate]`
 * (yyyy-MM-dd), filtered by `filter` (e.g. the child's class via `groupIds`).
 */
export async function fetchEdtCourses(
  structureId: string,
  startDate: string,
  endDate: string,
  filter: EdtCourseFilter,
): Promise<EdtCourse[]> {
  const courses = await odeServices
    .http()
    .post<
      EdtCourse[]
    >(`/edt/structures/${structureId}/common/courses/${startDate}/${endDate}`, filter);
  if (!Array.isArray(courses)) {
    throw new Error('timetable.widget.courses.fetch.error');
  }
  return courses;
}

const TIMETABLE_COLORS: TimetableColor[] = [
  'blue',
  'yellow',
  'green',
  'orange',
  'pink',
  'grey',
];

const hashString = (value: string): number => {
  let hash = 0;
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) >>> 0;
  }
  return hash;
};

/** Fallback used only when the course carries no `color` of its own. */
const colorForSubject = (subjectId: string): TimetableColor =>
  TIMETABLE_COLORS[hashString(subjectId) % TIMETABLE_COLORS.length];

/** The API returns colors like "pink-lighter" — map the base hue to our palette. */
const parseColor = (
  rawColor: string | undefined,
  subjectId: string,
): TimetableColor => {
  const base = rawColor?.split('-')[0].toLowerCase();
  const match = TIMETABLE_COLORS.find((color) => color === base);
  return match ?? (base === 'gray' ? 'grey' : colorForSubject(subjectId));
};

/** The API returns "yyyy-MM-dd HH:mm:ss" (space-separated, no timezone) — normalize to ISO-ish so `new Date()` parses it consistently across browsers. */
const normalizeDateTime = (value: string): string =>
  value.includes('T') ? value : value.replace(' ', 'T');

export const mapCoursesToEntries = (
  courses: EdtCourse[],
  subjects: EdtSubject[],
): TimetableEntry[] => {
  const labelBySubjectId = new Map(
    subjects.map((subject) => [subject.subjectId, subject.subjectLabel]),
  );

  return courses.map((course) => ({
    id: course._id,
    subject:
      course.subject?.name ??
      labelBySubjectId.get(course.subjectId) ??
      course.subjectId,
    room: course.roomLabels?.find((label) => !!label),
    startDate: normalizeDateTime(course.startDate),
    endDate: normalizeDateTime(course.endDate),
    color: parseColor(course.color, course.subjectId),
  }));
};

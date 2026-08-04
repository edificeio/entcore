import { useToast } from '@edifice.io/react';
import { EditorRef } from '@edifice.io/react/editor';
import { useEffect, useRef, useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useI18n } from '~/hooks/useI18n';
import { AbsenceSettings } from '~/models/absence';

export interface UseAbsenceModalProps {
  /** Existing settings to pre-fill the form, or `undefined` if none exist yet. */
  settings?: AbsenceSettings;
  /** Called with the full payload when the user saves a valid form. */
  onSave: (payload: AbsenceSettings) => Promise<void>;
  /** Called once a save succeeds, to close the modal. */
  onModalClose: () => void;
}

export interface AbsenceFormValues {
  enabled: boolean;
  startDate?: Date;
  endDate?: Date;
}

/** Converts a locally-picked calendar date to the UTC instant of its start of day. */
function startOfLocalDayUtc(date: Date): string {
  const value = new Date(date);
  value.setHours(0, 0, 0, 0);
  return value.toISOString();
}

/** Converts a locally-picked calendar date to the UTC instant of its end of day. */
function endOfLocalDayUtc(date: Date): string {
  const value = new Date(date);
  value.setHours(23, 59, 59, 999);
  return value.toISOString();
}

function hasBodyContent(bodyJson: unknown): boolean {
  return typeof bodyJson === 'string' ? !!bodyJson.trim() : !!bodyJson;
}

/** Returns the calendar day right after the given date. */
function dayAfter(date: Date): Date {
  const value = new Date(date);
  value.setDate(value.getDate() + 1);
  return value;
}

/**
 * Local form state and save handling for the absence settings modal. Has no
 * network concerns: the caller supplies `settings` to pre-fill the form and
 * an `onSave` action to persist a valid payload.
 *
 * Deliberate departure from the FS (IMPULS-6130 US-1 Gherkin scenarios),
 * decided in pairing: no inline error messages under the fields. Instead,
 * the Save button is simply disabled — via `canSave` — until the end date
 * is defined and on/after the start date, the text is filled in whenever
 * the message is enabled, and at least one field has actually changed
 * since the last load/save (`isDirty`) — otherwise saving would be a no-op.
 */
export function useAbsenceModal({
  settings,
  onSave,
  onModalClose,
}: UseAbsenceModalProps) {
  const { t } = useI18n();
  const toast = useToast();
  const editor = useRef<EditorRef>(null);
  const initialBodyJson = useRef(settings?.bodyJson);
  const [hasBody, setHasBody] = useState(() =>
    hasBodyContent(settings?.bodyJson),
  );
  const [isBodyDirty, setIsBodyDirty] = useState(false);

  const defaultValues: AbsenceFormValues = {
    enabled: settings?.enabled ?? false,
    startDate: settings?.startAt ? new Date(settings.startAt) : undefined,
    endDate: settings?.endAt ? new Date(settings.endAt) : undefined,
  };
  // Own snapshot of the last loaded/saved field values: `formState.isDirty`
  // only reflects changes made with `shouldDirty: true`, which the reactive
  // end-date effect below deliberately doesn't set, so it can't be relied on
  // here.
  const initialFieldValues = useRef(defaultValues);

  const form = useForm<AbsenceFormValues>({ defaultValues });
  const { control, register, setValue, handleSubmit } = form;

  const enabled = useWatch({ control, name: 'enabled' });
  const startDate = useWatch({ control, name: 'startDate' });
  const endDate = useWatch({ control, name: 'endDate' });

  // Reactively bumps the end date to the day after the start date whenever
  // a start date change makes the currently picked end date invalid (or
  // none was picked yet). Leaves an already-valid end date untouched.
  useEffect(() => {
    if (!startDate) {
      return;
    }
    if (!endDate || endDate < startDate) {
      setValue('endDate', dayAfter(startDate));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [startDate]);

  const isFieldsDirty =
    enabled !== initialFieldValues.current.enabled ||
    startDate?.getTime() !== initialFieldValues.current.startDate?.getTime() ||
    endDate?.getTime() !== initialFieldValues.current.endDate?.getTime();
  const isDirty = isFieldsDirty || isBodyDirty;

  const canSave =
    !!startDate &&
    !!endDate &&
    endDate >= startDate &&
    (!enabled || hasBody) &&
    isDirty;

  function handleBodyChange() {
    const bodyJson = editor.current?.getContent('json');
    setHasBody(hasBodyContent(editor.current?.getContent('plain')));
    setIsBodyDirty(
      JSON.stringify(bodyJson) !== JSON.stringify(initialBodyJson.current),
    );
  }

  const handleSave = handleSubmit(async (values) => {
    if (!canSave) {
      return;
    }

    const payload: AbsenceSettings = {
      enabled: values.enabled,
      startAt: startOfLocalDayUtc(values.startDate as Date),
      endAt: endOfLocalDayUtc(values.endDate as Date),
      bodyJson: editor.current?.getContent('json') ?? '',
    };

    try {
      await onSave(payload);
      toast.success(t('conversation.absence.notify.saved'));
      // Rebase the dirty-tracking baseline in case the modal instance
      // survives the close (e.g. reopened without remounting).
      initialFieldValues.current = values;
      initialBodyJson.current = payload.bodyJson;
      setIsBodyDirty(false);
      onModalClose();
    } catch (error) {
      toast.error(t('conversation.absence.notify.error'));
      console.error('error:', error);
    }
  });

  return {
    editor,
    control,
    register,
    setValue,
    enabled,
    startDate,
    endDate,
    canSave,
    handleBodyChange,
    handleSave,
  };
}

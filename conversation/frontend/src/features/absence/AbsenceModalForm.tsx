import {
  ButtonBeta,
  DatePicker,
  Flex,
  FormControl,
  Label,
  Modal,
  Switch,
  useBreakpoint,
} from '@edifice.io/react';
import { Editor } from '@edifice.io/react/editor';
import clsx from 'clsx';
import { Controller } from 'react-hook-form';
import { useI18n } from '~/hooks/useI18n';
import { AbsenceSettings } from '~/models/absence';
import './AbsenceModal.css';
import { useAbsenceModal } from './hooks';

export interface AbsenceModalFormProps {
  onModalClose: () => void;
  /** Existing settings to pre-fill the form, or `undefined` if none exist yet. */
  settings?: AbsenceSettings;
  /** Whether a save is currently in flight. */
  isSaving?: boolean;
  /** Called with the full payload when the user saves a valid form. */
  onSave: (payload: AbsenceSettings) => Promise<void>;
}

export function AbsenceModalForm({
  onModalClose,
  settings,
  isSaving,
  onSave,
}: AbsenceModalFormProps) {
  const { t } = useI18n();
  const { md } = useBreakpoint();
  const {
    editor,
    control,
    register,
    enabled,
    startDate,
    canSave,
    handleBodyChange,
    handleSave,
  } = useAbsenceModal({ settings, onSave, onModalClose });

  return (
    <>
      <Modal.Body>
        <Flex direction="column" gap="16">
          <Switch
            label={t('conversation.absence.modal.toggle.label')}
            {...register('enabled')}
          />

          <Flex direction="column" gap="16">
            <Flex
              direction={md ? 'row' : 'column'}
              gap={md ? '24' : '16'}
              align={md ? 'start' : 'stretch'}
              wrap="nowrap"
            >
              <FormControl id="absence-start-date" className="flex-fill">
                <Label className="d-block">
                  {t('conversation.absence.modal.startDate.label')}
                </Label>
                <Controller
                  control={control}
                  name="startDate"
                  render={({ field }) => (
                    <DatePicker
                      id="absence-start-date"
                      value={field.value}
                      onChange={field.onChange}
                      disabled={!enabled}
                      dateFormat={t('conversation.absence.modal.dateFormat')}
                      className="w-100"
                    />
                  )}
                />
              </FormControl>

              <FormControl id="absence-end-date" className="flex-fill">
                <Label className="d-block">
                  {t('conversation.absence.modal.endDate.label')}
                </Label>
                <Controller
                  control={control}
                  name="endDate"
                  render={({ field }) => (
                    <DatePicker
                      id="absence-end-date"
                      value={field.value}
                      onChange={field.onChange}
                      disabled={!enabled}
                      minDate={startDate}
                      dateFormat={t('conversation.absence.modal.dateFormat')}
                      className="w-100"
                    />
                  )}
                />
              </FormControl>
            </Flex>

            <FormControl id="absence-body">
              <Label className="d-block">
                {t('conversation.absence.modal.editor.label')}
              </Label>
              <div
                className={clsx('absence-editor', {
                  'absence-editor--disabled': !enabled,
                })}
              >
                <Editor
                  ref={editor}
                  id="absence-body"
                  content={settings?.bodyJson ?? ''}
                  mode={enabled ? 'edit' : 'read'}
                  placeholder={t(
                    'conversation.absence.modal.editor.placeholder',
                  )}
                  onContentChange={handleBodyChange}
                />
              </div>
            </FormControl>
          </Flex>
        </Flex>
      </Modal.Body>

      <Modal.Footer>
        <ButtonBeta
          type="button"
          color="tertiary"
          variant="ghost"
          onClick={onModalClose}
          data-testid="conversation-absence-modal-button-cancel"
        >
          {t('conversation.absence.modal.cancel')}
        </ButtonBeta>
        <ButtonBeta
          type="button"
          color="default"
          variant="filled"
          onClick={handleSave}
          isLoading={isSaving}
          disabled={!canSave}
          data-testid="conversation-absence-modal-button-save"
        >
          {t('conversation.absence.modal.save')}
        </ButtonBeta>
      </Modal.Footer>
    </>
  );
}

import {
  ButtonSkeleton,
  Flex,
  FormControl,
  Label,
  Modal,
  TextSkeleton,
  useBreakpoint,
} from '@edifice.io/react';
import { EditorSkeleton } from '@edifice.io/react/editor';
import { useI18n } from '~/hooks/useI18n';

export function AbsenceModalSkeleton() {
  const { t } = useI18n();
  const { md } = useBreakpoint();

  return (
    <>
      <Modal.Body>
        <Flex
          direction="column"
          gap="16"
          data-testid="conversation-absence-modal-skeleton"
        >
          <TextSkeleton className="col-4" />

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
                <ButtonSkeleton className="w-100" />
              </FormControl>

              <FormControl id="absence-end-date" className="flex-fill">
                <Label className="d-block">
                  {t('conversation.absence.modal.endDate.label')}
                </Label>
                <ButtonSkeleton className="w-100" />
              </FormControl>
            </Flex>

            <FormControl id="absence-body">
              <Label className="d-block">
                {t('conversation.absence.modal.editor.label')}
              </Label>
              <EditorSkeleton mode="edit" />
            </FormControl>
          </Flex>
        </Flex>
      </Modal.Body>

      <Modal.Footer>
        <ButtonSkeleton className="col-2" />
        <ButtonSkeleton className="col-2" />
      </Modal.Footer>
    </>
  );
}

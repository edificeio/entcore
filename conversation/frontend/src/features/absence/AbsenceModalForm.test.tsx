/**
 * Test suite for the AbsenceModalForm component (IMPULS-6138).
 *
 * Deliberate departure from the FS (IMPULS-6130 US-1 Gherkin scenarios),
 * decided in pairing: no inline error messages under the fields. Instead the
 * Save button is simply disabled until the form is complete — detailed
 * `canSave` logic lives in `useAbsenceModal.test.ts`. This suite covers
 * rendering and wiring: pre-fill from `settings`, that the date pickers and
 * the editor become non-interactive when the message is disabled, that the
 * end date picker's `minDate` reactively follows the chosen start date, that
 * the Save button is disabled until required fields are filled in, and that
 * the modal never closes itself after a save (success or failure).
 *
 * AntD's `DatePicker` calendar popup and the tiptap `Editor` are not worth
 * driving through jsdom: both are replaced with plain controllable inputs
 * exposing the same props/ref contract.
 */
import { forwardRef, useImperativeHandle, useRef, useState } from 'react';
import { describe, expect, it } from 'vitest';
import { fireEvent, render, screen } from '~/mocks/setup';
import { AbsenceModalForm } from './AbsenceModalForm';

const mocks = vi.hoisted(() => ({
  useToast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return {
    ...actual,
    useToast: () => {
      const useToast = actual.useToast();
      return {
        ...useToast,
        success: mocks.useToast.success,
        error: mocks.useToast.error,
      };
    },
    DatePicker: ({
      value,
      onChange,
      disabled,
      minDate,
    }: {
      value?: Date;
      onChange?: (date?: Date) => void;
      disabled?: boolean;
      minDate?: Date;
    }) => (
      <input
        type="text"
        data-testid="date-picker-mock"
        data-min-date={minDate ? minDate.toISOString().slice(0, 10) : ''}
        disabled={disabled}
        value={value ? value.toISOString().slice(0, 10) : ''}
        onChange={(event) => {
          const raw = event.target.value;
          onChange?.(raw ? new Date(raw) : undefined);
        }}
      />
    ),
  };
});

vi.mock('@edifice.io/react/editor', () => ({
  Editor: forwardRef(function EditorMock(
    {
      content,
      placeholder,
      mode,
      onContentChange,
    }: {
      content?: unknown;
      placeholder?: string;
      mode?: 'edit' | 'read';
      onContentChange?: () => void;
    },
    ref: any,
  ) {
    const initial = typeof content === 'string' ? content : '';
    const [text, setText] = useState(initial);
    // A ref (not React state) so `getContent` reflects the latest keystroke
    // immediately, matching the real (uncontrolled) tiptap editor — state
    // updates are batched and wouldn't be visible yet within the same
    // `onChange` handler that also calls `onContentChange`.
    const textRef = useRef(initial);
    useImperativeHandle(ref, () => ({
      getContent: (as: string) =>
        as === 'json' ? { text: textRef.current } : textRef.current,
    }));
    return (
      <textarea
        placeholder={placeholder}
        readOnly={mode === 'read'}
        value={text}
        onChange={(event) => {
          textRef.current = event.target.value;
          setText(event.target.value);
          onContentChange?.();
        }}
      />
    );
  }),
}));

function getDatePickers() {
  const [startDate, endDate] = screen.getAllByTestId('date-picker-mock');
  return { startDate, endDate };
}

describe('AbsenceModalForm component', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('pre-fills the form from existing settings', () => {
    render(
      <AbsenceModalForm
        onModalClose={vi.fn()}
        onSave={vi.fn()}
        settings={{
          enabled: true,
          startAt: '2026-01-10T00:00:00.000Z',
          endAt: '2026-01-20T23:59:59.999Z',
          bodyJson: 'Je suis absent',
        }}
      />,
    );

    expect(
      screen.getByLabelText('conversation.absence.modal.toggle.label'),
    ).toBeChecked();

    const { startDate, endDate } = getDatePickers();
    expect(startDate).toHaveValue('2026-01-10');
    expect(endDate).toHaveValue('2026-01-20');

    expect(
      screen.getByPlaceholderText(
        'conversation.absence.modal.editor.placeholder',
      ),
    ).toHaveValue('Je suis absent');
  });

  it('disables the dates and the editor when the message is disabled', () => {
    render(<AbsenceModalForm onModalClose={vi.fn()} onSave={vi.fn()} />);

    const { startDate, endDate } = getDatePickers();
    expect(startDate).toBeDisabled();
    expect(endDate).toBeDisabled();
    expect(
      screen.getByPlaceholderText(
        'conversation.absence.modal.editor.placeholder',
      ),
    ).toHaveAttribute('readonly');

    fireEvent.click(
      screen.getByLabelText('conversation.absence.modal.toggle.label'),
    );

    expect(startDate).not.toBeDisabled();
    expect(endDate).not.toBeDisabled();
    expect(
      screen.getByPlaceholderText(
        'conversation.absence.modal.editor.placeholder',
      ),
    ).not.toHaveAttribute('readonly');
  });

  it('applies the start date as a reactive minDate on the end date', () => {
    render(<AbsenceModalForm onModalClose={vi.fn()} onSave={vi.fn()} />);

    const { startDate, endDate } = getDatePickers();
    expect(endDate).toHaveAttribute('data-min-date', '');

    fireEvent.change(startDate, { target: { value: '2026-01-10' } });

    expect(endDate).toHaveAttribute('data-min-date', '2026-01-10');
  });

  it('disables the Save button until the dates are filled in', () => {
    render(<AbsenceModalForm onModalClose={vi.fn()} onSave={vi.fn()} />);

    const saveButton = screen.getByTestId(
      'conversation-absence-modal-button-save',
    );
    expect(saveButton).toBeDisabled();

    const { startDate, endDate } = getDatePickers();
    fireEvent.change(startDate, { target: { value: '2026-01-10' } });

    // The end date is reactively filled in the day after the start date.
    expect(endDate).toHaveValue('2026-01-11');
    expect(saveButton).not.toBeDisabled();
  });

  it('bumps the end date the day after the start date once it is no longer valid', () => {
    render(<AbsenceModalForm onModalClose={vi.fn()} onSave={vi.fn()} />);

    const { startDate, endDate } = getDatePickers();
    fireEvent.change(startDate, { target: { value: '2026-01-10' } });
    fireEvent.change(endDate, { target: { value: '2026-01-15' } });

    // Still valid after a small shift: left untouched.
    fireEvent.change(startDate, { target: { value: '2026-01-12' } });
    expect(endDate).toHaveValue('2026-01-15');

    // No longer valid: reactively bumped to the day after the new start date.
    fireEvent.change(startDate, { target: { value: '2026-01-20' } });
    expect(endDate).toHaveValue('2026-01-21');
  });

  it('disables the Save button when the message is enabled without text', () => {
    render(<AbsenceModalForm onModalClose={vi.fn()} onSave={vi.fn()} />);

    const saveButton = screen.getByTestId(
      'conversation-absence-modal-button-save',
    );
    const { startDate, endDate } = getDatePickers();
    fireEvent.change(startDate, { target: { value: '2026-01-10' } });
    fireEvent.change(endDate, { target: { value: '2026-01-20' } });
    expect(saveButton).not.toBeDisabled();

    fireEvent.click(
      screen.getByLabelText('conversation.absence.modal.toggle.label'),
    );
    expect(saveButton).toBeDisabled();

    fireEvent.change(
      screen.getByPlaceholderText(
        'conversation.absence.modal.editor.placeholder',
      ),
      { target: { value: 'Je suis en congés' } },
    );
    expect(saveButton).not.toBeDisabled();
  });

  it('does not close the modal after a successful save (toast only)', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    const onModalClose = vi.fn();
    render(<AbsenceModalForm onModalClose={onModalClose} onSave={onSave} />);

    const { startDate, endDate } = getDatePickers();
    fireEvent.change(startDate, { target: { value: '2026-01-10' } });
    fireEvent.change(endDate, { target: { value: '2026-01-20' } });

    fireEvent.click(
      screen.getByTestId('conversation-absence-modal-button-save'),
    );
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(onSave).toHaveBeenCalledTimes(1);
    expect(mocks.useToast.success).toHaveBeenCalledTimes(1);
    expect(onModalClose).not.toHaveBeenCalled();
  });

  it('shows an error toast and does not close the modal when onSave fails', async () => {
    const onSave = vi.fn().mockRejectedValue(new Error('network error'));
    const onModalClose = vi.fn();
    render(<AbsenceModalForm onModalClose={onModalClose} onSave={onSave} />);

    const { startDate, endDate } = getDatePickers();
    fireEvent.change(startDate, { target: { value: '2026-01-10' } });
    fireEvent.change(endDate, { target: { value: '2026-01-20' } });

    fireEvent.click(
      screen.getByTestId('conversation-absence-modal-button-save'),
    );
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(mocks.useToast.error).toHaveBeenCalledTimes(1);
    expect(onModalClose).not.toHaveBeenCalled();
  });

  it('toggles the enabled state on click', () => {
    render(<AbsenceModalForm onModalClose={vi.fn()} onSave={vi.fn()} />);

    const toggle = screen.getByLabelText(
      'conversation.absence.modal.toggle.label',
    );
    expect(toggle).not.toBeChecked();

    fireEvent.click(toggle);

    expect(toggle).toBeChecked();
  });
});

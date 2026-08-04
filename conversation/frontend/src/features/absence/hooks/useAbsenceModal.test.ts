/**
 * Test suite for the useAbsenceModal hook (IMPULS-6138).
 *
 * Deliberate departure from the FS (IMPULS-6130 US-1 Gherkin scenarios),
 * decided in pairing: no inline error messages. Instead `canSave` reflects
 * whether the Save button should be enabled — end date defined and on/after
 * the start date, text filled in whenever the message is enabled, and at
 * least one field changed since the last load/save — and `handleSave`
 * refuses to call `onSave` when it's false, as a defensive backstop behind
 * the disabled button.
 *
 * Covers pre-fill from `settings`, the UTC conversion of the payload passed
 * to `onSave`, and that `onModalClose` is called once a save succeeds
 * (closing the modal so the success toast isn't hidden behind it) but never
 * when it fails, so the user can see the error and retry.
 * Rendering/wiring is covered separately in AbsenceModal.test.tsx.
 */
import { act, renderHook } from '@testing-library/react';
import { MockedProviders } from '~/mocks/mockedProvider';
import { useAbsenceModal } from './useAbsenceModal';

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
  };
});

/** Wires a fake editor ref exposing `getContent`, as the real Editor would. */
function setEditorText(
  result: ReturnType<
    typeof renderHook<ReturnType<typeof useAbsenceModal>, unknown>
  >['result'],
  text: string,
) {
  (result.current.editor as any).current = {
    getContent: (as: 'html' | 'json' | 'plain') =>
      as === 'json' ? { text } : text,
  };
}

function renderAbsenceModal(
  props: Partial<Parameters<typeof useAbsenceModal>[0]> = {},
) {
  return renderHook(
    () =>
      useAbsenceModal({
        onSave: vi.fn().mockResolvedValue(undefined),
        onModalClose: vi.fn(),
        ...props,
      }),
    { wrapper: MockedProviders },
  );
}

describe('useAbsenceModal', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it('initializes its state from existing settings', () => {
    const { result } = renderAbsenceModal({
      settings: {
        enabled: true,
        startAt: '2026-01-10T00:00:00.000Z',
        endAt: '2026-01-20T23:59:59.999Z',
        bodyJson: 'Je suis absent',
      },
    });

    expect(result.current.enabled).toBe(true);
    expect(result.current.startDate).toEqual(
      new Date('2026-01-10T00:00:00.000Z'),
    );
    expect(result.current.endDate).toEqual(
      new Date('2026-01-20T23:59:59.999Z'),
    );
    // Nothing has changed since load: saving would be a no-op.
    expect(result.current.canSave).toBe(false);
  });

  it('forbids saving when nothing has changed since the settings were loaded', async () => {
    const onSave = vi.fn();
    const { result } = renderAbsenceModal({
      onSave,
      settings: {
        enabled: true,
        startAt: '2026-01-10T00:00:00.000Z',
        endAt: '2026-01-20T23:59:59.999Z',
        bodyJson: 'Je suis absent',
      },
    });

    expect(result.current.canSave).toBe(false);
    await act(async () => result.current.handleSave());
    expect(onSave).not.toHaveBeenCalled();
  });

  it('allows saving once a field is changed from its loaded value', () => {
    const { result } = renderAbsenceModal({
      settings: {
        enabled: true,
        startAt: '2026-01-10T00:00:00.000Z',
        endAt: '2026-01-20T23:59:59.999Z',
        bodyJson: 'Je suis absent',
      },
    });

    act(() => result.current.setValue('endDate', new Date('2026-01-25')));

    expect(result.current.canSave).toBe(true);
  });

  it('allows saving once only the body text is changed from its loaded value', () => {
    const { result } = renderAbsenceModal({
      settings: {
        enabled: true,
        startAt: '2026-01-10T00:00:00.000Z',
        endAt: '2026-01-20T23:59:59.999Z',
        bodyJson: 'Je suis absent',
      },
    });

    expect(result.current.canSave).toBe(false);
    act(() => setEditorText(result, 'Nouveau message'));
    act(() => result.current.handleBodyChange());

    expect(result.current.canSave).toBe(true);
  });

  it('starts disabled and without dates when no settings exist', () => {
    const { result } = renderAbsenceModal();

    expect(result.current.enabled).toBe(false);
    expect(result.current.startDate).toBeUndefined();
    expect(result.current.endDate).toBeUndefined();
    expect(result.current.canSave).toBe(false);
  });

  it('reactively fills in the end date the day after the start date', () => {
    const { result } = renderAbsenceModal();

    act(() => result.current.setValue('startDate', new Date('2026-01-10')));

    expect(result.current.endDate).toEqual(new Date('2026-01-11'));
    expect(result.current.canSave).toBe(true);
  });

  it('leaves an already-valid end date untouched when the start date changes', () => {
    const { result } = renderAbsenceModal();

    act(() => {
      result.current.setValue('startDate', new Date('2026-01-10'));
      result.current.setValue('endDate', new Date('2026-01-15'));
    });
    act(() => result.current.setValue('startDate', new Date('2026-01-12')));

    expect(result.current.endDate).toEqual(new Date('2026-01-15'));
  });

  it('bumps the end date the day after the new start date once it is no longer valid', () => {
    const { result } = renderAbsenceModal();

    act(() => {
      result.current.setValue('startDate', new Date('2026-01-10'));
      result.current.setValue('endDate', new Date('2026-01-15'));
    });
    act(() => result.current.setValue('startDate', new Date('2026-01-20')));

    expect(result.current.endDate).toEqual(new Date('2026-01-21'));
  });

  it('forbids saving when the end date is missing', async () => {
    const onSave = vi.fn();
    const { result } = renderAbsenceModal({ onSave });

    act(() => {
      result.current.setValue('startDate', new Date('2026-01-10'));
      result.current.setValue('endDate', new Date('2026-01-20'));
    });
    // Cleared afterwards, independently of the start date: the reactive
    // shift only reacts to start date changes, so this stays missing.
    act(() => result.current.setValue('endDate', undefined));

    expect(result.current.canSave).toBe(false);
    await act(async () => result.current.handleSave());
    expect(onSave).not.toHaveBeenCalled();
  });

  it('forbids saving when the end date is before the start date', async () => {
    const onSave = vi.fn();
    const { result } = renderAbsenceModal({ onSave });

    act(() => result.current.setValue('startDate', new Date('2026-01-20')));
    // Set independently of the start date change, so the reactive shift
    // (which only reacts to the start date) doesn't correct it.
    act(() => result.current.setValue('endDate', new Date('2026-01-10')));

    expect(result.current.canSave).toBe(false);
    await act(async () => result.current.handleSave());
    expect(onSave).not.toHaveBeenCalled();
  });

  it('requires text only when the message is enabled', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    const { result } = renderAbsenceModal({ onSave });

    act(() => {
      result.current.setValue('startDate', new Date('2026-01-10'));
      result.current.setValue('endDate', new Date('2026-01-20'));
    });

    // Disabled (default) + empty text: save is allowed.
    expect(result.current.canSave).toBe(true);
    await act(async () => result.current.handleSave());
    expect(onSave).toHaveBeenCalledTimes(1);

    onSave.mockClear();

    // Enabled + still empty text: save is blocked.
    act(() => result.current.setValue('enabled', true));
    expect(result.current.canSave).toBe(false);
    await act(async () => result.current.handleSave());
    expect(onSave).not.toHaveBeenCalled();

    // Filling in the text allows saving again.
    act(() => setEditorText(result, 'Je suis en congés'));
    act(() => result.current.handleBodyChange());
    expect(result.current.canSave).toBe(true);
  });

  it('calls onSave with a payload converted to UTC (local day boundaries)', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    const { result } = renderAbsenceModal({ onSave });

    act(() => {
      result.current.setValue('enabled', true);
      result.current.setValue('startDate', new Date('2026-01-10'));
      result.current.setValue('endDate', new Date('2026-01-20'));
    });
    act(() => setEditorText(result, 'Je suis en congés'));
    act(() => result.current.handleBodyChange());

    await act(async () => result.current.handleSave());

    expect(onSave).toHaveBeenCalledTimes(1);
    const payload = onSave.mock.calls[0][0];
    expect(payload.enabled).toBe(true);
    expect(new Date(payload.startAt).getHours()).toBe(0);
    expect(new Date(payload.endAt).getHours()).toBe(23);
    expect(payload.bodyJson).toEqual({ text: 'Je suis en congés' });
  });

  it('shows a success toast and closes the modal after a successful save', async () => {
    const onSave = vi.fn().mockResolvedValue(undefined);
    const onModalClose = vi.fn();
    const { result } = renderAbsenceModal({ onSave, onModalClose });

    act(() => {
      result.current.setValue('startDate', new Date('2026-01-10'));
      result.current.setValue('endDate', new Date('2026-01-20'));
    });
    await act(async () => result.current.handleSave());

    expect(mocks.useToast.success).toHaveBeenCalledTimes(1);
    expect(onModalClose).toHaveBeenCalledTimes(1);
  });

  it('shows an error toast and keeps the modal open when onSave fails', async () => {
    const onSave = vi.fn().mockRejectedValue(new Error('network error'));
    const onModalClose = vi.fn();
    const { result } = renderAbsenceModal({ onSave, onModalClose });

    act(() => {
      result.current.setValue('startDate', new Date('2026-01-10'));
      result.current.setValue('endDate', new Date('2026-01-20'));
    });
    await act(async () => result.current.handleSave());

    expect(mocks.useToast.error).toHaveBeenCalledTimes(1);
    expect(onModalClose).not.toHaveBeenCalled();
  });
});

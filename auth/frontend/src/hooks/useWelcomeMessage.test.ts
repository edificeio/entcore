import { HttpResponse, http } from 'msw';
import { describe, expect, it } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { server } from '~/mocks/server';
import { pickContent, useWelcomeMessage } from './useWelcomeMessage';

const WELCOME_URL = '/auth/configure/welcome';

// ─── pickContent unit tests ───────────────────────────────────────────────────

describe('pickContent', () => {
  it('returns null when enabled is false', () => {
    expect(pickContent({ enabled: false, fr: 'Bonjour' }, 'fr')).toBeNull();
  });

  it('returns the content for the requested language', () => {
    expect(
      pickContent({ enabled: true, fr: 'Bonjour', en: 'Hello' }, 'en'),
    ).toBe('Hello');
  });

  it('falls back to fr when the requested language is empty', () => {
    expect(
      pickContent({ enabled: true, fr: 'Bonjour', en: '' }, 'en'),
    ).toBe('Bonjour');
  });

  it('falls back to fr when the requested language is absent', () => {
    expect(pickContent({ enabled: true, fr: 'Bonjour' }, 'es')).toBe(
      'Bonjour',
    );
  });

  it('returns null when all languages are empty', () => {
    expect(pickContent({ enabled: true, fr: '', en: '' }, 'en')).toBeNull();
  });
});

// ─── useWelcomeMessage integration tests ─────────────────────────────────────

describe('useWelcomeMessage', () => {
  it('returns ready state with sanitized HTML on success', async () => {
    server.use(
      http.get(WELCOME_URL, () =>
        HttpResponse.json({ enabled: true, fr: '<p>Bienvenue</p>' }),
      ),
    );

    const { result } = renderHook(() => useWelcomeMessage());
    expect(result.current.status).toBe('loading');

    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect((result.current as { status: 'ready'; html: string }).html).toBe(
      '<p>Bienvenue</p>',
    );
  });

  it('strips XSS payloads from the HTML', async () => {
    server.use(
      http.get(WELCOME_URL, () =>
        HttpResponse.json({
          enabled: true,
          fr: '<p>Hello</p><script>alert(1)</script>',
        }),
      ),
    );

    const { result } = renderHook(() => useWelcomeMessage());
    await waitFor(() => expect(result.current.status).toBe('ready'));
    const { html } = result.current as { status: 'ready'; html: string };
    expect(html).not.toContain('<script>');
    expect(html).toContain('<p>Hello</p>');
  });

  it('returns hidden when enabled is false', async () => {
    server.use(
      http.get(WELCOME_URL, () =>
        HttpResponse.json({ enabled: false, fr: 'Bonjour' }),
      ),
    );

    const { result } = renderHook(() => useWelcomeMessage());
    await waitFor(() => expect(result.current.status).toBe('hidden'));
  });

  it('returns hidden when all content is empty', async () => {
    server.use(
      http.get(WELCOME_URL, () =>
        HttpResponse.json({ enabled: true, fr: '', en: '' }),
      ),
    );

    const { result } = renderHook(() => useWelcomeMessage());
    await waitFor(() => expect(result.current.status).toBe('hidden'));
  });

  it('returns hidden on 404', async () => {
    server.use(
      http.get(WELCOME_URL, () => new HttpResponse(null, { status: 404 })),
    );

    const { result } = renderHook(() => useWelcomeMessage());
    await waitFor(() => expect(result.current.status).toBe('hidden'));
  });

  it('returns hidden on 500', async () => {
    server.use(
      http.get(WELCOME_URL, () => new HttpResponse(null, { status: 500 })),
    );

    const { result } = renderHook(() => useWelcomeMessage());
    await waitFor(() => expect(result.current.status).toBe('hidden'));
  });

  it('returns hidden on network error', async () => {
    server.use(
      http.get(WELCOME_URL, () => HttpResponse.error()),
    );

    const { result } = renderHook(() => useWelcomeMessage());
    await waitFor(() => expect(result.current.status).toBe('hidden'));
  });
});

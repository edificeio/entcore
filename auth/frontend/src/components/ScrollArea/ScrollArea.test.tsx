import { fireEvent } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { render, screen } from '~/mocks/setup';
import { ScrollArea } from '.';

const setScrollMetrics = (
  el: HTMLElement,
  metrics: { scrollTop: number; clientHeight: number; scrollHeight: number },
) => {
  Object.entries(metrics).forEach(([key, value]) =>
    Object.defineProperty(el, key, { value, configurable: true }),
  );
};

const shadows = (container: HTMLElement) => ({
  top: container.querySelector('.wayf-scroll-area__shadow--top'),
  bottom: container.querySelector('.wayf-scroll-area__shadow--bottom'),
});

describe('ScrollArea', () => {
  it('renders its children', () => {
    render(
      <ScrollArea>
        <p>content</p>
      </ScrollArea>,
    );
    expect(screen.getByText('content')).toBeTruthy();
  });

  it('only shows the shadow of the side where content is hidden', () => {
    const { container } = render(
      <ScrollArea>
        <p>content</p>
      </ScrollArea>,
    );
    const viewport = container.querySelector(
      '.wayf-scroll-area__viewport',
    ) as HTMLElement;
    const { top, bottom } = shadows(container);

    setScrollMetrics(viewport, {
      scrollTop: 0,
      clientHeight: 100,
      scrollHeight: 300,
    });
    fireEvent.scroll(viewport);
    expect(top?.getAttribute('data-visible')).toBe('false');
    expect(bottom?.getAttribute('data-visible')).toBe('true');

    setScrollMetrics(viewport, {
      scrollTop: 200,
      clientHeight: 100,
      scrollHeight: 300,
    });
    fireEvent.scroll(viewport);
    expect(top?.getAttribute('data-visible')).toBe('true');
    expect(bottom?.getAttribute('data-visible')).toBe('false');
  });

  it('shows no shadow when the content fits', () => {
    const { container } = render(
      <ScrollArea>
        <p>content</p>
      </ScrollArea>,
    );
    const viewport = container.querySelector(
      '.wayf-scroll-area__viewport',
    ) as HTMLElement;
    setScrollMetrics(viewport, {
      scrollTop: 0,
      clientHeight: 300,
      scrollHeight: 300,
    });
    fireEvent.scroll(viewport);
    const { top, bottom } = shadows(container);
    expect(top?.getAttribute('data-visible')).toBe('false');
    expect(bottom?.getAttribute('data-visible')).toBe('false');
  });
});

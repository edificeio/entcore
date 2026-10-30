import { resolveHomepageWidgets } from './homepageWidgets';

vi.mock('@edifice.io/react/homepage', () => {
  const stub = () => null;
  return {
    AgendaContainer: stub,
    BriefMeContainer: stub,
    CantineContainer: stub,
    CommunitiesContainer: stub,
    EdumalinContainer: stub,
    GenerationHdfContainer: stub,
    QwantContainer: stub,
    RssContainer: stub,
    UniversalisContainer: stub,
    UsefulLinksContainer: stub,
  };
});

const names = (order: { name: string; fullWidth?: boolean }[]) =>
  resolveHomepageWidgets(order).map((widget) => widget.name);

describe('resolveHomepageWidgets', () => {
  it('keeps the order given and the full width flag', () => {
    const widgets = resolveHomepageWidgets([
      { name: 'agenda-widget' },
      { name: 'communities-widget', fullWidth: true },
    ]);

    expect(widgets.map((widget) => widget.name)).toEqual([
      'agenda-widget',
      'communities-widget',
    ]);
    expect(widgets.map((widget) => widget.fullWidth)).toEqual([false, true]);
  });

  it('ignores the widgets the homepage does not handle', () => {
    expect(names([{ name: 'agenda-widget' }, { name: 'mood' }])).toEqual([
      'agenda-widget',
    ]);
  });

  it('keeps only the first entry of a repeated widget', () => {
    const widgets = resolveHomepageWidgets([
      { name: 'agenda-widget' },
      { name: 'rss-widget' },
      { name: 'agenda-widget', fullWidth: true },
    ]);

    expect(widgets.map((widget) => widget.name)).toEqual([
      'agenda-widget',
      'rss-widget',
    ]);
    expect(widgets[0].fullWidth).toBe(false);
  });

  it.each(['constructor', '__proto__', 'toString', 'hasOwnProperty'])(
    'does not resolve the inherited object key "%s" to a component',
    (name) => {
      expect(names([{ name }, { name: 'agenda-widget' }])).toEqual([
        'agenda-widget',
      ]);
    },
  );
});

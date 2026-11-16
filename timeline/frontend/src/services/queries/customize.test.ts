import { IThemeDesc } from '@edifice.io/client';
import { reorderFonts } from './customize';

const font = (_id: string, displayName: string): IThemeDesc => ({
  _id,
  displayName,
  path: `/assets/themes/${_id}`,
});

const DEFAULT = font('default', 'theme.font.default');
const DYSLEXIC = font('dyslexic', 'theme.font.dyslexic');
const ARIAL = font('arial', 'theme.font.arial');

describe('reorderFonts', () => {
  it('puts the default font first whatever its position in the response', () => {
    expect(reorderFonts([DYSLEXIC, DEFAULT]).map(({ _id }) => _id)).toEqual([
      'default',
      'dyslexic',
    ]);
  });

  it('returns the same order whatever the response order', () => {
    const expected = ['default', 'arial', 'dyslexic'];

    expect(
      reorderFonts([DYSLEXIC, ARIAL, DEFAULT]).map(({ _id }) => _id),
    ).toEqual(expected);
    expect(
      reorderFonts([DEFAULT, DYSLEXIC, ARIAL]).map(({ _id }) => _id),
    ).toEqual(expected);
  });

  it('sorts the remaining fonts when there is no default font', () => {
    expect(reorderFonts([DYSLEXIC, ARIAL]).map(({ _id }) => _id)).toEqual([
      'arial',
      'dyslexic',
    ]);
  });

  it('sorts by _id, not by the i18n displayName key', () => {
    const zoe = font('a-zoe', 'theme.font.zzz');
    const bob = font('b-bob', 'theme.font.aaa');

    expect(reorderFonts([bob, zoe]).map(({ _id }) => _id)).toEqual([
      'a-zoe',
      'b-bob',
    ]);
  });

  it('does not mutate the response', () => {
    const fonts = [DYSLEXIC, ARIAL, DEFAULT];

    reorderFonts(fonts);

    expect(fonts).toEqual([DYSLEXIC, ARIAL, DEFAULT]);
  });
});

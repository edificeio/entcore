/**
 * Test suite for hasBodyContent (IMPULS-6326): an absence message body with
 * only empty blocks or whitespace must not count as content.
 */
import { hasBodyContent } from './useAbsenceModal';

const doc = (...content: unknown[]) => ({ type: 'doc', content });
const paragraph = (...content: unknown[]) => ({ type: 'paragraph', content });
const text = (value: string) => ({ type: 'text', text: value });

describe('hasBodyContent', () => {
  it.each([
    ['undefined', undefined],
    ['null', null],
    ['empty string', ''],
    ['blank string', '   '],
    ['empty doc', { type: 'doc' }],
    ['doc with empty content', doc()],
    ['doc with empty paragraphs', doc(paragraph(), paragraph())],
    ['paragraph with whitespace text', doc(paragraph(text('  \n ')))],
    ['hard breaks only', doc(paragraph({ type: 'hardBreak' }))],
  ])('returns false for %s', (_label, body) => {
    expect(hasBodyContent(body)).toBe(false);
  });

  it.each([
    ['non-blank string', 'hello'],
    ['paragraph with text', doc(paragraph(text('Away until Monday')))],
    ['text after empty paragraphs', doc(paragraph(), paragraph(text('hi')))],
    [
      'nested list item with text',
      doc({
        type: 'bulletList',
        content: [{ type: 'listItem', content: [paragraph(text('x'))] }],
      }),
    ],
    ['image leaf node', doc(paragraph(), { type: 'custom-image' })],
  ])('returns true for %s', (_label, body) => {
    expect(hasBodyContent(body)).toBe(true);
  });
});

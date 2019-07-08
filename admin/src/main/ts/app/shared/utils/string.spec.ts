import { trim } from './string'

describe('string utils', () => {
    describe('trim', () => {
        it('should return "input" when given " input "', () => {
            expect(trim(' input ')).toBe('input');
        });
        it('should return "input" when given " input"', () => {
            expect(trim(' input')).toBe('input');
        });
        it('should return "input" when given "input "', () => {
            expect(trim('input ')).toBe('input');
        });
        it('should return "input" when given "input"', () => {
            expect(trim('input')).toBe('input');
        });
    });
});

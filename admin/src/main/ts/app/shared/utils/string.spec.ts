import { trim } from './string'

fdescribe('string utils', () => {
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
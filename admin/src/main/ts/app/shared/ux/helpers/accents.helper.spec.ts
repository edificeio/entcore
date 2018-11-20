import { removeAccents } from './accents.helper';

describe('accents helper', () => {
    it('should replace é by e when given string été', () => {
        expect(removeAccents('été')).toBe('ete');
    });
});

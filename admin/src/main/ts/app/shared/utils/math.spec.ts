import { toDecimal, getUnit, Unit } from './math';

describe('math utils', () => {
    describe('toDecimal', () => {
        it('should return 10.46 when given number 10.4568', () => {
            expect(toDecimal(10.4568, 2)).toBe(10.46);
        });
    });

    describe('getUnit', () => {
        it('should return the Unit {label: "quota.megabyte", value: Math.pow(1024, 2)} when given number Math.pow(1024, 2) and 2', () => {
            expect(getUnit(Math.pow(1024, 2))).toEqual({label: "quota.megabyte", value: Math.pow(1024, 2)});
        });
    });
}); 
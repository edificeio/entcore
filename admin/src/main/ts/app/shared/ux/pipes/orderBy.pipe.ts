import { Pipe, PipeTransform } from '@angular/core'

@Pipe({ name: 'orderBy' })
export class OrderPipe implements PipeTransform {

    transform(array, sortPredicate, reverseOrder?, compareFn?) {
        if (array == null || !sortPredicate) return array;

        /*** ***/
        let isString = (value) => {
            return typeof value === "string"
        }

        let isFunction = (value) => {
            return typeof value === "function"
        }

        let isArray = (value) => {
            return value && value instanceof Array
        }

        let isObject = (value) => {
            return typeof value === "object"
        }

        let isPrimitive = (value) => {
            switch (typeof value) {
                case 'number': /* falls through */
                case 'boolean': /* falls through */
                case 'string':
                    return true;
                default:
                    return false;
            }
        }

        let hasCustomToString = (value) => {
            return value &&
                typeof value.toString === "function" &&
                    value.toString !== Object.prototype.toString
        }

        let getComparisonObject = (value, index) => {
            // NOTE: We are adding an extra `tieBreaker` value based on the element's index.
            // This will be used to keep the sort stable when none of the input predicates can
            // distinguish between two elements.
            return {
                value: value,
                tieBreaker: { value: index, type: 'number', index: index },
                predicateValues: predicates.map(function(predicate) {
                    return getPredicateValue(predicate.get(value), index);
                })
            };
        }

        let doComparison = (v1, v2) => {
            for (var i = 0, ii = predicates.length; i < ii; i++) {
                var result = compare(v1.predicateValues[i], v2.predicateValues[i]);
                if (result) {
                    return result * predicates[i].descending * descending;
                }
            }

            return compare(v1.tieBreaker, v2.tieBreaker) * descending;
        }

        let processPredicates = (sortPredicates) => {
            return sortPredicates.map(function(predicate) {
                var descending = 1, get = (value) => { return value };

                if (isFunction(predicate)) {
                    get = predicate;
                } else if (isString(predicate)) {
                    if ((predicate.charAt(0) === '+' || predicate.charAt(0) === '-')) {
                        descending = predicate.charAt(0) === '-' ? -1 : 1;
                        predicate = predicate.substring(1);
                    }
                    if (predicate !== '') {
                        get = (value) => { return value[predicate] }
                    }
                }
                return { get: get, descending: descending };
            });
        }

        let objectValue = (value) => {
            // If `valueOf` is a valid function use that
            if (isFunction(value.valueOf)) {
                value = value.valueOf();
                if (isPrimitive(value)) return value;
            }
            // If `toString` is a valid function and not the one from `Object.prototype` use that
            if (hasCustomToString(value)) {
                value = value.toString();
                if (isPrimitive(value)) return value;
            }

            return value;
        }

        let getPredicateValue = (value, index) => {
            var type = typeof value;
            if (value === null) {
                type = 'string';
                value = 'null';
            } else if (type === 'object') {
                value = objectValue(value);
            }
            return { value: value, type: type, index: index };
        }

        let defaultCompare = (v1, v2) => {
            var result = 0;
            var type1 = v1.type;
            var type2 = v2.type;

            if (type1 === type2) {
                var value1 = v1.value;
                var value2 = v2.value;

                if (type1 === 'string') {
                    // Compare strings case-insensitively
                    value1 = value1.toLowerCase();
                    value2 = value2.toLowerCase();
                } else if (type1 === 'object') {
                    // For basic objects, use the position of the object
                    // in the collection instead of the value
                    if (isObject(value1)) value1 = v1.index;
                    if (isObject(value2)) value2 = v2.index;
                }

                if (value1 !== value2) {
                    result = value1 < value2 ? -1 : 1;
                }
            } else {
                result = type1 < type2 ? -1 : 1;
            }

            return result;
        }
        /*** ***/

        if (!isArray(sortPredicate)) {
            sortPredicate = [sortPredicate]
        }
        if (sortPredicate.length === 0) {
            sortPredicate = ['+']
        }

        let predicates = processPredicates(sortPredicate);

        let descending = reverseOrder ? -1 : 1;

        // Define the `compare()` function. Use a default comparator if none is specified.
        let compare = isFunction(compareFn) ? compareFn : defaultCompare;

        // The next three lines are a version of a Swartzian Transform idiom from Perl
        // (sometimes called the Decorate-Sort-Undecorate idiom)
        // See https://en.wikipedia.org/wiki/Schwartzian_transform
        let compareValues = Array.prototype.map.call(array, getComparisonObject);
        compareValues.sort(doComparison);
        array = compareValues.map(function(item) { return item.value; });

        return array;
    }

}

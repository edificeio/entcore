import {Pipe} from '@angular/core';

@Pipe({ name: 'flattenObjArray' })
export class FlattenObjectArrayPipe {

    transform(array: Array<Object>, onlyProps?: Array<String>) {
        if (!array) {
            return [];
        }
        if (onlyProps.length < 1) {
            return array;
        }

        let flattenedArray = Array.from(array);

        const flatten = (array: Array<Object>) => {
            array.forEach(item => {
                for (const prop in item) {
                    const val = item[prop];
                    if (val instanceof Array &&
                            !onlyProps ||
                            onlyProps.indexOf(prop) > -1) {
                        flattenedArray = [...flattenedArray, ...val];
                        flatten(val);
                    }
                }
            });
        };
        flatten(array);

        return Array.from(new Set<Object>(flattenedArray));

    }

}

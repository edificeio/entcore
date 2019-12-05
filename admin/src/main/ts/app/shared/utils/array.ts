export function isEqual(arr1: Array<any>, arr2: Array<any>): boolean {
    if (arr1.length != arr2.length) {
        return false;
    } else {
        for (let i = 0; i < arr1.length; i++) {
            if (arr1[i] != arr2[i]) {
                return false;
            }
        }
        return true;
    }
}

export function includes(arr1: Array<any>, arr2: Array<any>) {
    let res = false;
    arr1.forEach(a => {
        if (isEqual(a, arr2)) {
            res = true;
        }
    });
    return res;
}
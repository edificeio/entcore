/** User Storage and Quota returned by /workspace/quota/user/:userId API. */
export interface Quota {
    'quota': number;
    'storage': number;
}
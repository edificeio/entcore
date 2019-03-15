/** User Storage and Quota returned by /workspace/quota/user/:userId API. */
export interface UsedSpace {
    'quota': number;
    'storage': number;
}
export interface Config {
  maxDepth: number;
  recallDelayMinutes: number;
  getVisibleStrategy: 'all-at-once' | 'filtered';
}

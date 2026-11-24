import { AlloRegion } from '@edifice.io/react/homepage';

/**
 * Demo "Allô Région": shown without the alloregion.access right, its
 * buttons do nothing.
 */
export function AlloRegionDemo() {
  return <AlloRegion onRequestsClick={() => {}} onNewRequestClick={() => {}} />;
}

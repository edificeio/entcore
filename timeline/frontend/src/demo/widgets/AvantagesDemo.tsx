import { AvantagesWidget } from '~/components/AvantagesWidget';

/** Demo "Mes avantages": static content, "Voir plus" does nothing. */
export function AvantagesDemo() {
  return <AvantagesWidget onSeeMore={() => {}} />;
}

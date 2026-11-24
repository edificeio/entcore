import { GenerationHdf, GenerationHdfWallet } from '@edifice.io/react/homepage';

const WALLETS: GenerationHdfWallet[] = [
  { label: 'Manuels et équipements', amount: '64,90 €' },
  { label: 'Équipements numériques', amount: '100,00 €' },
];

/** Demo "Génération #HDF": a card already registered, with its balances. */
export function GenerationHdfDemo() {
  return (
    <GenerationHdf
      handleActionClick={() => {}}
      status="account"
      cardNumber="475948"
      onCardNumberChange={() => {}}
      onSubmit={(event) => event.preventDefault()}
      onEdit={() => {}}
      wallets={WALLETS}
    />
  );
}

import './Level2Stub.css';

interface Level2StubProps {
  onBack: () => void;
}

export const Level2Stub = ({ onBack }: Level2StubProps) => {
  return (
    <div className="wayf-level2-stub">
      <button className="wayf-btn-back" onClick={onBack} type="button">
        ← Retour
      </button>
      <p>Niveau 2 — à implémenter</p>
    </div>
  );
};

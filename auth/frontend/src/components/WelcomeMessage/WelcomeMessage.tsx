import type { WelcomeState } from '~/models/welcome';
import './WelcomeMessage.css';

interface WelcomeMessageProps {
  state: WelcomeState;
}

export const WelcomeMessage = ({ state }: WelcomeMessageProps) => {
  if (state.status !== 'ready') return <div />;

  return (
    <aside className="wayf-welcome">
      <div
        className="wayf-welcome__scroll"
        dangerouslySetInnerHTML={{ __html: state.html }}
      />
    </aside>
  );
};

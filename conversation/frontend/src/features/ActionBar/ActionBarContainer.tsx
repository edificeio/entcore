import { ActionBar } from '@edifice-ui/react';
import { useTransition, animated } from '@react-spring/web';
import { ReactNode } from 'react';

export interface ActionBarContainerProps {
  visible: boolean;
  children: ReactNode;
}

export const ActionBarContainer = ({
  visible,
  children,
}: ActionBarContainerProps) => {
  const transition = useTransition(visible, {
    from: { opacity: 0, transform: 'translateY(100%)' },
    enter: { opacity: 1, transform: 'translateY(0)' },
    leave: { opacity: 0, transform: 'translateY(100%)' },
  });

  return transition(
    (style, visible) =>
      visible && (
        <animated.div
          className="position-fixed bottom-0 start-0 end-0 z-3"
          style={style}
        >
          <ActionBar>{children}</ActionBar>
        </animated.div>
      ),
  );
};

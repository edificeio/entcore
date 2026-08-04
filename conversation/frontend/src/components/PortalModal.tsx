import { Modal, ModalElement, ModalProps } from '@edifice.io/react';
import { Ref, forwardRef } from 'react';
import { createPortal } from 'react-dom';

/**
 * `Modal` rendered into the `#portal` DOM node instead of wherever it is
 * mounted in the React tree. Lets a modal be triggered from several,
 * unrelated places (e.g. a menu item and a banner button) by mounting one
 * lightweight, independent instance per trigger — each with its own local
 * `isOpen` state — instead of lifting shared state to a common ancestor.
 */
export const PortalModal = forwardRef(
  ({ children, ...otherProps }: ModalProps, ref: Ref<ModalElement>) => {
    const portal =
      (document.getElementById('portal') as HTMLElement) || document.body;

    return createPortal(
      <Modal ref={ref} {...otherProps}>
        {children}
      </Modal>,
      portal,
    );
  },
);

PortalModal.displayName = 'PortalModal';

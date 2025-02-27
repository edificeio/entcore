import { useConfirmModalStore } from "~/hooks/useConfirmModalStore";
import ConfirmModal from "./ConfirmModal";

const ConfirmModalWrapper = () => {
  const { isOpen, id, header, body, variant, okText, koText, onSuccess, onCancel } =
    useConfirmModalStore();

  if (!isOpen) return null;

  return (
    <ConfirmModal
      id={id}
      isOpen={isOpen}
      header={header}
      body={body}
      variant={variant}
      okText={okText}
      koText={koText}
      onSuccess={onSuccess}
      onCancel={onCancel}
    />
  );
};

export default ConfirmModalWrapper;

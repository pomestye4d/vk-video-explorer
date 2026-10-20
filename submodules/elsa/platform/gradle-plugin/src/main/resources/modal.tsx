import React, { useState, useEffect, ReactNode } from 'react';
import ReactDOM from 'react-dom';
import { BaseReactUiElement } from './component';

interface ModalProps {
    isOpen: boolean;
    onClose: () => void;
    title?: string;
    children: ReactNode;
}

interface ModalPortalProps {
    children: ReactNode;
}

const ModalPortal: React.FC<ModalPortalProps> = ({ children }) => {
    const [modalRoot, setModalRoot] = useState<HTMLElement | null>(null);

    useEffect(() => {
        const root = document.createElement('div');
        root.id = 'modal-root';
        document.body.appendChild(root);
        setModalRoot(root);

        return () => {
            if (document.body.contains(root)) {
                document.body.removeChild(root);
            }
        };
    }, []);

    if (!modalRoot) return null;

    return ReactDOM.createPortal(children, modalRoot);
};

export const AdvancedModal: React.FC<ModalProps> = ({
    isOpen,
    onClose,
    title,
    children,
}) => {
    useEffect(() => {
        const handleEscape = (e: KeyboardEvent) => {
            if (e.key === 'Escape') onClose();
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    return (
        <ModalPortal>
            <div className="modal-overlay" onClick={onClose}>
                <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                    <div className="modal-header">
                        {title && <h2>{title}</h2>}
                        <button
                            className="modal-close"
                            onClick={onClose}
                            aria-label="Закрыть диалоговое окно"
                        >
                            ×
                        </button>
                    </div>
                    <div className="modal-body">{children}</div>
                </div>
            </div>
        </ModalPortal>
    );
};

interface MainModalProps {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    content?: BaseReactUiElement;
    buttons?: BaseReactUiElement[];
}

export const Modal: React.FC<MainModalProps> = ({
    isOpen,
    onClose,
    content,
    buttons,
    title,
}) => {
    return isOpen ? (
        <ModalPortal>
            <div className="modal-overlay" onClick={onClose}>
                <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                    <div className="modal-header">
                        <h2>{title}</h2>
                        <button
                            className="modal-close"
                            onClick={onClose}
                            aria-label="Закрыть диалоговое окно"
                        >
                            ×
                        </button>
                    </div>
                    <div className="modal-body" style={{ height: '420px' }}>
                        {content?.createReactElement() ?? ''}
                    </div>
                    <div className="modal-footer">
                        {buttons?.map((it) => it.createReactElement()) ?? ''}
                    </div>
                </div>
            </div>
        </ModalPortal>
    ) : null;
};

interface UseModalReturn {
    isOpen: boolean;
    open: () => void;
    close: () => void;
}

export const useModal = (): UseModalReturn => {
    const [isOpen, setIsOpen] = useState<boolean>(false);

    const open = (): void => {
        setIsOpen(true);
    };
    const close = (): void => setIsOpen(false);

    return { isOpen, open, close };
};

// components/ToastNotification.tsx
import React, { useEffect } from 'react';
import { NotificationType } from '@g/common/NotificationType';

interface NotificationProps {
    isOpen: boolean;
    onClose: () => void;
    message: string;
    duration?: number;
    type?: NotificationType;
}

export const NotificationFC: React.FC<NotificationProps> = ({
    isOpen,
    onClose,
    message,
    duration = 2000,
    type = 'INFO',
}) => {
    useEffect(() => {
        if (!isOpen) return;

        const timer = setTimeout(() => {
            onClose();
        }, duration);

        return () => clearTimeout(timer);
    }, [isOpen, onClose, duration]);

    if (!isOpen) return null;

    const getIcon = () => {
        switch (type) {
            case 'INFO':
                return '✅';
            case 'ERROR':
                return '❌';
            case 'WARN':
                return '⚠️';
            default:
                return '💡';
        }
    };

    const getClassName = () => {
        return `toast-notification toast-${type}`;
    };

    return (
        <div className={getClassName()}>
            <span className="toast-icon">{getIcon()}</span>
            <span className="toast-message">{message}</span>
        </div>
    );
};

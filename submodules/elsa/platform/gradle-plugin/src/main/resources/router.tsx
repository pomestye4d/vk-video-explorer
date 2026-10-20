import { createContext, useContext } from 'react';

export interface RouterContextType {
    hasChanges: boolean;
    setHasChanges: (value: boolean) => void;
    confirmMessage?: string;
    setConfirmMessage: (value?: string) => void;
}

export const RouterContext = createContext<RouterContextType | undefined>(undefined);

export const useRouter = (): RouterContextType | undefined => {
    return useContext(RouterContext);
};

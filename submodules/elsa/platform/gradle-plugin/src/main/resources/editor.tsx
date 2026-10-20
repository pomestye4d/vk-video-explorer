import { createContext, useContext } from 'react';

export interface EditorContextType {
    hasChanges: boolean;
    setHasChanges: (value: boolean) => void;
}

export const EditorContext = createContext<EditorContextType | undefined>(undefined);

export const useEditor = (): EditorContextType | undefined => {
    return useContext(EditorContext);
};

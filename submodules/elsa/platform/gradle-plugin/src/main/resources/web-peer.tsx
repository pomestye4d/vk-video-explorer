import {
    BaseUiElement,
    PreloaderMiddleware,
    webpeerExt,
    WebPeerExtension,
} from 'webpeer-core';
import { createRoot, Root } from 'react-dom/client';
import { BaseReactUiElement } from './component';

export interface ReactUiElementFactory {
    createElement(model: any): BaseReactUiElement;
}

export type ReactWebPeerExtension = WebPeerExtension & {
    elementHandlersFactories: Map<string, ReactUiElementFactory>;
};
export const reactWebPeerExt = webpeerExt as ReactWebPeerExtension;

reactWebPeerExt.elementHandlersFactories = new Map();

export const registerFactory = (type: string, factory: ReactUiElementFactory) => {
    webpeerExt.elementTypes = webpeerExt.elementTypes || [];
    webpeerExt.elementTypes.push(type);
    reactWebPeerExt.elementHandlersFactories.set(type, factory);
};

let root: Root | null = null;

export const preloaderHolder = {
    showPreloader: () => {},
    hidePreloader: () => {},
};

reactWebPeerExt.setMiddleware([
    new PreloaderMiddleware(
        {
            showPreloader() {
                preloaderHolder.showPreloader();
            },
            hidePreloader() {
                preloaderHolder.hidePreloader();
            },
        },
        {
            delay: 300,
        }
    ),
]);
reactWebPeerExt.uiHandler = {
    drawUi(rootElm: BaseReactUiElement) {
        if (!root) {
            root = createRoot(document.getElementById('root') as Element);
        }
        root.render(rootElm.createReactElement());
    },
    createElement(model: any): BaseUiElement {
        return reactWebPeerExt.elementHandlersFactories
            .get(model.type)!
            .createElement(model);
    },
    handleServerUpdate() {
        if (confirm('Server was updated, reload?')) {
            window.location.reload();
        }
    },
};

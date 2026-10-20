import {
    API,
    BaseUiElement,
    generateUUID,
    PreloaderMiddleware,
    webpeerExt,
} from 'webpeer-core';
import 'web-admin';
import {
    BaseReactUiElement,
    preloaderHolder,
    reactWebPeerExt,
    registerFactory,
} from 'admin/src/common/component';
import { createRoot, Root } from 'react-dom/client';
import { JTasksMainFrameComponent } from 'jtasks/src/webApp/JTasksMainFrame';
import 'jtasks/src-gen/registry/RegistryConfigurator';
import 'jtasks/src/index.css';

registerFactory('ru.vga.jtasks.mainFrame.JTasksMainFrame', {
    createElement: (model: any) => new JTasksMainFrameComponent(model),
});

reactWebPeerExt.api = new API({
    clientId: generateUUID(),
    restPath: webpeerExt.parameters.restPath,
    webSocketUrl: webpeerExt.parameters.webSocketUrl,
});

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

let root: Root | null = null;
let rootElement: BaseReactUiElement | null = null;

let reloadCallback: () => Promise<void> = async () => {};

reactWebPeerExt.uiHandler = {
    handleRemotingError(message: string, details: string): void {
        (rootElement as any).showErrorDialog(message, details);
    },
    drawUi(rootElm: BaseReactUiElement) {
        if (!root) {
            root = createRoot(document.getElementById('root') as Element);
        }
        rootElement = rootElm;
        root.render(rootElm.createReactElement());
    },
    createElement(model: any): BaseUiElement {
        return reactWebPeerExt.elementHandlersFactories
            .get(model.type)!
            .createElement(model);
    },
    handleServerUpdate() {
        reloadCallback();
    },
};

async function init() {
    await webpeerExt.api.sendCommandAsync(
        'root',
        'init',
        {
            ls: JSON.parse(window.localStorage.getItem('webpeer') || '{}'),
            params: {
                windowWidth: window.innerWidth,
                windowHeight: window.innerHeight,
                ...((window as any).webPeer?.parameters || {}),
            },
            state: JSON.parse(window.localStorage.getItem('webpeer-state') || '{}'),
        },
        false
    );
}
window.addEventListener('pagehide', function () {
    webpeerExt.api.destroy();
});
document.addEventListener('DOMContentLoaded', init);

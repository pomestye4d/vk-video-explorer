import React, { ReactElement, useEffect, useState } from 'react';
import {
    BaseUiElement,
    PreloaderMiddleware,
    webpeerExt,
    WebPeerExtension,
} from 'webpeer-core';
import { createRoot, Root } from 'react-dom/client';

export interface ReactUiElementFactory {
    createElement(model: any): BaseReactUiElement;
}

export type ReactWebPeerExtension = WebPeerExtension & {
    elementHandlersFactories: Map<string, ReactUiElementFactory>;
};
export const reactWebPeerExt = webpeerExt as ReactWebPeerExtension;

reactWebPeerExt.elementHandlersFactories = new Map();

export type InputType = 'SELECT' | 'TEXT_FIELD' | 'TEXT_AREA';

export type InputDescription = {
    inputType: InputType;
    id: string;
};

export function initStateSetters(element: BaseReactUiElement) {
    for (const prop of [...element.state.keys(), 'counter']) {
        const [value, setValue] = useState(element.state.get(prop));
        element.state.set(prop, value);
        element.stateSetters.set(prop, setValue);
    }
    useEffect(() => {
        element.state.forEach((value, key) => {
            element.stateSetters.get(key)?.(value);
        });
    }, [element]);
}

export type ReactElementDescription = {
    state: string[];
    actionsFromClient: string[];
    input?: InputType;
    services: string[];
};
export abstract class BaseReactUiElement extends BaseUiElement {
    readonly description: ReactElementDescription;

    state = new Map<string, any | null | undefined>();

    counter = 1;

    stateSetters: Map<string, (value?: any) => void> = new Map<
        string,
        (value?: any) => void
    >();

    createReactElement(): React.ReactElement {
        return React.createElement(this.functionalComponent, {
            element: this,
            key: this.id,
        });
    }

    abstract functionalComponent: (props: any) => ReactElement;

    protected constructor(description: ReactElementDescription, model: any) {
        super(model);
        this.description = description;
        (model.children || []).forEach((ch: any) => {
            const elm = reactWebPeerExt.elementHandlersFactories
                .get(ch.type)!
                .createElement(ch);
            elm.parent = this;
            elm.init();
            this.children = this.children || [];
            this.children.push(elm);
        });
        description.state.forEach((key) => this.state.set(key, model[key]));
        this.state.set('counter', this.counter);
    }

    public findByTag(tag: string) {
        return this.children?.find((it) => it.tag === tag) as BaseReactUiElement;
    }

    getState(): any {
        const result = super.getState();
        this.state.forEach((value, key) => {
            result[key] = value;
        });
        return result;
    }

    redraw() {
        this.counter++;
        this.stateSetters.get('counter')!(this.counter);
    }

    processCommandFromServer(commandId: string, data?: any) {
        if (commandId === 'pc') {
            this.updatePropertyValue(data!.pn, data!.pv);
            return;
        }
        super.processCommandFromServer(commandId, data);
    }

    protected updatePropertyValue(pn: string, pv: any) {
        this.state.set(pn, pv);
        this.stateSetters.get(pn)?.call(this, pv);
    }
}

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

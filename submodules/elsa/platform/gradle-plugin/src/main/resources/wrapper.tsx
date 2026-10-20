import React, { useState } from 'react';

import './styles.css';
import { BaseReactUiElement } from './component';
import { Tabs } from './tabs';
import { AdvancedModal } from './modal';
import { useModal } from './modal';

interface DetailsModalProps {
    isOpen: boolean;
    onClose: () => void;
    element: BaseReactUiElement;
}

const toString = (data: Map<string, any | undefined>) => {
    const obj = {} as any;
    data.forEach((value, key) => (obj[key] = value));
    return JSON.stringify(obj, null, 3);
};

export const DetailsModal: React.FC<DetailsModalProps> = ({
    isOpen,
    onClose,
    element,
}) => {
    const state = toString(element.state);
    const payloadValues = new Map<string, string | undefined>();
    const payloadSetters = new Map<string, any>();
    const [inputValue, setInputValue] = useState<string>(
        element.state.get('value') && JSON.stringify(element.state.get('value'))
    );
    for (const act of element.description.actionsFromClient) {
        const [v, setV] = useState<string | undefined>();
        payloadValues.set(act, v);
        payloadSetters.set(act, setV);
    }
    return (
        <AdvancedModal isOpen={isOpen} onClose={onClose} title="Details">
            <Tabs
                tabs={[
                    {
                        id: 'state',
                        label: 'State',
                        content: (
                            <textarea
                                name="state"
                                disabled
                                style={{ width: '100%', height: '400px' }}
                                value={state}
                            />
                        ),
                    },
                    {
                        id: 'actions',
                        label: 'Actions',
                        content: (
                            <div
                                key="actions"
                                style={{
                                    width: '100%',
                                    height: '400px',
                                    overflowY: 'auto',
                                }}
                            >
                                {element.description.actionsFromClient.map((action) => (
                                    <div
                                        key={action}
                                        className="webpeer-action-container"
                                    >
                                        <button
                                            key="button"
                                            className="webpeer-button"
                                            onClick={() =>
                                                element.sendCommand(
                                                    action,
                                                    payloadValues.get(action) == null
                                                        ? null
                                                        : JSON.parse(
                                                              payloadValues.get(action)!
                                                          ),
                                                    false
                                                )
                                            }
                                        >
                                            {action}
                                        </button>
                                        <textarea
                                            key="text-area"
                                            className="webpeer-action-textarea"
                                        ></textarea>
                                    </div>
                                ))}
                            </div>
                        ),
                    },
                    {
                        id: 'input',
                        label: 'Input',
                        content: (
                            <div
                                key="input"
                                style={{
                                    width: '100%',
                                    height: '400px',
                                    overflowY: 'auto',
                                }}
                            >
                                {element.description.input ? (
                                    <div
                                        key="container"
                                        className="webpeer-inputs-container"
                                    >
                                        <button
                                            key="button"
                                            className="webpeer-button"
                                            onClick={() =>
                                                (element as any).setValue(
                                                    inputValue
                                                        ? JSON.parse(inputValue)
                                                        : null
                                                )
                                            }
                                        >
                                            Commit
                                        </button>
                                        <textarea
                                            value={inputValue}
                                            key="text-area"
                                            className="webpeer-action-textarea"
                                            onChange={(e) => {
                                                setInputValue(e.target.value);
                                            }}
                                        ></textarea>
                                    </div>
                                ) : (
                                    ''
                                )}
                            </div>
                        ),
                    },
                ]}
                key="tabs"
                defaultActiveTab="state"
            />
        </AdvancedModal>
    );
};

export function WebComponentWrapper(
    props: React.PropsWithChildren<{ element: BaseReactUiElement }>
) {
    const modal = useModal();
    return (
        <div
            className="webpeer-container"
            key={props.element.id}
            style={{
                display: 'flex',
                flexDirection: 'column',
            }}
        >
            <div
                className="webpeer-container-header"
                key="header"
                style={{
                    display: 'flex',
                    flexDirection: 'row',
                }}
            >
                <div style={{ flexGrow: 0 }} key="id">
                    {props.element.tag}
                </div>
                <div style={{ flexGrow: 1 }} key="glue" />
                <button
                    className="webpeer-details-button"
                    style={{ flexGrow: 0 }}
                    onClick={() => {
                        modal.open();
                    }}
                >
                    Details
                </button>
            </div>
            {props.children}
            <DetailsModal
                element={props.element}
                key="details"
                isOpen={modal.isOpen}
                onClose={modal.close}
            />
        </div>
    );
}

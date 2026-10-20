import React, { useState } from 'react';

// Типы для табов
interface Tab {
    id: string;
    label: string;
    content: React.ReactNode;
}

interface TabsProps {
    tabs: Tab[];
    defaultActiveTab?: string;
}

export const Tabs: React.FC<TabsProps> = ({ tabs, defaultActiveTab }) => {
    const [activeTab, setActiveTab] = useState(defaultActiveTab || tabs[0]?.id || '');

    if (tabs.length === 0) {
        return <div>Нет доступных табов</div>;
    }

    const activeTabContent = tabs.find((tab) => tab.id === activeTab)?.content;

    return (
        <div className="webpeer-tab-container">
            {/* Панель с кнопками табов */}
            <div className="webpeer-tab-header" role="tablist">
                {tabs.map((tab) => (
                    <button
                        key={tab.id}
                        className={`webpeer-tab-button ${activeTab === tab.id ? 'active' : ''}`}
                        onClick={() => setActiveTab(tab.id)}
                        role="tab"
                        aria-selected={activeTab === tab.id}
                        aria-controls={`tabpanel-${tab.id}`}
                        id={`tab-${tab.id}`}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>

            {/* Контент активного таба */}
            <div
                className="webpeer-tab-content"
                role="tabpanel"
                id={`tabpanel-${activeTab}`}
                aria-labelledby={`tab-${activeTab}`}
            >
                {activeTabContent}
            </div>
        </div>
    );
};

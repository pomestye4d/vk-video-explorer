import React from 'react';

export const Loader: React.FC<{}> = () => {
    return (
        <div className={'loader-container'}>
            <div
                className={'loader medium'}
                style={{ borderTopColor: '#007bff' }}
            ></div>
            {<p className="loader-text">loading</p>}
        </div>
    );
};

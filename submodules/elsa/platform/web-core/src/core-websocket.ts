/* eslint-disable no-plusplus,no-console */
export type WebSocketParams = {
    connectUrl: string
}

export type SubscriptionParams = {
    remoting: string;
    group: string;
    subscription: string;
    parameter?: any;
    handler: (event: any | undefined) => Promise<void> | void
}

type CallData = {
    subscribe: boolean,
    subscriptionId?: number,
    handler?: (event: any | undefined) => Promise<void> | void,
    resolve: (result: any | undefined) => void
    reject: (error: any | undefined) => void
}

export class WebSocketFacade {
    private params: WebSocketParams;

    private socket: WebSocket | null = null;

    private openingSocket = false;

    private callId: number = 0;

    private calls: Map<number, CallData> = new Map();

    private subscriptions: Map<number, (event: any | undefined) => Promise<void> | void> = new Map();

    private isDebugEnabled() {
      return (window as any).webSocketDebug ?? false;
    }

    constructor(params: WebSocketParams) {
      this.params = params;
      (window as any).webSocketDebug = window.localStorage.getItem('webSocketDebug') === 'true';
    }

    private cleanupQueue(reason: string) {
      try {
        this.openingSocket = false;
        this.subscriptions.clear();
        this.calls.forEach((value) => value.reject && value.reject(reason));
        this.calls.clear();
      } catch (e) {
        // noop
      }
    }

    private openSocketPromise: Promise<void>| null = null;

    private async ensureSocketOpen() {
      if (this.socket) {
        return;
      }
      if (this.openSocketPromise) {
        await this.openSocketPromise;
        return;
      }
      this.openingSocket = true;
      this.openSocketPromise = new Promise<void>((resolve, reject) => {
        this.socket = new WebSocket(this.params.connectUrl);
        this.socket.onopen = () => {
          if (this.isDebugEnabled()) {
            console.debug('[ws open]');
          }
          resolve();
        };
        this.socket.onclose = (event) => {
          if (this.isDebugEnabled()) {
            console.error(`[ws close] code =${event.code} reason=${event.reason}`);
          }
          this.cleanupQueue('closing socket');
          this.socket = null;
          reject();
        };
        this.socket.onmessage = (event) => {
          const content = event.data as string;
          const data = JSON.parse(content);
          if (data.callId) {
            if (this.isDebugEnabled()) {
              console.debug('got message for call', data.callId);
            }
            const call = this.calls.get(data.callId);
            if (!call) {
              if (this.isDebugEnabled()) {
                console.error('unable to find appropriate call');
              }
              return;
            }
            this.calls.delete(data.callId);
            if (call.subscribe) {
              if (!data.subscriptionId) {
                if (this.isDebugEnabled()) {
                  console.error('wrong subscription id', data.subscriptionId);
                }
                call.reject('no subscription id');
                return;
              }
              if (this.isDebugEnabled()) {
                console.log('added subscription with id', data.subscriptionId);
              }
              this.subscriptions.set(data.subscriptionId, call.handler!);
              call.resolve(data.subscriptionId);
              return;
            }
            if (call.subscriptionId) {
              this.subscriptions.delete(call.subscriptionId);
            }
            if (this.isDebugEnabled()) {
              console.log('successfully unsubscribed from ', call.subscriptionId);
            }
            call.resolve(undefined);
            return;
          }
          const handler = this.subscriptions.get(data.subscriptionId);
          if (!handler) {
            if (this.isDebugEnabled()) {
              console.error(`no subscription found for id ${data.subscriptionId}`);
            }
            return;
          }
          if (this.isDebugEnabled()) {
            console.debug('processing message for subscription', data.subscriptionId);
          }
          handler(data.event);
        };
        this.socket.onerror = (error) => {
          if (this.isDebugEnabled()) {
            console.error('[ws error]', error);
          }
          if (this.openingSocket) {
            reject(error);
          }
          this.cleanupQueue('socket error');
        };
      });
      await this.openSocketPromise;
    }

    async subscribe(params: SubscriptionParams): Promise<number> {
      await this.ensureSocketOpen();
      return await new Promise<number>((resolve, reject) => {
        if (this.socket) {
          this.callId++;
          const callId = this.callId;
          const callData: CallData = {
            subscribe: true,
            handler: params.handler,
            reject,
            resolve,
          };
          this.calls.set(callId, callData);
          this.socket.send(JSON.stringify({
            operation: 'subscribe',
            callId,
            remoting: params.remoting,
            group: params.group,
            subscription: params.subscription,
            parameter: params.parameter,
          }));
          if (this.isDebugEnabled()) {
            console.log('subscription request sent', callId);
          }
          return;
        }
        reject(new Error('socket is closed'));
      });
    }

    async unsubscribe(subscriptionId: number): Promise<void> {
      this.subscriptions.delete(subscriptionId);
      if (!this.socket) {
        if (this.isDebugEnabled()) {
          console.warn('unable to unsubscribe because socket is closed');
        }
        return;
      }
      await new Promise<void>((resolve, reject) => {
        try {
          if (this.socket) {
            this.callId++;
            const callId = this.callId;
            const callData: CallData = {
              subscriptionId,
              subscribe: false,
              reject,
              resolve,
            };
            this.calls.set(callId, callData);
            this.socket.send(JSON.stringify({
              operation: 'unsubscribe',
              callId,
              subscriptionId,
            }));
            if (this.isDebugEnabled()) {
              console.log(`unsubscribe request sent: callId=${callId} subscriptionId=${subscriptionId}`);
            }
            return;
          }
          if (this.isDebugEnabled()) {
            console.warn('unable to unsubscribe because socket is closed');
          }
          resolve();
        } catch (e) {
          reject(e);
        }
      });
    }
}

export class SubsequentSubscriptionsManager {
    private delegate: WebSocketFacade;

    private subscriptions: Map<string, number> = new Map<string, number>();

    private currentPromise: Promise<void>| null = null;

    constructor(facade: WebSocketFacade) {
      this.delegate = facade;
    }

    private toKey(remoting: string, group: string, subscription: string): string {
      return `${remoting}:${group}:${subscription}`;
    }

    async subscribe(params: SubscriptionParams): Promise<void> {
      if (this.currentPromise) {
        await this.currentPromise;
      }
      const key = this.toKey(params.remoting, params.group, params.subscription);
      // eslint-disable-next-line no-async-promise-executor
      this.currentPromise = new Promise<void>(async (resolve, reject) => {
        try {
          let subscriptionId = this.subscriptions.get(key);
          if (subscriptionId) {
            await this.delegate.unsubscribe(subscriptionId);
            this.subscriptions.delete(key);
          }
          subscriptionId = await this.delegate.subscribe(params);
          this.subscriptions.set(key, subscriptionId);
          this.currentPromise = null;
          resolve();
        } catch (e) {
          reject(e);
        }
      });
      await this.currentPromise;
    }

    async unsubscribe(remoting: string, group: string, subscription: string) : Promise<void> {
      if (this.currentPromise) {
        await this.currentPromise;
      }
      const key = this.toKey(remoting, group, subscription);
      // eslint-disable-next-line no-async-promise-executor
      this.currentPromise = new Promise<void>(async (resolve, reject) => {
        try {
          const subscriptionId = this.subscriptions.get(key);
          if (subscriptionId) {
            await this.delegate.unsubscribe(subscriptionId);
          }
          this.subscriptions.delete(key);
          this.currentPromise = null;
          resolve();
        } catch (e) {
          reject(e);
        }
      });
      await this.currentPromise;
    }
}

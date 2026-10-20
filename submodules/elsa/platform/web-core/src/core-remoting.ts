import { getServiceDescription, HTTPHeaders, HTTPMethod } from './core-metadata-provider';
import { deserializeFromJson, serializeToJson, serializeToQueryString } from './core-serialization';
import {
  generateUUID, isBlank, isNotBlank, isNull,
} from './core-utils';

export type ServiceData = {
  serviceId:string;
  request?: any,
  rawResponse?: Response
  response?: any;
  context?: Map<string, any|null>
}

export type RemotingGroup = {
  remotingId: string,
  groupId: string,
}
export class ResponseError extends Error {
  override name: 'ResponseError' = 'ResponseError';

  response: Response

  constructor(public resp: Response, msg?: string) {
    // noinspection TypeScriptValidateTypes
    super(msg);
    this.response = resp;
  }
}

export class FetchError extends Error {
  override name: 'FetchError' = 'FetchError';

  cause: Error

  constructor(public aCause: Error, msg?: string) {
    // noinspection TypeScriptValidateTypes
    super(msg);
    this.cause = aCause;
  }
}

export type Middleware = {
  advice: (request: ServiceData, callback: (req: ServiceData) => Promise<ServiceData>) => Promise<ServiceData>
  priority: number
}

export type PreloaderHandler = {
  showPreloader: ()=>void
  hidePreloader: () => void
}

export type PreloaderParams = {
  delay: number
  priorityValue?:number
}
export class PreloaderMiddleware implements Middleware {
  protected preloaderShown = false

  protected operations: string[] = []

  constructor(protected handler:PreloaderHandler, protected params:PreloaderParams) {
    this.priority = params.priorityValue ?? 0;
  }

  advice(request: ServiceData, callback: (req: ServiceData) => Promise<ServiceData>): Promise<ServiceData> {
    return new Promise<ServiceData>((resolve, reject) => {
      const operationId = generateUUID();
      try {
        this.operations.push(operationId);
        setTimeout(() => this.showPreloader(), this.params.delay);
        callback(request).then((result) => {
          resolve(result);
        }).catch((e) => reject(e));
      } catch (e) {
        reject(e);
      } finally {
        this.operations.slice(this.operations.indexOf(operationId), this.operations.indexOf(operationId) + 1);
        this.hidePreloader();
      }
    });
  }

  priority = 0

  private showPreloader() : void {
    if (!this.preloaderShown && this.operations.length > 0) {
      this.handler.showPreloader();
      this.preloaderShown = true;
    }
  }

  private hidePreloader() {
    if (this.preloaderShown && this.operations.length > 0) {
      this.handler.hidePreloader();
      this.preloaderShown = false;
    }
  }
}
export class Configuration {
  // eslint-disable-next-line no-useless-constructor
  constructor(private configuration: ConfigurationParameters) {
    // noops
  }

  get basePath(): string {
    if (isBlank(this.configuration.basePath)) {
      throw new Error('base path is not defined');
    }
    return this.configuration.basePath!!;
  }

  get middleware(): Middleware[] {
    return this.configuration.middleware || [];
  }

  get headers(): HTTPHeaders | undefined {
    return this.configuration.headers;
  }
}

export interface ConfigurationParameters {
  basePath?: string; // override base path
  middleware?: Middleware[]; // middleware to apply before/after fetch requests
  headers?: HTTPHeaders // header params we want to use on every request
}

export type Json = any;
export type HTTPBody = Json | FormData | URLSearchParams;
export type HTTPRequestInit = { headers?: HTTPHeaders; method: HTTPMethod; body?: HTTPBody };

export type InitOverrideFunction = (requestContext: HTTPRequestInit) => Promise<HTTPRequestInit>

export class BaseAPI {
  private middleware: Middleware[];

  private sortMiddleware() {
    this.middleware.sort((a, b) => a.priority - b.priority);
  }

  constructor(protected configuration: Configuration, protected group: RemotingGroup) {
    this.middleware = configuration.middleware;
    this.sortMiddleware();
  }

  withMiddleware<T extends BaseAPI>(this: T, ...middlewares: Middleware[]) {
    const next = this.clone<T>();
    next.middleware = next.middleware.concat(...middlewares);
    this.sortMiddleware();
    return next;
  }

  protected async request(request:ServiceData, initOverrides?: HTTPRequestInit | InitOverrideFunction) {
    return await this.requestWithMiddleware(request, 0, initOverrides);
  }

  private async requestWithMiddleware(request:ServiceData, idx: number, initOverrides?: HTTPRequestInit | InitOverrideFunction):Promise<ServiceData> {
    if (isNull(request.context)) {
      request.context = new Map<string, any>();
    }
    if (this.middleware.length === 0 || idx === this.middleware.length) {
      const serviceDescription = await getServiceDescription(this.configuration, this.group.remotingId, this.group.groupId, request.serviceId);
      const headers = { ...this.configuration.headers || {} };
      const httpRequestInit = {
        method: serviceDescription.method,
        headers,
      } as HTTPRequestInit;
      let url = isNotBlank(serviceDescription.path) ? `${this.configuration.basePath}/${this.group.remotingId}/${this.group.groupId}/${serviceDescription.path}` : `${this.configuration.basePath}/${this.group.remotingId}/${this.group.groupId}/${request.serviceId}`;
      if (request.request && serviceDescription.requestClassName && serviceDescription.method !== 'GET') {
        httpRequestInit.body = await serializeToJson(this.configuration, request.request, serviceDescription.requestClassName);
      } else if (request.request && serviceDescription.requestClassName && serviceDescription.method === 'GET') {
        url = `${url}?${await serializeToQueryString(this.configuration, request.request, serviceDescription.requestClassName)}`;
      }
      const initParams = {
        ...httpRequestInit,
      } as HTTPRequestInit;
      let overrides = {};
      if (typeof initOverrides === 'function') {
        overrides = await initOverrides(httpRequestInit);
      } else if (initOverrides) {
        overrides = initOverrides;
      }
      const requestInit = { ...initParams, ...overrides };
      let result: Response;
      try {
        result = await fetch(url, requestInit);
      } catch (e) {
        throw new FetchError(e as Error, 'Response returned an error code');
      }
      request.rawResponse = result;
      if (!result || result.status < 200 || result.status >= 300) {
        throw new ResponseError(result, 'Response returned an error code');
      }
      if (serviceDescription.responseClassName) {
        request.response = await deserializeFromJson(this.configuration, await result.text(), serviceDescription.responseClassName);
      }
      return request;
    }
    const result = await this.middleware[idx].advice(request, (req) => this.requestWithMiddleware(req, idx + 1, initOverrides));
    return result;
  }

  private clone<T extends BaseAPI>(this: T): T {
    const constructor = this.constructor as any;
    const next = new constructor(this.configuration);
    next.middleware = this.middleware.slice();
    return next;
  }
}

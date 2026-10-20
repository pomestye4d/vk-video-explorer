import { HTTPRequestInit } from './core-remoting';

export type HTTPHeaders = { [key: string]: string };
export type HTTPMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE' | 'OPTIONS' | 'HEAD';

export type Configuration = {
    basePath?: string; // override base path
    headers?: HTTPHeaders; // header params we want to use on every request
}
export type BaseElementWithId = {
    id: string
}

export type StandardValueType = 'STRING' | 'LOCAL_DATE' | 'LOCAL_DATE_TIME' | 'ENUM'
    | 'BOOLEAN' | 'ENTITY_REFERENCE' | 'LONG' | 'INT' | 'BIG_DECIMAL' | 'INSTANT' | 'UUID'|'ENTITY';

export type L10nMessageParameterDescription = BaseElementWithId & {
    type: StandardValueType,
    collection: boolean
}

export type ServiceDescription = {
    requestClassName?: string;
    responseClassName?: string
    method: HTTPMethod
    path?: string;
    multipartRequest: boolean
}

export type SubscriptionDescription = {
    parameterClassName?: string;
    eventClassName?: string
}

export type EntityPropertyDescription = BaseElementWithId &{
    type: StandardValueType,
    className?: string
}

export type EntityCollectionDescription = BaseElementWithId &{
    elementType: StandardValueType,
    elementClassName?: string
}
export type EntityMapDescription = BaseElementWithId &{
    keyType: StandardValueType,
    keyClassName?: string
    valueType: StandardValueType,
    valueClassName?: string
}

export type EntityDescription = {
    isAbstract: boolean
    properties?:EntityPropertyDescription[]
    collections?:EntityCollectionDescription[]
    maps?: EntityMapDescription[]
}
export type L10nMessageDescription = {
    id: string,
    displayName: string,
    parameters: L10nMessageParameterDescription[],
};

export type L10nMessagesBundleDescription = BaseElementWithId & {
    messages: Map<String, L10nMessageDescription>
}

const services = new Map<string, ServiceDescription>();
const entities = new Map<string, EntityDescription>();
const subscriptions = new Map<string, SubscriptionDescription>();

const request = async (configuration: Configuration, relativeUrl: string) => {
  const headers = { ...configuration.headers || {} };
  const initParams = {
    method: 'GET',
    headers,
  } as HTTPRequestInit;
  const url = `${configuration.basePath}/${relativeUrl}`;
  const response = await fetch(url, initParams);
  const result = await response.json();
  return result;
};

export async function getServiceDescription(configuration: Configuration, remotingId: string, groupId: string, serviceId: string) {
  const id = `${remotingId}/${groupId}/${serviceId}`;
  if (services.has(id)) {
    return services.get(id)!!;
  }
  const description = (await request(configuration, `core/meta/service-description?remotingId=${remotingId}&groupId=${groupId}&serviceId=${serviceId}`)) as ServiceDescription;
  services.set(id, description);
  return description!!;
}

export async function getSubscriptionDescription(configuration: Configuration, remotingId:string, groupId:string, subscriptionId:string) {
  const id = `${remotingId}_${groupId}_${subscriptionId}`;
  if (subscriptions.has(id)) {
    return subscriptions.get(id)!!;
  }
  const description = (await request(configuration, `core/meta/subscription-description?remotingId=${remotingId}&groupId=${groupId}&subscriptionId=${subscriptionId}`)) as SubscriptionDescription;
  subscriptions.set(id, description);
  return description!!;
}

export async function getEntityDescription(configuration: Configuration, entityId:string) {
  if (entities.has(entityId)) {
    return entities.get(entityId)!!;
  }
  const description = (await request(configuration, `core/meta/entity-description?entityId=${entityId}`)).description as EntityDescription;
  entities.set(entityId, description);
  return description!!;
}

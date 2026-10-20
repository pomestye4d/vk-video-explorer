import { Configuration } from './core-remoting';
import { getEntityDescription, StandardValueType } from './core-metadata-provider';
import {
  formatDigit, isNotBlank, isNotNull, isNull,
} from './core-utils';

export async function serializeToQueryString(configuration: Configuration, param: any, className: string) {
  const ed = await getEntityDescription(configuration, className);
  let result = '';
  ed.properties?.forEach((prop) => {
    const propValue = param[prop.id];
    if (isNull(propValue)) {
      return;
    }
    if (isNotBlank(result)) {
      result = `${result}&`;
    }
    result = `${result + prop.id}=${encodeURIComponent(toString(propValue, prop.type))}`;
  });
  ed.collections?.forEach((coll) => {
    const collValue = param[coll.id];
    if (!collValue) {
      return;
    }
    collValue.forEach((item:any) => {
      if (isNotBlank(result)) {
        result = `${result}&`;
      }
      result = `${result + coll.id}=${encodeURIComponent(toString(item, coll.elementType))}`;
    });
  });
  return result;
}

export async function serializeToJson(configuration: Configuration, param: any, className: string) {
  const obj = await toObject(configuration, param, className);
  return JSON.stringify(obj);
}

export async function deserializeFromJson(configuration: Configuration, content: string, className: string) {
  const obj = JSON.parse(content);
  await updateObject(configuration, obj, className);
  return obj;
}

const toString = (value: any, type: StandardValueType): string => {
  switch (type) {
    case 'ENTITY_REFERENCE':
      return value.id;
    case 'INSTANT':
      return (value as Date).toISOString();
    case 'LOCAL_DATE': {
      const dateValue = value as Date;
      return `${formatDigit(dateValue.getFullYear(), 4)}-${formatDigit(dateValue.getMonth() + 1, 2)}-${formatDigit(dateValue.getDate(), 2)}`;
    }
    case 'LOCAL_DATE_TIME': {
      const dateValue = value as Date;
      return `${formatDigit(dateValue.getFullYear(), 4)}-${formatDigit(dateValue.getMonth() + 1, 2)}-${formatDigit(dateValue.getDate(), 2)}T${formatDigit(dateValue.getHours(), 2)}-${formatDigit(dateValue.getMinutes(), 2)}-${formatDigit(dateValue.getSeconds(), 2)}-${formatDigit(dateValue.getMilliseconds(), 3)}`;
    }
    default:
      return value.toString();
  }
};

async function toJsonValue(configuration: Configuration, propValue: any | null | undefined, type: StandardValueType, className: string | undefined) {
  if (isNull(propValue)) {
    return null;
  }
  switch (type) {
    case 'ENTITY_REFERENCE':
    case 'INT':
    case 'LONG':
    case 'BOOLEAN':
      return propValue;
    case 'ENTITY': {
      return await toObject(configuration, propValue, className!!);
    }
    default:
      return toString(propValue, type);
  }
}

async function toObject(configuration: Configuration, object: any, className: string) {
  let ed = await getEntityDescription(configuration, className);
  const result = {} as any;
  const isAbstract = ed.isAbstract;
  if (isAbstract) {
    result._cn = object._cn;
    ed = await getEntityDescription(configuration, object._cn);
  }
  if (ed.properties) {
    for (const prop of ed.properties) {
      const propValue = object[prop.id];
      if (isNotNull(propValue)) {
        result[prop.id] = await toJsonValue(configuration, propValue, prop.type, prop.className);
      }
    }
  }
  if (ed.collections) {
    for (const coll of ed.collections) {
      const collValue = object[coll.id] as Array<any> | null | undefined;
      if (isNotNull(collValue)) {
        const array = [];
        for (const item of collValue!!) {
          array.push(await toJsonValue(configuration, item, coll.elementType, coll.elementClassName));
        }
        result[coll.id] = array;
      }
    }
  }
  if (ed.maps) {
    for (const map of ed.maps) {
      const mapValue = object[map.id] as Map<any, any> | null | undefined;
      if (isNotNull(mapValue)) {
        const array = [];
        for (const [k, v] of mapValue!!) {
          array.push({
            key: await toJsonValue(configuration, k, map.keyType, map.keyClassName),
            value: await toJsonValue(configuration, v, map.valueType, map.valueClassName),
          });
        }
        result[map.id] = array;
      }
    }
  }
  return result;
}

async function fromJsonValue(configuration: Configuration, propValue: any | null | undefined, type: StandardValueType, className: string | undefined) {
  if (isNull(propValue)) {
    return null;
  }
  switch (type) {
    case 'INSTANT':
      return new Date(propValue as string);
    case 'LOCAL_DATE': {
      const dateValue = propValue as string;
      const values = dateValue.split('-');
      const result = new Date();
      result.setFullYear(Number(values[0]), Number(values[1]) - 1, Number(values[2]));
      result.setHours(0, 0, 0, 0);
      return result;
    }
    case 'LOCAL_DATE_TIME': {
      const dateValue = propValue as string;
      const values = dateValue.replace('T', '-').split('-');
      const result = new Date();
      result.setFullYear(Number(values[0]), Number(values[1]) - 1, Number(values[2]));
      result.setHours(Number(values[3]), Number(values[4]), Number(values[5]), Number(values[6]));
      return result;
    }
    case 'ENTITY': {
      return await toObject(configuration, propValue, className!!);
    }
    default:
      return propValue;
  }
}

async function updateObject(configuration: Configuration, obj: any, className: string) {
  const ed = await getEntityDescription(configuration, className);
  if (ed.isAbstract) {
    obj._cn = className;
  }
  if (ed.properties) {
    for (const prop of ed.properties) {
      const propValue = obj[prop.id];
      if (isNotNull(propValue)) {
        obj[prop.id] = await fromJsonValue(configuration, propValue, prop.type, prop.className);
      }
    }
  }
  if (ed.collections) {
    for (const coll of ed.collections) {
      const collValue = obj[coll.id] as Array<any>|null|undefined;
      if (isNotNull(collValue)) {
        const array = [];
        for (const item of collValue!!) {
          array.push(await fromJsonValue(configuration, item, coll.elementType, coll.elementClassName));
        }
        obj[coll.id] = array;
      }
    }
  }
  if (ed.maps) {
    for (const map of ed.maps) {
      const jsonValue = obj[map.id] as Array<{ key: any, value: any }> | null | undefined;
      if (isNotNull(jsonValue)) {
        const objectValue = new Map<any, any>();
        for (const item of jsonValue!!) {
          objectValue.set(
            await fromJsonValue(configuration, item.key, map.keyType, map.keyClassName),
            await fromJsonValue(configuration, item.value, map.valueType, map.valueClassName),
          );
        }
        obj[map.id] = objectValue;
      }
    }
  }
}

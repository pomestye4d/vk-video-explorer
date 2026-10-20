// eslint-disable-next-line import/prefer-default-export
export const generateUUID = () => {
  const s: string[] = [];
  const hexDigits = '0123456789abcdef';
  for (let i = 0; i <= 36; i += 1) {
    const r = Math.round(Math.random() * 0x10);
    const substr = hexDigits.substring(r, r + 1);
    s.push(substr);
  }
  s[14] = '4';
  s[8] = '-';
  s[13] = '-';
  s[18] = '-';
  s[23] = '-';
  return s.join('');
};

export const formatDigit = (number: number, scale: number) => {
  let remaining = Math.round(number);
  let result = '';
  let digit = scale;
  while (digit >= 0) {
    const part = Math.abs(Math.floor(remaining / 10 ** digit));
    result += part;
    digit -= 1;
    remaining -= part * 10 ** digit;
  }
  if (number < 0) {
    return `-${result}`;
  }
  return result;
};

// eslint-disable-next-line max-len
export const str2Bytes = (base64String: string) => {
  const raw = window.atob(base64String);
  const rawLength = raw.length;
  const array = new Uint8Array(new ArrayBuffer(rawLength));
  for (let i = 0; i < rawLength; i += 1) {
    array[i] = raw.charCodeAt(i);
  }
  return array;
};

export const bytes2Str = (u8: Uint8Array) => {
  const decoder = new TextDecoder('utf8');
  return btoa(decoder.decode(u8));
};

export const isBlank = (str: string|null|undefined) => str === null || str === undefined || str === '';
export const isNotBlank = (str: string|null|undefined) => !isBlank(str);

export const isNull = (obj: any| null | undefined) => obj === null || obj === undefined;
export const isNotNull = (obj: any| null | undefined) => !isNull(obj);

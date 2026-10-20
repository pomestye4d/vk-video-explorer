// eslint-disable-next-line import/prefer-default-export,max-classes-per-file,no-unused-vars
// @ts-ignore
export class RegistryItemType<T> {
  id: string;

  constructor(id: string) {
    this.id = id;
  }
}

export interface RegistryItem<T> {
  getType(): RegistryItemType<T>;
  getId(): string;
}

class CoreRegistry {
  private reg = new Map<string, Map<string, RegistryItem<any>>>();

  register(item: RegistryItem<any>) {
    let items = this.reg.get(item.getType().id);
    if (items == null) {
      items = new Map<string, RegistryItem<any>>();
      this.reg.set(item.getType().id, items);
    }
    items.set(item.getId(), item);
  }

  get<T>(itemType: RegistryItemType<T>, id:string) {
    return this.reg.get(itemType.id)?.get(id) as unknown as T|null;
  }

  allOf<T>(itemType: RegistryItemType<T>): T[] {
    const result = this.reg.get(itemType.id)?.values();
    return result ? ((Array.from(result) as any) as [T]) : [];
  }
}

export const registry = new CoreRegistry();

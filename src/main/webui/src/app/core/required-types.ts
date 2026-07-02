/** Makes OpenAPI-generated optional fields required for loaded domain entities. */
export type Loaded<T> = Required<T>;

export const asLoaded = <T>(value: T): Loaded<T> => value as Loaded<T>;

export const asLoadedArray = <T>(values: T[]): Loaded<T>[] => values as Loaded<T>[];

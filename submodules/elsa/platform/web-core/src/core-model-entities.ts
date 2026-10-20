export interface HasClassName {
    _cn: string;
}

export type BaseIdentity = {
    id: number
}

export type VersionInfo = {
    revision: number,
    modifiedBy: string,
    modified: Date,
    comment?: string,
    versionNumber: number
}

export type BaseAsset = BaseIdentity & {
    versionInfo?: VersionInfo,
}

export type BaseDocument = BaseIdentity & {
    versionInfo?: VersionInfo,
}

export type EntityReference = {
    id: number,
    type: string,
    caption?: string
}

export type BinaryData = {
}

export type BaseProjection = {
    document: EntityReference,
    navigationKey?: number
}

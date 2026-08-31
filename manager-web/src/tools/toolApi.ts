import {fetchJson, post, unwrap} from '../api';
import type {ApiResponse, ParameterDefinition, ServiceInstance, ToolMetadata} from '../types';

export interface ToolSummary {
    id: string;
    name: string;
    description?: string;
    serviceName: string;
    owner: string;
    ownerId: string;
    version: number;
    metadata: ToolMetadata;
    favorite: boolean;
    lastOpenTime?: string;
    canRead: boolean;
    canEdit: boolean;
    canInvoke: boolean;
}

export interface ToolForm {
    id: string;
    name: string;
    description?: string;
    serviceName: string;
    owner: string;
    version: number;
    metadata: ToolMetadata;
    parameters: ParameterDefinition[];
    environments: Array<{ value: string; name: string; production: boolean }>;
    allowAllInstances: boolean;
    canRead: boolean;
    canEdit: boolean;
    defaultTimeoutSeconds: number;
}

export const CATALOG_VIEWS = {
    ALL: '全部可用',
    MINE: '我创建的',
    SHARED: '授权给我的',
    FAVORITES: '收藏',
    RECENT: '最近使用'
} as const;
export type CatalogView = keyof typeof CATALOG_VIEWS;
export const OPERATION_LABELS = {UNSPECIFIED: '未标记类型', QUERY: '查询类', OPERATION: '操作类'} as const;

export interface ToolPage {
    items: ToolSummary[];
    nextCursor?: number | null
}

export const toolApi = {
    async list(view: CatalogView, search: string, serviceName: string, cursor: number): Promise<ToolPage> {
        const query = new URLSearchParams({view, search, serviceName, cursor: String(cursor)});
        return unwrap(await fetchJson<ApiResponse<ToolPage>>(`/manager/tools?${query}`));
    },
    async open(id: string): Promise<ToolForm> {
        return unwrap(await fetchJson<ApiResponse<ToolForm>>(`/manager/tools/${encodeURIComponent(id)}`));
    },
    async instances(id: string, environment: string): Promise<ServiceInstance[]> {
        return unwrap(await fetchJson<ApiResponse<ServiceInstance[]>>(
            `/manager/tools/${encodeURIComponent(id)}/instances?${new URLSearchParams({environment})}`));
    },
};

export interface ToolPermissions {
    version: number;
    readerNo?: string;
    editorNo?: string;
    invokerNo?: string;
    allowedEnvironments?: string[];
    allowAllInstances: boolean;
    enabled: boolean;
}

export interface ToolGrants {
    version: number;
    ownerId: string;
    permissions: ToolPermissions
}

export async function loadGrants(id: string): Promise<ToolGrants> {
    return unwrap(await fetchJson<ApiResponse<ToolGrants>>(`/manager/tools/${encodeURIComponent(id)}/grants`));
}

export async function saveGrants(id: string, expectedVersion: number, permissions: ToolPermissions): Promise<number> {
    return unwrap(await post<number>(`/manager/tools/${encodeURIComponent(id)}/grants`, {
        expectedVersion,
        permissions
    }));
}

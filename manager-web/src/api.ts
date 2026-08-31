import type {
    AiAssistAction,
    AiAssistResponse,
    ApiResponse,
    DirectoryNode,
    ExecutionHistory,
    LoginInfo,
    PageResponse,
    RuntimeMetadata,
    ScriptDetail,
    ScriptResourceOverview,
    ScriptRevision,
    ServiceInstance,
    TreeNodeSaveRequest,
} from './types';

export class ApiError extends Error {
    constructor(message: string, readonly rejected: boolean) {
        super(message);
    }
}

export async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(path, {cache: 'no-store', ...init});
    if (!response.ok) {
        throw new ApiError(response.status === 401 ? '登录已失效，请重新登录后刷新此链接' : `请求失败（HTTP ${response.status}）`,
            response.status >= 400 && response.status < 500);
    }
    return response.json() as Promise<T>;
}

export async function post<T>(path: string, body?: unknown): Promise<ApiResponse<T>> {
    return fetchJson<ApiResponse<T>>(path, {
        method: 'POST',
        headers: body === undefined ? undefined : {'Content-Type': 'application/json'},
        body: body === undefined ? undefined : JSON.stringify(body),
    });
}

export function unwrap<T>(response: ApiResponse<T>): T {
    if (!response.success) {
        throw new ApiError(response.msg || '操作失败', true);
    }
    return response.data;
}

export const api = {
    async getLoginInfo(): Promise<LoginInfo> {
        return unwrap(await post<LoginInfo>('/manager/login/getInfo'));
    },

    async assistScript(request: {
        action: AiAssistAction;
        scriptId?: string;
        serviceName?: string;
        script?: string;
        parameterSchema?: string;
        instruction?: string;
    }): Promise<AiAssistResponse> {
        return unwrap(await post<AiAssistResponse>('/manager/ai/assist', request));
    },

    async listServices(): Promise<string[]> {
        return unwrap(await post<string[]>('/manager/service/list'));
    },

    async getDirectoryTree(serviceName: string): Promise<DirectoryNode[]> {
        const query = new URLSearchParams({serviceName});
        return unwrap(await post<DirectoryNode[]>(`/manager/directory/tree?${query}`));
    },

    async getScriptDetail(scriptId: string): Promise<ScriptDetail> {
        return unwrap(await post<ScriptDetail>('/manager/directory/script/detail', {scriptId}));
    },

    async saveTreeNode(request: TreeNodeSaveRequest): Promise<string> {
        return unwrap(await post<string>('/manager/directory/treeNode/save', request));
    },

    async deleteTreeNode(nodeId: string, forceDelete: boolean): Promise<string> {
        return unwrap(await post<string>('/manager/directory/treeNode/delete', {nodeId, forceDelete}));
    },

    async previewScript(scriptId: string, script: string, params: Record<string, unknown>, parameterSchema?: string): Promise<string> {
        return unwrap(await post<string>('/manager/script/preview', {
            script,
            scriptId,
            params: JSON.stringify(params),
            parameterSchema,
        }));
    },

    async listInstances(scriptId: string, environment: string): Promise<ServiceInstance[]> {
        const query = new URLSearchParams({scriptId, environment});
        return unwrap(await fetchJson<ApiResponse<ServiceInstance[]>>(`/manager/service/instances?${query}`));
    },

    async getRuntimeMetadata(scriptId: string, serviceName: string, environment: string, instanceId?: string): Promise<RuntimeMetadata> {
        const query = new URLSearchParams({scriptId, serviceName, environment});
        if (instanceId) query.set('instanceId', instanceId);
        return unwrap(await fetchJson<ApiResponse<RuntimeMetadata>>(`/manager/service/runtime-metadata?${query}`));
    },

    async getResourceOverview(serviceName: string): Promise<ScriptResourceOverview> {
        const query = new URLSearchParams({serviceName});
        return unwrap(await fetchJson<ApiResponse<ScriptResourceOverview>>(`/manager/resources/overview?${query}`));
    },

    async setFavorite(scriptId: string, favorite: boolean): Promise<boolean> {
        return unwrap(await post<boolean>('/manager/resources/favorite', {scriptId, favorite}));
    },

    async getHistory(scriptId: string, page: number, size: number): Promise<PageResponse<ExecutionHistory[]>> {
        const query = new URLSearchParams({scriptId, page: String(page), size: String(size)});
        const response = await fetchJson<PageResponse<ExecutionHistory[]>>(`/manager/script/history?${query}`);
        if (!response.success) {
            throw new Error(response.msg || '获取执行历史失败');
        }
        return response;
    },

    async getScriptRevisions(scriptId: string): Promise<ScriptRevision[]> {
        const query = new URLSearchParams({scriptId});
        return unwrap(await fetchJson<ApiResponse<ScriptRevision[]>>(`/manager/directory/script/revisions?${query}`));
    },

    async restoreScriptRevision(scriptId: string, version: number, expectedVersion: number): Promise<number> {
        return unwrap(await post<number>('/manager/directory/script/revision/restore', {
            scriptId,
            version,
            expectedVersion
        }));
    },
};

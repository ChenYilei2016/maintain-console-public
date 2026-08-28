import type {
  ApiResponse,
  DirectoryNode,
  ExecutionHistory,
  LoginInfo,
  PageResponse,
  ScriptDetail,
  TreeNodeSaveRequest,
} from './types';

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(path, init);
    if (!response.ok) {
        throw new Error(`请求失败（HTTP ${response.status}）`);
    }
    return response.json() as Promise<T>;
}

async function post<T>(path: string, body?: unknown): Promise<ApiResponse<T>> {
    return fetchJson<ApiResponse<T>>(path, {
        method: 'POST',
        headers: body === undefined ? undefined : {'Content-Type': 'application/json'},
        body: body === undefined ? undefined : JSON.stringify(body),
    });
}

function unwrap<T>(response: ApiResponse<T>): T {
    if (!response.success) {
        throw new Error(response.msg || '操作失败');
    }
    return response.data;
}

export const api = {
    async getLoginInfo(): Promise<LoginInfo> {
        return unwrap(await post<LoginInfo>('/manager/login/getInfo'));
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

    async previewScript(script: string, params: Record<string, string>): Promise<string> {
        return unwrap(await post<string>('/manager/script/preview', {script, params: JSON.stringify(params)}));
    },

    async executeScript(request: {
        service: string;
        script: string;
        env: string;
        scriptId: string;
        params: string;
    }): Promise<string> {
        return unwrap(await post<string>('/manager/script/eval', request));
    },

    async getHistory(scriptId: string, page: number, size: number): Promise<PageResponse<ExecutionHistory[]>> {
        const query = new URLSearchParams({scriptId, page: String(page), size: String(size)});
        const response = await fetchJson<PageResponse<ExecutionHistory[]>>(`/manager/script/history?${query}`);
        if (!response.success) {
            throw new Error(response.msg || '获取执行历史失败');
        }
        return response;
    },
};

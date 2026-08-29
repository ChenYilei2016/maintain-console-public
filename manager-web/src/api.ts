import type {
    AiAssistAction,
    AiAssistResponse,
    ApiResponse,
    DirectoryNode,
    ExecutionApproval,
    ExecutionHistory,
    ExecutionTask,
    ExecutionTaskRequest,
    LoginInfo,
    PageResponse,
    RuntimeMetadata,
    ScriptDetail,
    ScriptExecutionResult,
    ScriptResourceOverview,
    ScriptRevision,
    ServiceInstance,
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

    async previewScript(script: string, params: Record<string, unknown>, parameterSchema?: string): Promise<string> {
        return unwrap(await post<string>('/manager/script/preview', {
            script,
            params: JSON.stringify(params),
            parameterSchema,
        }));
    },

    async executeScript(request: {
        service: string;
        script: string;
        env: string;
        scriptId: string;
        params: string;
        parameterSchema?: string;
    }): Promise<ScriptExecutionResult> {
        return unwrap(await post<ScriptExecutionResult>('/manager/script/eval/v2', request));
    },

    async listInstances(serviceName: string, environment: string): Promise<ServiceInstance[]> {
        const query = new URLSearchParams({serviceName, environment});
        return unwrap(await fetchJson<ApiResponse<ServiceInstance[]>>(`/manager/service/instances?${query}`));
    },

    async getRuntimeMetadata(serviceName: string, environment: string, instanceId?: string): Promise<RuntimeMetadata> {
        const query = new URLSearchParams({serviceName, environment});
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

    async createExecutionTask(request: ExecutionTaskRequest): Promise<ExecutionTask> {
        return unwrap(await post<ExecutionTask>('/manager/script/tasks', request));
    },

    async createExecutionApproval(execution: ExecutionTaskRequest, reason: string): Promise<ExecutionApproval> {
        return unwrap(await post<ExecutionApproval>('/manager/execution/approvals', {execution, reason}));
    },

    async getExecutionApproval(approvalId: string): Promise<ExecutionApproval> {
        return unwrap(await fetchJson<ApiResponse<ExecutionApproval>>(`/manager/execution/approvals/${approvalId}`));
    },

    async listPendingApprovals(): Promise<ExecutionApproval[]> {
        return unwrap(await fetchJson<ApiResponse<ExecutionApproval[]>>('/manager/execution/approvals/pending'));
    },

    async decideExecutionApproval(approvalId: string, approved: boolean, comment: string): Promise<ExecutionApproval> {
        return unwrap(await post<ExecutionApproval>(`/manager/execution/approvals/${approvalId}/decision`, {
            approved,
            comment,
        }));
    },

    async getExecutionTask(taskId: string): Promise<ExecutionTask> {
        return unwrap(await fetchJson<ApiResponse<ExecutionTask>>(`/manager/script/tasks/${taskId}`));
    },

    async cancelExecutionTask(taskId: string): Promise<ExecutionTask> {
        return unwrap(await post<ExecutionTask>(`/manager/script/tasks/${taskId}/cancel`));
    },

    watchExecutionTask(
        taskId: string,
        onUpdate: (task: ExecutionTask) => void,
        onError: (error: Error) => void,
    ): () => void {
        let stopped = false;
        const source = new EventSource(`/manager/script/tasks/${taskId}/events`);
        const deliver = (task: ExecutionTask) => {
            onUpdate(task);
            const terminal = ['SUCCESS', 'FAILED', 'PARTIAL_SUCCESS', 'CANCELLED', 'TIMED_OUT'].includes(task.status);
            if (terminal) {
                stopped = true;
                source.close();
            }
            return terminal;
        };
        const poll = async () => {
            if (stopped) return;
            try {
                const task = await api.getExecutionTask(taskId);
                if (!deliver(task)) {
                    window.setTimeout(poll, 1000);
                }
            } catch (error) {
                onError(error instanceof Error ? error : new Error('执行任务连接失败'));
            }
        };
        source.onmessage = (event) => deliver(JSON.parse(event.data) as ExecutionTask);
        source.onerror = () => {
            source.close();
            void poll();
        };
        return () => {
            stopped = true;
            source.close();
        };
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

    async restoreScriptRevision(scriptId: string, version: number): Promise<number> {
        return unwrap(await post<number>('/manager/directory/script/revision/restore', {scriptId, version}));
    },
};

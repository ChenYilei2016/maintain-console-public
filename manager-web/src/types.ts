export type NoticeType = 'success' | 'warning' | 'error';

export interface ApiResponse<T> {
    success: boolean;
    data: T;
    msg?: string;
    code: number;
}

export interface PageResponse<T> extends ApiResponse<T> {
    page: number;
    pageSize: number;
    totalElements: number;
}

export interface EnvironmentOption {
    value: string;
    name: string;
    icon: string;
}

export interface LoginInfo {
    employeeName: string;
    employeeNo: string;
    env: string;
    availableEnvironments: EnvironmentOption[];
}

export interface DirectoryNode {
    id: string;
    name: string;
    type: 'folder' | 'script';
    parentId?: string;
    serviceName: string;
    children?: DirectoryNode[];
    permissionType?: 'public' | 'private';
    level?: number;
}

export interface ScriptDetail extends DirectoryNode {
    content: string;
    permissions?: string;
    creator?: string;
    createTime?: string;
    updateTime?: string;
    canRead: boolean;
    canEdit: boolean;
    canInvoke: boolean;
}

export interface ExecutionHistory {
    id: string;
    scriptId: string;
    scriptName: string;
    serviceName: string;
    executorId: string;
    executorName: string;
    scriptContent: string;
    finalScriptContent: string;
    parameters?: string;
    result?: string;
    status: 'success' | 'error';
    errorMessage?: string;
    startTime: string;
    endTime: string;
    duration: number;
    createTime: string;
}

export interface TreeNodeSaveRequest {
    nodeType: DirectoryNode['type'];
    nodeId?: string;
    nodeName: string;
    parentId?: string;
    serviceName: string;
    content?: string;
    permissions?: string;
    description?: string;
}

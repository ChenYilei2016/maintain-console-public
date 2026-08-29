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
    cluster?: string;
    namespace?: string;
    description?: string;
    production: boolean;
}

export interface LoginInfo {
    employeeName: string;
    employeeNo: string;
    env: string;
    availableEnvironments: EnvironmentOption[];
    canApprove: boolean;
    aiEnabled: boolean;
}

export type AiAssistAction = 'GENERATE_SCRIPT' | 'EXPLAIN_SCRIPT' | 'GENERATE_PARAMETER_SCHEMA' | 'REVIEW_RISK';

export interface AiAssistResponse {
    action: AiAssistAction;
    content: string;
    model: string;
    notice: string;
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
    parameterSchema?: string;
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
    protocolVersion?: number;
    resultPayload?: string;
    status: 'success' | 'error';
    errorMessage?: string;
    startTime: string;
    endTime: string;
    duration: number;
    createTime: string;
}

export type ResultBlockType = 'text' | 'log' | 'json' | 'table' | 'metric' | 'chart' | 'file' | 'error';

export interface ResultBlock {
    type: ResultBlockType;
    title?: string;
    data: unknown;
}

export interface ScriptExecutionResult {
    protocolVersion: 1;
    blocks: ResultBlock[];
}

export interface TreeNodeSaveRequest {
    nodeType: DirectoryNode['type'];
    nodeId?: string;
    nodeName: string;
    parentId?: string;
    serviceName: string;
    content?: string;
    parameterSchema?: string;
    permissions?: string;
    description?: string;
}

export type ParameterType =
    'STRING'
    | 'NUMBER'
    | 'BOOLEAN'
    | 'ENUM'
    | 'JSON'
    | 'MULTILINE'
    | 'DATETIME'
    | 'SERVICE_INSTANCE';

export interface ParameterDefinition {
    name: string;
    type: ParameterType;
    required?: boolean;
    defaultValue?: unknown;
    description?: string;
    example?: string;
    options?: string[];
    pattern?: string;
    min?: number;
    max?: number;
    sensitive?: boolean;
}

export interface ParameterSchema {
    version: 1;
    parameters: ParameterDefinition[];
}

export interface ScriptRevision {
    id: string;
    scriptId: string;
    version: number;
    content: string;
    parameterSchema?: string;
    description?: string;
    creatorId: string;
    creatorName: string;
    createTime: string;
}

export interface ServiceInstance {
    id: string;
    serviceId: string;
    host: string;
    port: number;
    secure: boolean;
    uri: string;
    metadata: Record<string, string>;
}

export interface RuntimeMetadata {
    protocolVersion: number;
    beans: Array<{ name: string; type: string; methods: string[] }>;
}

export interface ScriptShortcut {
    id: string;
    name: string;
    serviceName: string;
    parentId?: string;
    favorite: boolean;
    lastOpenTime?: string;
}

export interface ScriptResourceOverview {
    favorites: ScriptShortcut[];
    recent: ScriptShortcut[];
}

export type TargetSelectionMode = 'RANDOM' | 'SPECIFIC' | 'ALL';
export type ExecutionTaskStatus = 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'PARTIAL_SUCCESS'
    | 'CANCELLING' | 'CANCELLED' | 'TIMED_OUT';

export interface ExecutionTargetResult {
    instance: ServiceInstance;
    status: ExecutionTaskStatus;
    duration?: number;
    result?: ScriptExecutionResult;
    errorMessage?: string;
}

export interface ExecutionTask {
    id: string;
    scriptId: string;
    scriptName: string;
    serviceName: string;
    environment: string;
    selectionMode: TargetSelectionMode;
    requestedInstanceId?: string;
    executorId: string;
    executorName: string;
    status: ExecutionTaskStatus;
    targets: ExecutionTargetResult[];
    timeoutSeconds: number;
    cancelRequested: boolean;
    errorMessage?: string;
    createTime: string;
    startTime?: string;
    endTime?: string;
    duration?: number;
    approvalId?: string;
    production: boolean;
}

export interface ExecutionTaskRequest {
    service: string;
    env: string;
    scriptId: string;
    script: string;
    params: string;
    parameterSchema?: string;
    selectionMode: TargetSelectionMode;
    instanceId?: string;
    timeoutSeconds: number;
    approvalId?: string;
    productionConfirmation?: string;
}

export type ExecutionApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CONSUMED';

export interface ExecutionApproval {
    id: string;
    scriptId: string;
    scriptName: string;
    serviceName: string;
    environment: string;
    selectionMode: TargetSelectionMode;
    requestedInstanceId?: string;
    requesterId: string;
    requesterName: string;
    status: ExecutionApprovalStatus;
    scriptContent: string;
    parameters?: string;
    reason: string;
    approverId?: string;
    approverName?: string;
    decisionComment?: string;
    createTime: string;
    expireTime: string;
    decisionTime?: string;
    consumedTime?: string;
    productionConfirmation: string;
}

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
    userId: string;
    employeeName: string;
    employeeNo: string;
    env: string;
    availableEnvironments: EnvironmentOption[];
    aiEnabled: boolean;
    roles: string[];
    administrator: boolean;
}

export interface AuthState {
    authenticated: boolean;
    provider: 'LOCAL_PASSWORD' | 'TRUSTED_HEADERS';
    csrfToken: string;
}

export type ConsoleRole = 'ADMIN';
export type ConsoleUserStatus = 'ACTIVE' | 'DISABLED';

export interface ConsoleUser {
    id: string;
    provider: string;
    employeeNo: string;
    displayName: string;
    roles: ConsoleRole[];
    status: ConsoleUserStatus;
    lastLoginTime?: string;
    createTime: string;
}

export interface EnvironmentManagementOverview {
    mode: 'SPRING_CLOUD' | 'MULTI_NACOS';
    environments: Array<{
        id: string; name: string; production: boolean; registryId?: string; legacyNamespace?: string;
        groupName?: string; instanceClusters: string[]
    }>;
    registries: Array<{
        id: string; name?: string; namespaceId?: string; defaultGroup: string;
        authenticationConfigured: boolean
    }>;
}

export type UsageWindow = 'WEEK' | 'MONTH' | 'QUARTER';

export interface UsageStatistics {
    window: UsageWindow;
    days: number;
    summary: {
        totalExecutions: number;
        successfulExecutions: number;
        failedExecutions: number;
        averageDurationMillis: number;
        activeUsers: number;
        activeTools: number;
    };
    tools: Array<{
        scriptId: string;
        scriptName: string;
        serviceName: string;
        totalExecutions: number;
        successfulExecutions: number;
        averageDurationMillis: number;
        lastRunTime: string;
    }>;
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
    creator?: string;
    canRead?: boolean;
    canEdit?: boolean;
    canInvoke?: boolean;
    canManage?: boolean;
    canCreateChild?: boolean;
    canRename?: boolean;
    canDelete?: boolean;
}

export interface ScriptDetail extends DirectoryNode {
    version: number;
    description?: string;
    toolMetadata?: ToolMetadata;
    canManage: boolean;
    allowedEnvironments?: string[];
    allowAllInstances: boolean;
    enabled: boolean;
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
    environment?: string;
    scriptVersion?: number;
    targetsJson?: string;
    outcome?: string;
    draft?: boolean;
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
    expectedVersion?: number;
    allowedEnvironments?: string[];
    toolMetadata?: ToolMetadata;
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
    label?: string;
    group?: string;
    advanced?: boolean;
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

export const OPERATION_TYPES = {
    UNSPECIFIED: 'UNSPECIFIED',
    QUERY: 'QUERY',
    OPERATION: 'OPERATION',
} as const;
export type OperationType = typeof OPERATION_TYPES[keyof typeof OPERATION_TYPES];

export interface ToolMetadata {
    operationType: OperationType;
    riskNote?: string;
    usageExample?: string;
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

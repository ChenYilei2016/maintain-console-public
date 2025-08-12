const {createApp, ref, reactive, computed, onMounted, nextTick, watch} = Vue;

// 目录节点组件
const DirectoryNode = {
    name: 'DirectoryNode',
    props: ['node', 'searchQuery'],
    emits: ['select', 'create', 'delete', 'rename'],
    setup(props, {emit}) {
        const expanded = ref(false);
        const isSelected = ref(false);

        // 监听搜索查询变化，展开匹配的节点
        watch(() => props.searchQuery, (newQuery) => {
            if (newQuery && props.node.type === 'folder') {
                // 如果有搜索查询且当前是文件夹，检查是否包含匹配的子节点
                if (hasMatchingChildren(props.node, newQuery)) {
                    expanded.value = true;
                }
            }
        });

        // 检查节点是否匹配搜索查询
        const isMatched = computed(() => {
            if (!props.searchQuery) return true;
            return props.node.name.toLowerCase().includes(props.searchQuery.toLowerCase());
        });

        // 检查是否有匹配的子节点
        const hasMatchingChildren = (node, query) => {
            if (!node.children) return false;
            return node.children.some(child => {
                return child.name.toLowerCase().includes(query.toLowerCase()) ||
                    (child.children && hasMatchingChildren(child, query));
            });
        };

        // 计算过滤后的子节点
        const filteredChildren = computed(() => {
            if (!props.node.children || !props.searchQuery) {
                return props.node.children || [];
            }

            return props.node.children.filter(child => {
                return child.name.toLowerCase().includes(props.searchQuery.toLowerCase()) ||
                    (child.children && hasMatchingChildren(child, props.searchQuery));
            });
        });

        const toggleExpand = () => {
            if (props.node.type === 'folder') {
                expanded.value = !expanded.value;
            }
        };

        const selectNode = () => {
            emit('select', props.node);
        };

        const createChild = () => {
            emit('create', props.node.id);
        };

        const deleteNode = () => {
            emit('delete', props.node);
        };

        const renameNode = () => {
            emit('rename', props.node);
        };

        // 计算节点深度，判断是否可以创建子节点
        const canCreateChild = computed(() => {
            // 只有文件夹类型才能创建子节点，且层级不能超过1（即最多到level 1的文件夹都可以创建脚本）
            return props.node.type === 'folder' && (props.node.level === undefined || props.node.level <= 1);
        });

        const handleNodeClick = () => {
            if (props.node.type === 'folder') {
                // 文件夹：展开/收缩
                toggleExpand();
            } else {
                // 脚本文件：选中
                selectNode();
            }
        };

        return {
            expanded,
            isSelected,
            isMatched,
            filteredChildren,
            toggleExpand,
            selectNode,
            createChild,
            deleteNode,
            renameNode,
            canCreateChild,
            handleNodeClick
        };
    },
    template: `
      <div class="group" v-if="isMatched || (node.children && filteredChildren.length > 0)">
        <div @click="handleNodeClick"
             :class="['tree-node px-2 py-1 rounded flex items-center text-sm', 
                      isSelected ? 'selected' : '',
                      searchQuery && isMatched ? 'bg-yellow-50 border border-yellow-200' : '']">
          <!-- 文件夹图标 -->
          <i v-if="node.type === 'folder'"
             :class="['fas mr-2 text-blue-500 cursor-pointer', expanded ? 'fa-folder-open' : 'fa-folder']"></i>
          <!-- 脚本图标，根据权限类型显示不同颜色 -->
          <i v-else-if="node.type === 'script'"
             :class="['fas fa-file-code mr-2 cursor-pointer', 
                      node.permissionType === 'private' ? 'text-orange-500' : 'text-green-500']"></i>
          <!-- 默认文件图标 -->
          <i v-else class="fas fa-file mr-2 text-gray-400"></i>

          <span class="flex-1 cursor-pointer">{{ node.name }}</span>

          <!-- 权限标识 -->
          <i v-if="node.type === 'script' && node.permissionType === 'private'"
             class="fas fa-lock text-xs text-orange-400 mr-1"
             title="私有脚本"></i>
          <i v-else-if="node.type === 'script' && node.permissionType === 'public'"
             class="fas fa-globe text-xs text-green-400 mr-1"
             title="公共脚本"></i>

          <!-- 操作按钮组 -->
          <div class="opacity-0 group-hover:opacity-100 flex items-center space-x-1">
            <!-- 创建子节点按钮 -->
            <button v-if="canCreateChild"
                    @click.stop="createChild"
                    class="w-4 h-4 flex items-center justify-center text-gray-400 hover:text-green-500"
                    title="创建子节点">
              <i class="fas fa-plus text-xs"></i>
            </button>
            
            <!-- 重命名按钮（文件夹和脚本都显示） -->
            <button @click.stop="renameNode"
                    class="w-4 h-4 flex items-center justify-center text-gray-400 hover:text-blue-500"
                    title="重命名">
              <i class="fas fa-edit text-xs"></i>
            </button>
            
            <!-- 删除按钮 -->
            <button @click.stop="deleteNode"
                    class="w-4 h-4 flex items-center justify-center text-gray-400 hover:text-red-500"
                    title="删除">
              <i class="fas fa-trash text-xs"></i>
            </button>
          </div>
          
          <!-- 第二层文件夹提示 -->
          <span v-if="node.type === 'folder' && node.parentId !== null && !canCreateChild"
                class="opacity-0 group-hover:opacity-100 text-xs text-gray-400 mr-1"
                title="已达到最大目录深度，无法创建子文件夹">
            <i class="fas fa-ban"></i>
          </span>
        </div>
        <div v-if="expanded && filteredChildren.length > 0" class="ml-4 mt-1 space-y-1">
          <directory-node v-for="child in filteredChildren"
                          :key="child.id"
                          :node="child"
                          :search-query="searchQuery"
                          @select="$emit('select', $event)"
                          @create="$emit('create', $event)"
                          @delete="$emit('delete', $event)"
                          @rename="$emit('rename', $event)">
          </directory-node>
        </div>
      </div>
    `
};

// 主应用
createApp({
    components: {
        DirectoryNode
    },
    setup() {
        // 响应式数据
        const currentUser = ref(null);

        // 环境相关
        const availableEnvironments = ref([]);
        const selectedEnvironment = ref('');
        const showEnvironmentDropdown = ref(false);

        const availableServices = ref([]);
        const selectedService = ref('');
        const showServiceDropdown = ref(false);
        const loadingServices = ref(false);
        const directoryTree = ref([]);

        // 搜索相关
        const searchQuery = ref('');

        // 默认脚本内容
        const DEFAULT_SCRIPT_CONTENT = 'def queryData() {\n' +
            '    //注意使用的是groovy语法, 如果产生的变量没使用到可能回报类似 no property错误\n' +
            '    List<String> ids = strToList(\'$${idList}\',\',\')\n' +
            '    try {\n' +
            '        // 获取Spring Bean\n' +
            '        def bean = ctx.getBean(\'$${ yourBeanName }\') // 比如填写 campaignMapper \n' +
            '        // def bean2 = ctx.getBean(your.class)\n' +
            '        bean.doSomeThing();\n' +
            '    }catch(Throwable e){\n' +
            '        return [\n' +
            '            "ids" : ids ,\n' +
            '            "信息" :  \'发生了异常: \' + e.getMessage()\n' +
            '        ]\n' +
            '    }\n' +
            '    return [ "ids" : ids , \'queryTime\': new Date(), \'resultCount\': results.size(),\'results\': results    ]\n' +
            '}\n' +
            '\n' +
            'return queryData()';

        const currentScript = reactive({
            id: null,
            name: '',
            content: DEFAULT_SCRIPT_CONTENT,
            permissions: '',
            creator: '',
            updateTime: '',
            canRead: true,
            canEdit: true,
            canInvoke: true
        });

        const permissionJson = ref(`{
    "description": "这里可以扩展很多权限设置, 默认只有创建人有权限编辑, invokerNo:哪些工号有权限执行, 默认创建人;\n    readerNo:哪些工号拥有权限看到脚本的代码, 默认都可以; \n    editorNo:哪些工号拥有权限编辑此脚本 , 默认创建人",
    "invokerNo" : "12600" ,
    "readerNo": "12600,12599",
    "editorNo": ""
} `);
        const permissionError = ref('');
        const isEditingPermission = ref(false);
        const scriptParameters = ref([]);
        const parameterValues = reactive({});
        const executionResult = ref('<span class="text-gray-500">等待执行脚本...</span>');
        const isExecuting = ref(false);
        const showPreviewModal = ref(false);
        const previewCode = ref('');

        // 展开模态框相关
        const showExpandedCodeModal = ref(false);
        const expandedCodeTitle = ref('');
        const expandedCodeContent = ref('');
        const expandedCodeMode = ref('input'); // 'input' 或 'output'

        // 帮助指南相关
        const showHelpModal = ref(false);
        const helpContent = ref('');

        // 创建对话框相关
        const showCreateModal = ref(false);
        const createType = ref('folder');
        const createName = ref('');
        const createParentId = ref('');
        const createDirectoryType = ref('my');
        const isCreating = ref(false);

        // 删除确认对话框相关
        const showDeleteModal = ref(false);
        const deleteTargetNode = ref(null);
        const deleteForceDelete = ref(false);
        const isDeleting = ref(false);

        // 重命名对话框相关
        const showRenameModal = ref(false);
        const renameTargetNode = ref(null);
        const renameNewName = ref('');
        const isRenaming = ref(false);

        // 消息提示框相关
        const showToast = ref(false);
        const toastMessage = ref('');
        const toastType = ref('error'); // success, warning, error
        let toastTimer = null;

        const executionHistory = ref([]);
        const showHistoryListModal = ref(false);
        const showHistoryDetailModal = ref(false);
        const selectedHistory = ref(null);

        const historyCurrentPage = ref(1);
        const historyPageSize = ref(10);
        const historyTotalRecords = ref(0);

        let codeEditor = null;

        // 计算属性
        const selectedServiceName = computed(() => selectedService.value);
        const serviceStatusIcon = computed(() =>
            selectedService.value ? 'fas fa-circle text-green-500' : 'fas fa-circle text-gray-400'
        );

        // 环境相关计算属性
        const selectedEnvironmentName = computed(() => {
            const env = availableEnvironments.value.find(e => e.value === selectedEnvironment.value);
            return env ? env.name : '';
        });
        const environmentStatusIcon = computed(() => {
            const env = availableEnvironments.value.find(e => e.value === selectedEnvironment.value);
            return env ? env.icon : 'fas fa-circle text-gray-400';
        });

        const canEdit = computed(() => {
            return currentScript.canEdit;
        });

        // 查找节点的辅助函数
        const findNodeById = (nodeId, nodes = directoryTree.value) => {
            for (const node of nodes) {
                if (node.id === nodeId) {
                    return node;
                }
                if (node.children) {
                    const found = findNodeById(nodeId, node.children);
                    if (found) return found;
                }
            }
            return null;
        };

        // 计算是否在第二层文件夹中创建
        const isCreatingInSecondLevel = computed(() => {
            if (createParentId.value === null) {
                // 在根目录创建，不是第二层
                return false;
            }

            // 查找父节点
            const parentNode = findNodeById(createParentId.value);
            if (!parentNode) {
                return false;
            }

            // 基于level字段判断：如果父节点level为1，说明当前创建的是第三层节点，只能创建脚本
            return parentNode.level >= 1;
        });

        // 创建对话框标题
        const createModalTitle = computed(() => {
            return createType.value === 'folder' ? '新建文件夹' : '新建脚本';
        });

        // 格式化帮助内容（简单的Markdown解析）
        const formattedHelpContent = computed(() => {
            if (!helpContent.value) return '';

            let formatted = helpContent.value
                // 标题
                .replace(/^### (.*$)/gim, '<h3 class="text-lg font-semibold mt-4 mb-2">$1</h3>')
                .replace(/^## (.*$)/gim, '<h2 class="text-xl font-semibold mt-6 mb-3">$1</h2>')
                .replace(/^# (.*$)/gim, '<h1 class="text-2xl font-bold mt-8 mb-4">$1</h1>')
                // 粗体和斜体
                .replace(/\*\*(.*?)\*\*/g, '<strong class="font-semibold">$1</strong>')
                .replace(/\*(.*?)\*/g, '<em class="italic">$1</em>')
                // 行内代码
                .replace(/`([^`]+)`/g, '<code class="bg-gray-100 px-1 py-0.5 rounded text-sm font-mono">$1</code>')
                // 列表
                .replace(/^- (.*$)/gim, '<li class="ml-4">• $1</li>')
                // 换行
                .replace(/\n/g, '<br>');

            // 处理代码块
            formatted = formatted.replace(/```([^`]+)```/g, '<pre class="bg-gray-100 p-3 rounded mt-2 mb-2 text-sm font-mono overflow-x-auto">$1</pre>');

            return formatted;
        });

        // 搜索相关计算属性
        const filteredTree = computed(() => {
            console.log('filteredTree computed - searchQuery:', searchQuery.value, 'directoryTree length:', directoryTree.value.length);
            if (!searchQuery.value) {
                return directoryTree.value;
            }

            return filterNodes(directoryTree.value, searchQuery.value);
        });

        // 过滤节点的辅助函数
        const filterNodes = (nodes, query) => {
            const filtered = [];
            for (const node of nodes) {
                const isMatch = node.name.toLowerCase().includes(query.toLowerCase());
                const hasMatchingChildren = node.children ? filterNodes(node.children, query).length > 0 : false;

                if (isMatch || hasMatchingChildren) {
                    const filteredNode = {...node};
                    if (node.children) {
                        filteredNode.children = filterNodes(node.children, query);
                    }
                    filtered.push(filteredNode);
                }
            }
            return filtered;
        };

        // 计算匹配项总数
        const getTotalMatchCount = () => {
            if (!searchQuery.value) return 0;
            return countMatches(directoryTree.value, searchQuery.value);
        };

        // 递归计算匹配数量
        const countMatches = (nodes, query) => {
            let count = 0;
            for (const node of nodes) {
                if (node.name.toLowerCase().includes(query.toLowerCase())) {
                    count++;
                }
                if (node.children) {
                    count += countMatches(node.children, query);
                }
            }
            return count;
        };

        // 方法
        // 重置脚本状态到默认值
        const resetScriptToDefault = () => {
            currentScript.id = null;
            currentScript.name = '';
            currentScript.content = DEFAULT_SCRIPT_CONTENT;
            currentScript.permissions = '';
            currentScript.creator = '';
            currentScript.updateTime = '';

            if (codeEditor) {
                codeEditor.setValue(DEFAULT_SCRIPT_CONTENT);
            }

            executionResult.value = '<span class="text-gray-500">等待执行脚本...</span>';
        };

        const loadLoginInfo = async () => {
            try {
                const response = await axios.post('/manager/login/getInfo');
                if (response.data.success) {
                    currentUser.value = response.data.data;

                    // 更新可用环境配置
                    if (response.data.data.availableEnvironments) {
                        availableEnvironments.value = response.data.data.availableEnvironments;
                        // 如果只有一个环境选项，自动选择
                        if (availableEnvironments.value.length === 1) {
                            selectedEnvironment.value = availableEnvironments.value[0].value;
                        }
                    }

                    loadServiceList();
                } else {
                    showToastMessage(response.data.msg || '获取登录信息失败', 'error');
                }
            } catch (error) {
                showToastMessage('获取登录信息失败: ' + (error.response?.data?.message || error.message), 'error');
            }
        };

        const loadServiceList = async () => {
            loadingServices.value = true;
            try {
                const response = await axios.post('/manager/service/list');
                if (response.data.success) {
                    availableServices.value = response.data.data || [];
                } else {
                    showToastMessage(response.data.msg || '获取服务列表失败', 'error');
                }
            } catch (error) {
                showToastMessage('获取服务列表失败: ' + (error.response?.data?.message || error.message), 'error');
            } finally {
                loadingServices.value = false;
            }
        };

        const toggleServiceDropdown = () => {
            showServiceDropdown.value = !showServiceDropdown.value;
        };

        // 环境相关方法
        const toggleEnvironmentDropdown = () => {
            showEnvironmentDropdown.value = !showEnvironmentDropdown.value;
        };

        const selectEnvironment = (environment) => {
            selectedEnvironment.value = environment;
            showEnvironmentDropdown.value = false;

            // 根据环境名称显示警告
            const env = availableEnvironments.value.find(e => e.value === environment);
            if (env && (env.name.includes('生产') || env.name.includes('prod'))) {
                showToastMessage('已切换到生产环境，请谨慎操作！', 'warning');
            }
        };

        const selectService = (service) => {
            selectedService.value = service;
            showServiceDropdown.value = false;
            loadDirectoryTree();
        };

        const loadDirectoryTree = async () => {
            if (!selectedService.value) return;

            try {
                const response = await axios.post('/manager/directory/tree', null, {
                    params: {serviceName: selectedService.value}
                });
                if (response.data.success) {
                    directoryTree.value = response.data.data || [];
                } else {
                    showToastMessage(response.data.msg);
                }
            } catch (error) {
                showToastMessage('获取目录树失败: ' + (error.response?.data?.message || error.message), 'error');
            }
        };

        const selectNode = async (node) => {
            if (node.type === 'script') {
                try {
                    const requestData = {
                        scriptId: node.id
                    };
                    const response = await axios.post('/manager/directory/script/detail', requestData);
                    if (response.data.success) {
                        const script = response.data.data;
                        currentScript.id = script.id;
                        currentScript.name = script.name;
                        currentScript.content = script.content;
                        currentScript.permissions = script.permissions;
                        currentScript.creator = script.creator;
                        currentScript.updateTime = script.updateTime;
                        currentScript.canRead = script.canRead;
                        currentScript.canEdit = script.canEdit;
                        currentScript.canInvoke = script.canInvoke;

                        permissionJson.value = script.permissions || '{}';

                        // 更新编辑器内容
                        if (codeEditor) {
                            codeEditor.setValue(script.content);
                        }

                        // 清空旧参数值
                        Object.keys(parameterValues).forEach(key => delete parameterValues[key]);
                        parseParameters();
                    } else {
                        showToastMessage(response.data.msg);
                    }
                } catch (error) {
                    showToastMessage('获取脚本详情失败: ' + (error.response?.data?.message || error.message), 'error');
                }
            }
        };

        const historyTotalPages = computed(() => {
            return Math.ceil(historyTotalRecords.value / historyPageSize.value);
        });

        const fetchHistory = async (scriptId, page = 1) => {
            if (!scriptId) return;
            try {
                const response = await axios.get(`/manager/script/history?scriptId=${scriptId}&page=${page}&size=${historyPageSize.value}`);
                if (response.data.success) {
                    executionHistory.value = response.data.data;
                    historyTotalRecords.value = response.data.totalElements;
                    historyCurrentPage.value = page;
                } else {
                    showToastMessage(response.data.msg || '获取历史记录失败', 'error');
                }
            } catch (error) {
                showToastMessage('获取历史记录失败: ' + (error.response?.data?.message || error.message), 'error');
            }
        };

        const openHistoryListModal = async () => {
            if (!currentScript.id) {
                showToastMessage('请先选择一个脚本', 'warning');
                return;
            }
            await fetchHistory(currentScript.id, 1);
            showHistoryListModal.value = true;
        };

        const changeHistoryPage = async (page) => {
            if (page > 0 && page <= historyTotalPages.value) {
                await fetchHistory(currentScript.id, page);
            }
        };

        const closeHistoryListModal = () => {
            showHistoryListModal.value = false;
        };

        const showHistoryDetail = (history) => {
            selectedHistory.value = history;
            showHistoryListModal.value = false;
            showHistoryDetailModal.value = true;
        };

        const closeHistoryDetailModal = () => {
            showHistoryDetailModal.value = false;
        };

        const backToHistoryList = () => {
            showHistoryDetailModal.value = false;
            showHistoryListModal.value = true;
        };

        const restoreParameters = (params) => {
            if (params) {
                try {
                    const parsedParams = JSON.parse(params);
                    for (const key in parsedParams) {
                        if (key in parameterValues) {
                            parameterValues[key] = parsedParams[key];
                        }
                    }
                    showToastMessage('参数已恢复', 'success');
                    closeHistoryDetailModal();
                } catch (e) {
                    showToastMessage('参数格式错误，无法恢复', 'error');
                }
            }
        };

        const initCodeEditor = () => {
            nextTick(() => {
                const element = document.querySelector('.CodeMirror');
                if (element) {
                    element.remove();
                }

                codeEditor = CodeMirror(document.querySelector('[data-code-mirror]'), {
                    value: currentScript.content,
                    mode: 'groovy',
                    theme: 'default',
                    lineNumbers: true,
                    lineWrapping: true,
                    autoCloseBrackets: true,
                    matchBrackets: true,
                    styleActiveLine: true,
                    indentUnit: 4,
                    tabSize: 4,
                    indentWithTabs: false,
                    readOnly: !currentScript.canEdit
                });

                codeEditor.on('change', () => {
                    currentScript.content = codeEditor.getValue();
                    parseParameters();
                });
            });
        };

        const parseParameters = () => {
            const content = currentScript.content;
            const paramRegex = /\$\$\{([^}]+)\}/g;
            const parameters = new Set();
            let match;

            while ((match = paramRegex.exec(content)) !== null) {
                parameters.add(match[1].trim());
            }

            scriptParameters.value = Array.from(parameters);

            // 初始化参数值
            scriptParameters.value.forEach(param => {
                if (!(param in parameterValues)) {
                    parameterValues[param] = '';
                }
            });
        };

        const clearParameter = (param) => {
            parameterValues[param] = '';
        };


        const saveScript = async () => {
            if (!currentScript.canEdit) {
                showToastMessage('您没有编辑此脚本的权限', 'error');
                return;
            }
            if (!currentScript.content.trim()) {
                executionResult.value = '<span class="text-red-500">[ERROR] 脚本内容为空</span>';
                return;
            }

            try {
                const requestData = {
                    nodeType: 'script',
                    nodeId: currentScript.id,
                    nodeName: currentScript.name,
                    serviceName: selectedService.value,
                    content: currentScript.content,
                    permissions: permissionJson.value,
                    description: currentScript.name
                };

                const response = await axios.post('/manager/directory/treeNode/save', requestData);
                if (response.data.success) {
                    currentScript.id = response.data.data;
                    executionResult.value = '<span class="text-green-600">[SUCCESS] 脚本保存成功</span>';
                    showToastMessage('脚本保存成功', 'success');
                    // 刷新目录树
                    await loadDirectoryTree();
                } else {
                    executionResult.value = `<span class="text-red-500">[ERROR] ${response.data.msg}</span>`;
                    showToastMessage(response.data.msg || '保存失败', 'error');
                }
            } catch (error) {
                executionResult.value = `<span class="text-red-500">[ERROR] 保存失败: ${error.message}</span>`;
                showToastMessage('脚本保存失败: ' + (error.response?.data?.message || error.message), 'error');
            }
        };

        const executeScript = async () => {
            if (!currentScript.canInvoke) {
                showToastMessage('您没有执行此脚本的权限', 'error');
                return;
            }
            if (!currentScript.content.trim()) {
                executionResult.value = '<span class="text-red-500">[ERROR] 脚本内容为空</span>';
                return;
            }

            if (!selectedService.value) {
                executionResult.value = '<span class="text-red-500">[ERROR] 请先选择要执行脚本的应用服务</span>';
                return;
            }

            // 检查权限
            try {
                const permissions = JSON.parse(permissionJson.value);
                if (permissions.creator && permissions.creator !== currentUser.value?.employeeName) {
                    if (permissions.executePermission === 'creator') {
                        executionResult.value = '<span class="text-red-500">[ERROR] 权限不足：您没有执行此脚本的权限</span>';
                        return;
                    }
                }
            } catch {
                // 忽略权限解析错误
            }

            // 严格过滤出当前脚本实际使用的参数，只传递在脚本中存在的参数
            const currentScriptParams = {};
            scriptParameters.value.forEach(param => {
                if (param in parameterValues) {
                    // 如果值为空字符串或只包含空格，替换为"null"
                    const value = parameterValues[param];
                    currentScriptParams[param] = (value || '').trim() === '' ? 'null' : value;
                }
            });

            // // 检查是否所有参数都已提供
            // const missingParams = scriptParameters.value.filter(param => {
            //     const value = parameterValues[param];
            //     return !value || value.trim() === '';
            // });
            //
            // if (missingParams.length > 0) {
            //     executionResult.value = `<span class="text-red-500">[ERROR] 以下参数未提供值: ${missingParams.join(', ')}</span>`;
            //     return;
            // }

            isExecuting.value = true;
            executionResult.value = '<span class="text-blue-500">[INFO] 正在执行脚本...</span>';

            try {
                const response = await axios.post('/manager/script/eval', {
                    service: selectedService.value,
                    script: currentScript.content,
                    env: selectedEnvironment.value,
                    scriptId: currentScript.id,
                    params: JSON.stringify(currentScriptParams)
                });

                const now = new Date().toLocaleString();
                if (response.data.success) {
                    executionResult.value = `
                            <span class="text-gray-500">[INFO] 脚本执行完成 ${now}</span><br>
                            <span class="text-green-600">[SUCCESS] 执行成功</span><br>
                            <span class="text-gray-700">执行结果:</span><br>
                            <pre class="whitespace-pre-wrap">${escapeHtml(response.data.data)}</pre>
                        `;
                    showToastMessage('脚本执行成功', 'success');
                } else {
                    executionResult.value = `
                            <span class="text-gray-500">[INFO] 脚本执行完成 ${now}</span><br>
                            <span class="text-red-500">[ERROR] 执行失败</span><br>
                            <span class="text-red-600">错误信息: ${escapeHtml(response.data.msg || '未知错误')}</span>
                        `;
                    showToastMessage(response.data.msg || '脚本执行失败', 'error');
                }
            } catch (error) {
                const now = new Date().toLocaleString();
                executionResult.value = `
                        <span class="text-gray-500">[INFO] 脚本执行完成 ${now}</span><br>
                        <span class="text-red-500">[ERROR] 网络错误或服务器异常</span><br>
                        <span class="text-red-600">错误信息: ${escapeHtml(error.message)}</span>
                    `;
                showToastMessage('脚本执行失败: ' + (error.response?.data?.message || error.message), 'error');
            } finally {
                isExecuting.value = false;
            }
        };

        const closePreviewModal = (event) => {
            if (event.target === event.currentTarget) {
                showPreviewModal.value = false;
            }
        };

        const handlePreviewClick = async () => {
            console.log('Current showPreviewModal value:', showPreviewModal.value);

            // 直接调用API而不是通过watch监听器
            try {
                const requestData = {
                    script: currentScript.content,
                    params: JSON.stringify(parameterValues)
                };
                const response = await axios.post('/manager/script/preview', requestData);
                if (response.data.success) {
                    previewCode.value = response.data.data;
                } else {
                    showToastMessage(response.data.msg || '获取预览代码失败', 'error');
                    previewCode.value = currentScript.content; // 降级显示原始内容
                }
            } catch (error) {
                showToastMessage('获取预览代码失败: ' + (error.response?.data?.message || error.message), 'error');
                previewCode.value = currentScript.content; // 降级显示原始内容
            }

            // 最后设置模态框显示
            showPreviewModal.value = true;
        };

        const copyPreviewCode = async () => {
            try {
                await navigator.clipboard.writeText(previewCode.value);
                alert('代码已复制到剪贴板');
            } catch {
                // 降级方案
                const textArea = document.createElement('textarea');
                textArea.value = previewCode.value;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                alert('代码已复制到剪贴板');
            }
        };

        // 展开代码模态框相关方法
        const showExpandedCode = (type) => {
            if (type === 'input') {
                expandedCodeTitle.value = '脚本代码 - 展开视图';
                expandedCodeContent.value = codeEditor ? codeEditor.getValue() : currentScript.content;
            } else if (type === 'output') {
                expandedCodeTitle.value = '执行结果 - 展开视图';
                // 从HTML中提取纯文本内容
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = executionResult.value;
                expandedCodeContent.value = tempDiv.textContent || tempDiv.innerText || '';
            }
            expandedCodeMode.value = type;
            showExpandedCodeModal.value = true;
        };

        const closeExpandedCodeModal = (event) => {
            if (event && event.target === event.currentTarget) {
                showExpandedCodeModal.value = false;
            } else if (!event) {
                showExpandedCodeModal.value = false;
            }
        };

        const copyExpandedCode = async () => {
            try {
                await navigator.clipboard.writeText(expandedCodeContent.value);
                showToastMessage('代码已复制到剪贴板', 'success');
            } catch {
                // 降级方案
                const textArea = document.createElement('textarea');
                textArea.value = expandedCodeContent.value;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                showToastMessage('代码已复制到剪贴板', 'success');
            }
        };

        const showCreateDialog = (parentId) => {
            createParentId.value = parentId;
            createDirectoryType.value = 'my'; // 默认为个人目录
            createName.value = '';

            // 使用nextTick确保响应式数据更新完成后再设置createType
            nextTick(() => {
                // 判断是否在第二层级创建
                let shouldCreateScript = false;
                if (parentId !== null) {
                    const parentNode = findNodeById(parentId);
                    if (parentNode && parentNode.level >= 1) {
                        shouldCreateScript = true;
                    }
                }

                // 根据层级设置默认创建类型
                createType.value = shouldCreateScript ? 'script' : 'folder';
            });

            showCreateModal.value = true;
        };

        const closeCreateModal = (event) => {
            if (event.target === event.currentTarget) {
                showCreateModal.value = false;
            }
        };

        const confirmCreate = async () => {
            if (!createName.value.trim()) {
                showToastMessage('请输入名称', 'warning');
                return;
            }

            if (!selectedService.value) {
                showToastMessage('请先选择应用服务', 'warning');
                return;
            }

            isCreating.value = true;

            try {
                const requestData = {
                    nodeType: createType.value,
                    nodeName: createName.value.trim(),
                    parentId: createParentId.value,
                    serviceName: selectedService.value
                };

                // 如果是脚本，添加默认内容
                if (createType.value === 'script') {
                    requestData.content = '// 新建脚本\nreturn "Hello World";';
                    requestData.permissions = '{}';
                    requestData.description = createName.value.trim();
                }

                const response = await axios.post('/manager/directory/treeNode/save', requestData);

                if (response.data.success) {
                    showToastMessage(response.data.msg || (createType.value === 'folder' ? '文件夹创建成功' : '脚本创建成功'), 'success');
                    showCreateModal.value = false;

                    // 刷新目录树
                    await loadDirectoryTree();

                    // 如果是创建脚本，自动选中并加载
                    if (createType.value === 'script') {
                        const scriptId = response.data.data;
                        // 延迟一下再选中脚本
                        setTimeout(() => {
                            selectNode({id: scriptId, type: 'script'});
                        }, 500);
                    }
                } else {
                    showToastMessage(response.data.msg || '创建失败', 'error');
                }
            } catch (error) {
                showToastMessage('创建失败: ' + (error.response?.data?.message || error.message), 'error');
            } finally {
                isCreating.value = false;
            }
        };

        // 帮助指南相关方法
        const closeHelpModal = (event) => {
            if (event.target === event.currentTarget) {
                showHelpModal.value = false;
            }
        };

        const loadHelpContent = () => {
            // 设置固定的帮助内容
            helpContent.value = `# Maintain Console 使用指南
## 概述
Maintain Console 是一个脚本管理和执行平台，支持 Groovy 脚本的在线编写、管理和远程执行。
针对 查询数据、修复数据、订正数据、执行操作的过程 可以直接存储保存成可分享、可复用的脚本集合
## 主要功能
举个例子,比如查询数据:
如果简单的查询一个数据可能还好, 如果是紧急排查问题, 涉及先查出A, 再通过A去RPC过滤得到B, B再通过数据库查出C,
类似上面的标签场景, 我们每次的一些查询操作, 都需要编写代码、都需要发布、都需要有很高的成本去调用( postman? 登录态? 是否安全、是否方便)
因此使用 Maintain Console 可以实时编写java代码 + 实时执行解决以上痛点
## 技术支持
如有问题请联系技术团队。`;
        };

        const escapeHtml = (text) => {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        };

        // 删除和重命名相关方法
        const handleDeleteNode = (node) => {
            deleteTargetNode.value = node;
            deleteForceDelete.value = false;
            showDeleteModal.value = true;
        };

        const handleRenameNode = (node) => {
            renameTargetNode.value = node;
            renameNewName.value = node.name;
            showRenameModal.value = true;
        };

        const closeDeleteModal = (event) => {
            if (event && event.target === event.currentTarget) {
                showDeleteModal.value = false;
            } else if (!event) {
                showDeleteModal.value = false;
            }
        };

        const closeRenameModal = (event) => {
            if (event && event.target === event.currentTarget) {
                showRenameModal.value = false;
            } else if (!event) {
                showRenameModal.value = false;
            }
        };

        const confirmDelete = async () => {
            if (!deleteTargetNode.value) return;

            isDeleting.value = true;

            try {
                const requestData = {
                    nodeId: deleteTargetNode.value.id,
                    forceDelete: deleteForceDelete.value
                };

                const response = await axios.post('/manager/directory/treeNode/delete', requestData);

                if (response.data.success) {
                    showToastMessage(response.data.msg || '删除成功', 'success');
                    showDeleteModal.value = false;

                    // 刷新目录树
                    await loadDirectoryTree();

                    // 如果删除的是当前选中的脚本，清空编辑器
                    if (deleteTargetNode.value.type === 'script' && currentScript.id === deleteTargetNode.value.id) {
                        resetScriptToDefault();
                    }
                } else {
                    showToastMessage(response.data.msg || '删除失败', 'error');
                }
            } catch (error) {
                showToastMessage('删除失败: ' + (error.response?.data?.message || error.message), 'error');
            } finally {
                isDeleting.value = false;
            }
        };

        const confirmRename = async () => {
            if (!renameTargetNode.value || !renameNewName.value.trim()) return;

            isRenaming.value = true;

            try {
                const requestData = {
                    nodeType: renameTargetNode.value.type,
                    nodeId: renameTargetNode.value.id,
                    nodeName: renameNewName.value.trim(),
                    serviceName: selectedService.value
                };

                // 如果是脚本，需要保留原有内容
                if (renameTargetNode.value.type === 'script') {
                    // 如果是当前编辑的脚本，使用当前内容
                    if (currentScript.id === renameTargetNode.value.id) {
                        requestData.content = currentScript.content;
                        requestData.permissions = permissionJson.value;
                    } else {
                        // 需要获取脚本的当前内容
                        const scriptDetailResponse = await axios.post('/manager/directory/script/detail', {
                            scriptId: renameTargetNode.value.id
                        });
                        if (scriptDetailResponse.data.success) {
                            const scriptDetail = scriptDetailResponse.data.data;
                            requestData.content = scriptDetail.content;
                            requestData.permissions = scriptDetail.permissions;
                        }
                    }
                    requestData.description = renameNewName.value.trim();
                }

                const response = await axios.post('/manager/directory/treeNode/save', requestData);

                if (response.data.success) {
                    showToastMessage(response.data.msg || '重命名成功', 'success');
                    showRenameModal.value = false;

                    // 刷新目录树
                    await loadDirectoryTree();

                    // 如果重命名的是当前选中的脚本，更新名称
                    if (renameTargetNode.value.type === 'script' && currentScript.id === renameTargetNode.value.id) {
                        currentScript.name = renameNewName.value.trim();
                    }
                } else {
                    showToastMessage(response.data.msg || '重命名失败', 'error');
                }
            } catch (error) {
                showToastMessage('重命名失败: ' + (error.response?.data?.message || error.message), 'error');
            } finally {
                isRenaming.value = false;
            }
        };

        // 搜索和定位功能方法
        const clearSearch = () => {
            searchQuery.value = '';
        };

        const locateCurrentScript = () => {
            if (!currentScript.id) {
                showToastMessage('请先选择一个脚本', 'warning');
                return;
            }

            // 清空搜索以显示完整目录树
            searchQuery.value = '';

            // 通过脚本名称进行搜索定位
            nextTick(() => {
                searchQuery.value = currentScript.name;
                showToastMessage('已定位到当前脚本', 'success');
            });
        };

        // 消息提示框方法
        const showToastMessage = (message, type = 'error') => {
            if (toastTimer) {
                clearTimeout(toastTimer);
            }
            toastMessage.value = message;
            toastType.value = type;
            showToast.value = true;

            toastTimer = setTimeout(() => {
                showToast.value = false;
            }, 2000);
        };


        // 监听 isCreatingInSecondLevel 变化，自动设置 createType
        watch(isCreatingInSecondLevel, (isSecondLevel) => {
            // 确保在模态框打开状态下才进行类型调整
            if (showCreateModal.value) {
                createType.value = isSecondLevel ? 'script' : 'folder';
            }
        });

        // 点击外部关闭下拉菜单
        const handleClickOutside = (event) => {
            if (!event.target.closest('.relative')) {
                showServiceDropdown.value = false;
            }
        };

        // 生命周期
        onMounted(() => {
            loadLoginInfo();
            parseParameters();
            loadHelpContent(); // 加载帮助内容

            // 初始化CodeMirror (延迟执行)
            setTimeout(() => {
                let editorContainer = document.querySelector('.bg-gray-50.rounded.overflow-hidden div');
                if (!editorContainer) {
                    // 如果找不到，创建一个容器
                    const parent = document.querySelector('.bg-gray-50.rounded.overflow-hidden');
                    if (parent) {
                        editorContainer = document.createElement('div');
                        editorContainer.className = 'h-96';
                        editorContainer.style.maxHeight = '384px';
                        editorContainer.style.overflow = 'hidden';
                        parent.appendChild(editorContainer);
                    }
                }

                if (editorContainer && typeof CodeMirror !== 'undefined') {
                    codeEditor = CodeMirror(editorContainer, {
                        value: currentScript.content,
                        mode: 'groovy',
                        theme: 'default',
                        lineNumbers: true,
                        lineWrapping: true,
                        autoCloseBrackets: true,
                        matchBrackets: true,
                        styleActiveLine: true,
                        indentUnit: 4,
                        tabSize: 4,
                        indentWithTabs: false,
                        height: "384px",
                        viewportMargin: Infinity,
                        // 启用代码提示
                        hintOptions: {
                            hint: CodeMirror.hint.anyword,
                            completeSingle: false
                        },
                        extraKeys: {
                            "Ctrl-Space": "autocomplete",
                            "Alt-/": "autocomplete"
                        }
                    });

                    // 启用实时代码提示
                    codeEditor.on('inputRead', function (cm, change) {
                        if (!cm.state.completionActive &&
                            change.text[0].match(/[a-zA-Z_$]/)) {
                            CodeMirror.commands.autocomplete(cm, null, {completeSingle: false});
                        }
                    });

                    codeEditor.on('change', () => {
                        currentScript.content = codeEditor.getValue();
                        parseParameters();
                    });

                    // 添加更多Groovy关键词补全
                    const groovyKeywords = [
                        // Groovy关键字
                        'abstract', 'as', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class',
                        'const', 'continue', 'def', 'default', 'do', 'double', 'else', 'enum', 'extends',
                        'false', 'final', 'finally', 'float', 'for', 'goto', 'if', 'implements', 'import',
                        'in', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'null', 'package',
                        'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super',
                        'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'true', 'try',
                        'void', 'volatile', 'while',
                        // Groovy特有
                        'closure', 'each', 'eachWithIndex', 'collect', 'find', 'findAll', 'grep', 'inject',
                        'sort', 'unique', 'flatten', 'reverse', 'max', 'min', 'sum', 'join', 'split',
                        'println', 'print', 'printf', 'sprintf',
                        // 常用方法
                        'toString', 'equals', 'hashCode', 'getClass', 'wait', 'notify', 'notifyAll',
                        'length', 'size', 'isEmpty', 'contains', 'indexOf', 'substring', 'replace',
                        'toLowerCase', 'toUpperCase', 'trim', 'startsWith', 'endsWith'
                    ];

                    // 重写hint函数以包含Groovy关键词
                    CodeMirror.registerHelper("hint", "groovy", function (cm) {
                        const cursor = cm.getCursor();
                        const line = cm.getLine(cursor.line);
                        const word = /[a-zA-Z_$][\w$]*/.exec(line.slice(0, cursor.ch));

                        if (word) {
                            const start = cursor.ch - word[0].length;
                            const end = cursor.ch;
                            const prefix = word[0].toLowerCase();

                            const hints = groovyKeywords
                                .filter(keyword => keyword.toLowerCase().startsWith(prefix))
                                .map(keyword => ({
                                    text: keyword,
                                    displayText: keyword,
                                    className: 'CodeMirror-hint-keyword'
                                }));

                            // 添加当前文档中的词汇
                            const anywordHints = CodeMirror.hint.anyword(cm);
                            if (anywordHints && anywordHints.list) {
                                hints.push(...anywordHints.list
                                    .filter(hint => typeof hint === 'string' &&
                                        hint.toLowerCase().startsWith(prefix) &&
                                        !groovyKeywords.includes(hint))
                                    .map(hint => ({
                                        text: hint,
                                        displayText: hint,
                                        className: 'CodeMirror-hint-variable'
                                    })));
                            }

                            return {
                                list: hints.slice(0, 10), // 限制提示数量
                                from: {line: cursor.line, ch: start},
                                to: {line: cursor.line, ch: end}
                            };
                        }

                        return CodeMirror.hint.anyword(cm);
                    });
                }
            }, 1000);

            document.addEventListener('click', handleClickOutside);
        });

        return {
            // 数据
            currentUser,

            // 环境相关
            availableEnvironments,
            selectedEnvironment,
            selectedEnvironmentName,
            environmentStatusIcon,
            showEnvironmentDropdown,

            availableServices,
            selectedService,
            selectedServiceName,
            serviceStatusIcon,
            showServiceDropdown,
            loadingServices,
            directoryTree,

            // 搜索相关
            searchQuery,
            filteredTree,
            getTotalMatchCount,

            currentScript,
            permissionJson,
            permissionError,
            isEditingPermission,
            canEdit,
            scriptParameters,
            parameterValues,
            executionResult,
            isExecuting,
            showPreviewModal,
            previewCode,

            // 展开模态框相关
            showExpandedCodeModal,
            expandedCodeTitle,
            expandedCodeContent,
            expandedCodeMode,

            // 帮助指南相关
            showHelpModal,
            helpContent,
            formattedHelpContent,

            // 创建对话框相关
            showCreateModal,
            createType,
            createName,
            createModalTitle,
            isCreatingInSecondLevel,
            isCreating,

            // 重命名对话框相关
            showRenameModal,
            renameTargetNode,
            renameNewName,
            isRenaming,

            // 删除确认对话框相关
            showDeleteModal,
            deleteTargetNode,
            deleteForceDelete,
            isDeleting,

            // 消息提示框相关
            showToast,
            toastMessage,
            toastType,

            // 历史记录相关
            executionHistory,
            showHistoryListModal,
            showHistoryDetailModal,
            selectedHistory,
            historyCurrentPage,
            historyTotalPages,
            historyTotalRecords,

            // 方法
            openHistoryListModal,
            closeHistoryListModal,
            showHistoryDetail,
            closeHistoryDetailModal,
            restoreParameters,
            changeHistoryPage,
            backToHistoryList,

            // 方法
            // 环境相关方法
            toggleEnvironmentDropdown,
            selectEnvironment,

            toggleServiceDropdown,
            selectService,
            selectNode,
            parseParameters,
            clearParameter,
            saveScript,
            executeScript,
            closePreviewModal,
            handlePreviewClick,
            copyPreviewCode,
            showExpandedCode,
            closeExpandedCodeModal,
            copyExpandedCode,
            showCreateDialog,
            closeCreateModal,
            confirmCreate,

            // 帮助指南方法
            closeHelpModal,

            // 删除和重命名方法
            handleDeleteNode,
            handleRenameNode,
            confirmDelete,
            confirmRename,
            closeDeleteModal,
            closeRenameModal,

            // 搜索和定位方法
            clearSearch,
            locateCurrentScript,

            // 消息提示框方法
            showToastMessage
        };
    }
}).mount('#app');
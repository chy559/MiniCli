# Mini Agent CLI

一个基于 Java 17 + Maven 的本地 Agent CLI MVP，当前实现了几条主线：

- 默认 `ReAct` 执行循环
- `Memory` 短期 / 长期记忆
- 面向代码库的轻量 RAG
- `/plan` 触发的 `Plan + DAG` 执行模式
- Tool 调用、MCP 工具接入和 HITL 人工审批

## 当前实现范围

已实现：

- 交互式 CLI
- 普通输入走 ReAct
- `/plan <task>`
- `/save <fact>`
- `/memory`
- `/memory clear`
- `/rag`
- `/rag index`
- `/rag search <query>`
- `/exit`
- 基础工具：
  - `read_file`
  - `write_file`
  - `list_dir`
  - `execute_command`
  - `save_memory`
  - `search_code`
  - `index_code`
- 长期记忆 JSON 持久化
- 关键词检索的相关记忆注入
- 短期记忆 Map-Reduce 压缩
- 对话历史压缩
- 代码库 RAG：SQLite 持久化 chunk 和 JSON 向量，内存余弦相似度检索
- 混合检索：语义检索 + 中文/code 分词加权 + 代码类型加分 + 同文件限流
- Tool 并发执行
- Plan DAG 中同批可执行节点并发执行
- HITL 人工审批层：只读工具免审批，写入/执行/外部工具执行前询问用户
- MCP stdio 工具接入：通过 `~/.mini-agent/mcp.json` 动态注册工具
- DAG 解析、拓扑排序、执行失败传播
- 单元测试

## 项目结构

```text
src/main/java/com/example/miniagent/
├── cli/
├── agent/
├── plan/
├── memory/
├── rag/
├── mcp/
├── llm/
├── tool/
└── prompt/
```

## 环境要求

- Java 17
- Maven 3.9+

## API Key 和模型配置

本项目不会在代码里硬编码任何密钥。运行前请通过环境变量配置。

### 必填

```powershell
$env:MINI_AGENT_API_KEY="你的 API Key"
```

### 选填

```powershell
$env:MINI_AGENT_BASE_URL="https://你的兼容接口/v1"
$env:MINI_AGENT_MODEL="gpt-4o-mini"
```

默认值：

- `MINI_AGENT_BASE_URL=https://api.openai.com/v1`
- `MINI_AGENT_MODEL=gpt-4o-mini`

## 运行测试

```powershell
mvn test
```

## 启动 CLI

```powershell
mvn exec:java
```

启动后可使用：

```text
> Summarize this project
> /save User prefers Chinese explanations and English code comments
> /memory
> /rag index
> /rag search memory compression
> /plan Analyze the module, modify the implementation, then verify it
> /exit
```

## 长期记忆文件位置

默认保存在：

```text
~/.mini-agent/memory.json
```

Windows 下一般对应类似：

```text
C:\Users\<YourUser>\.mini-agent\memory.json
```

## 代码库 RAG 文件位置

默认保存在：

```text
~/.mini-agent/code-rag.sqlite
```

RAG 不会在每次普通 ReAct 前自动检索并注入代码片段。它通过以下入口使用：

- `/rag search <query>`
- `search_code` tool
- `/rag index`
- `index_code` tool

## MCP 配置

默认读取：

```text
~/.mini-agent/mcp.json
```

示例：

```json
{
  "servers": [
    {
      "name": "github",
      "transport": "stdio",
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_TOKEN": "${GITHUB_TOKEN}"
      }
    }
  ]
}
```

MCP 工具会以 `mcp_<server>_<tool>` 的名字注册进 `ToolRegistry`。

## 说明

这一版优先保证结构清晰和可测试：

- `Agent` 只负责 ReAct
- `PlanExecuteAgent` 只负责计划执行
- `MemoryManager` 只负责记忆
- `ToolRegistry` 是唯一工具执行入口
- `HitlToolRegistry` 继承 `ToolRegistry`，负责工具执行前审批
- `CodebaseRagService` 只负责代码库索引和检索
- `McpToolAdapter` 把 MCP 工具适配成普通 Tool

短期记忆压缩目前是启发式 Map-Reduce 摘要；conversationHistory 压缩仍是 MVP 级启发式摘要。后续可以替换为真实 LLM 压缩或更精确的 tokenizer/window 管理而不破坏整体结构。

# Mini Agent CLI

一个基于 Java 17 + Maven 的本地 Agent CLI MVP，按设计文档实现了三条主线：

- 默认 `ReAct` 执行循环
- `Memory` 短期 / 长期记忆
- `/plan` 触发的 `Plan + DAG` 执行模式

## 当前实现范围

已实现：

- 交互式 CLI
- 普通输入走 ReAct
- `/plan <task>`
- `/save <fact>`
- `/memory`
- `/memory clear`
- `/exit`
- 基础工具：
  - `read_file`
  - `write_file`
  - `list_dir`
  - `execute_command`
- 长期记忆 JSON 持久化
- 关键词检索的相关记忆注入
- 短期记忆压缩
- 对话历史压缩
- DAG 解析、拓扑排序、执行失败传播
- 单元测试

## 项目结构

```text
src/main/java/com/example/miniagent/
├── cli/
├── agent/
├── plan/
├── memory/
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

## 说明

这一版优先保证结构清晰和可测试：

- `Agent` 只负责 ReAct
- `PlanExecuteAgent` 只负责计划执行
- `MemoryManager` 只负责记忆
- `ToolRegistry` 是唯一工具执行入口

目前的压缩策略是 MVP 级别的启发式摘要，后续可以替换为真实的 LLM 压缩实现而不破坏整体结构。

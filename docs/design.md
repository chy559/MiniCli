Mini Agent CLI Design
Goal
Build a minimal local Agent CLI from scratch with only three core capabilities:

ReAct: a default single-agent loop that can think, call tools, observe results, and continue until it produces a final answer
Memory: short-term memory, long-term memory, context compression, and relevant memory injection
Plan + DAG: for complex tasks, generate a plan first, model it as a DAG, then execute tasks by dependency order
This project is not intended to fully replicate a mature product. The goal is to create a clean, extensible MVP that is easy to continue evolving.

Scope
In scope
Interactive CLI
Default ReAct execution mode
/plan <task> for plan execution mode
/save <fact> for long-term memory
/memory to inspect memory status
/memory clear to clear long-term memory
Basic tools:
read_file
write_file
list_dir
execute_command
Out of scope
Multi-Agent
MCP
Browser integration
RAG / vector retrieval
Approval flow / HITL
Rich TUI
Runtime API
Multi-model switching
High-Level Architecture
CLI
 ├─ Main
 └─ CommandParser

Core
 ├─ Agent                # ReAct loop
 ├─ PlanExecuteAgent     # Plan entry
 ├─ Planner              # Plan generation
 ├─ ExecutionPlan        # DAG model
 ├─ Task                 # DAG node
 └─ MemoryManager        # Memory facade

Infra
 ├─ LlmClient            # LLM abstraction
 ├─ ToolRegistry         # Tool registration and execution
 ├─ ConversationCompactor
 └─ LongTermMemoryStore
Design principles:

Agent is responsible only for ReAct
PlanExecuteAgent is responsible only for planning and execution
MemoryManager owns memory logic only
ToolRegistry is the single tool execution entry
Keep modules loosely coupled so they are easy to replace or extend later
ReAct Design
Purpose
ReAct is the default execution path for simple tasks, exploratory tasks, and small multi-step workflows.

Flow
Accept user input
Store it in short-term memory
Retrieve relevant facts from long-term memory
Inject those facts into system/context prompt
Call the LLM
If the LLM returns tool calls:
execute tools
append tool results into conversation history
store summarized tool results in short-term memory
continue the loop
If the LLM does not call tools:
output the final answer
store the assistant response in short-term memory
Pseudocode
String run(String userInput) {
    memoryManager.addUserMessage(userInput);

    String memoryContext = memoryManager.buildContextForQuery(userInput, maxTokens);
    updateSystemPrompt(memoryContext);

    conversationHistory.add(userMessage(userInput));

    while (true) {
        maybeCompactConversationHistory();

        ChatResponse response = llmClient.chat(conversationHistory, toolDefinitions);

        if (response.hasToolCalls()) {
            conversationHistory.add(assistantToolCallMessage(response));

            List<ToolResult> results = toolRegistry.executeTools(response.toolCalls());

            for (ToolResult result : results) {
                memoryManager.addToolResult(result.toolName(), result.content());
                conversationHistory.add(toolMessage(result));
            }

            continue;
        }

        memoryManager.addAssistantMessage(response.content());
        conversationHistory.add(assistantMessage(response.content()));
        return response.content();
    }
}
Constraints
Set a maximum loop count to prevent infinite loops
Tool results must be summarized or truncated before going into memory
Check context size before each model call
Tool failures must be returned back to the model rather than silently swallowed
Memory Design
Memory has two layers: short-term memory and long-term memory.

Short-Term Memory
Purpose
Store working context for the current session, including:

user messages
assistant replies
summarized tool results
compressed summary blocks
Data Model
class MemoryEntry {
    String id;
    String content;
    MemoryType type; // CONVERSATION, TOOL_RESULT, FACT, SUMMARY
    Map<String, String> metadata;
    int tokenEstimate;
    long createdAt;
}
Notes
Tool results should be stored as summaries rather than full raw output
Long text should be truncated
When short-term memory reaches a budget threshold, trigger compression
Long-Term Memory
Purpose
Store stable facts across sessions, such as:

user preferences
coding style defaults
long-lived project conventions
preferred output language
Write Rules
Long-term memory should only be written when:

the user explicitly runs /save <fact>
the user clearly asks to remember a stable fact
Never auto-save
Do not automatically save these into long-term memory:

current task goals
tool results from the current turn
temporary execution steps
transient short-term chat context
Storage Format
Use a local JSON file for MVP:

[
  {
    "id": "fact-001",
    "content": "User prefers Chinese explanations and English code comments",
    "tags": ["preference"],
    "createdAt": 1710000000000
  }
]
Suggested path:

~/.mini-agent/memory.json
Memory Retrieval
Goal
Inject only the most relevant long-term memory into each run, instead of dumping everything into prompt context.

MVP Strategy
No embeddings in the first version. Use lightweight keyword retrieval:

tokenize the query
score long-term memories by keyword overlap
take top K
assemble them into a small memory context block
Example
Relevant memory:
- User prefers Chinese explanations and English code comments
- This project uses Maven by default
Compression
Compression must be separated into two different concerns.

A. Short-Term Memory Compression
Target:

shortTermMemory
Purpose:

control memory footprint
Approach:

take older memory entries
summarize them with the LLM
replace them with one SUMMARY entry
B. Conversation History Compression
Target:

conversationHistory
Purpose:

control the actual input size sent to the LLM
Approach:

when history approaches the context window limit
summarize older multi-turn conversation into one compact message
keep the most recent raw turns intact
Key Rule
Short-term memory compression and conversation history compression must not be treated as the same thing.

Plan + DAG Design
Goal
Complex tasks should not be handled by plain ReAct directly. Instead:

generate a plan
represent tasks structurally
attach dependencies
execute tasks by DAG order
summarize results
This mode is suited for requests like:

analyze, then modify, then verify
refactor with explicit staged execution
multi-step engineering tasks with dependency order
Planner Output Format
The planner must produce strict JSON.

{
  "summary": "Analyze the module, implement changes, and verify the result",
  "tasks": [
    {
      "id": "t1",
      "description": "Read the target module and related callers to understand current behavior",
      "type": "ANALYSIS",
      "dependencies": []
    },
    {
      "id": "t2",
      "description": "Modify the target module implementation",
      "type": "FILE_WRITE",
      "dependencies": ["t1"]
    },
    {
      "id": "t3",
      "description": "Run tests or commands to verify the changes",
      "type": "VERIFICATION",
      "dependencies": ["t2"]
    }
  ]
}
Task Model
class Task {
    String id;
    String description;
    TaskType type;
    List<String> dependencies;
    List<String> dependents;
    TaskStatus status;
    String result;
    String error;
}
TaskType
ANALYSIS
FILE_READ
FILE_WRITE
COMMAND
VERIFICATION
TaskStatus
PENDING
RUNNING
COMPLETED
FAILED
SKIPPED
ExecutionPlan Model
class ExecutionPlan {
    String id;
    String goal;
    String summary;
    Map<String, Task> tasks;
    List<String> executionOrder;
    PlanStatus status;
}
PlanStatus
CREATED
RUNNING
COMPLETED
FAILED
CANCELLED
DAG Execution Logic
Execution Steps
parse planner JSON
build the task graph
detect cycles
topologically sort the graph
find all executable tasks
execute tasks batch by batch
mark the plan complete if all tasks succeed
mark the plan failed if any critical task fails
Executable Task Rule
A task can run when:

its status is PENDING
all dependency tasks are COMPLETED
Parallelism Rule
Tasks in the same dependency-free batch may be run in parallel
MVP can execute them serially first, but the design should preserve later parallel expansion
Task Execution Strategy
Each task should be executed by a constrained executor, ideally a small ReAct runner.

Flow
provide one task description
constrain the executor to solve only that task
allow the same toolset
return a task result
persist the result back into plan state
Rule
The task executor should not drift into solving unrelated tasks.

Failure Strategy
For MVP, keep it simple:

if any task fails
mark the whole plan as failed
return the failed task id and failure reason
Possible future extensions:

automatic replan
skip non-critical tasks
user-confirmed resume
Prompt Design
ReAct Prompt
The model should:

understand the user goal first
use tools when necessary
never fabricate file content or command output
continue reasoning from observations
provide a clear final answer
Planner Prompt
The model should:

decompose the task into the minimum necessary steps
output strict JSON only
define correct dependencies
avoid circular dependencies
maximize independence between tasks where reasonable
Task Executor Prompt
The model should:

solve only the current task
use tools if needed
return a compact execution result
explicitly explain failure if the task cannot be completed
Tool System Design
Tool Interface
interface Tool {
    String name();
    String description();
    JsonSchema inputSchema();
    String execute(Map<String, Object> args);
}
ToolRegistry
class ToolRegistry {
    List<ToolDefinition> getToolDefinitions();
    List<ToolResult> executeTools(List<ToolCall> calls);
}
Requirements:

all tools must be executed through ToolRegistry
both ReAct and Plan reuse the same tool layer
tool results should be returned in a structured format
Required Tools
read_file
Input:

path
Output:

file content
write_file
Input:

path
content
Output:

success or failure
list_dir
Input:

path
Output:

file and directory listing
execute_command
Input:

command
Output:

stdout
stderr
exitCode
CLI Design
Default Input
Normal input enters ReAct:

> Summarize the README
/plan <task>
Enter plan mode:

> /plan Analyze this module, modify the implementation, then run tests
/save <fact>
Save long-term memory:

> /save User prefers Chinese explanations and English code comments
/memory
Display memory status:

short-term memory count
long-term memory count
current token estimate
compression threshold status
/memory clear
Clear long-term memory

/exit
Exit the CLI

Data Flow
ReAct Data Flow
User Input
  -> MemoryManager.addUserMessage
  -> retrieve relevant long-term memory
  -> assemble prompt
  -> LLM
  -> tool calls?
      yes -> ToolRegistry.execute
           -> add tool result to memory
           -> add tool result to conversation history
           -> loop
      no  -> add assistant message to memory
           -> return final answer
Plan Data Flow
/plan userInput
  -> Planner.createPlan
  -> parse JSON
  -> build ExecutionPlan
  -> topo sort / dependency resolution
  -> execute tasks batch by batch
  -> collect results
  -> summarize final output
Memory Data Flow
conversation/tool result
  -> short-term memory
  -> compression if needed

/save
  -> long-term memory store

new user input
  -> retrieve relevant long-term memory
  -> inject into prompt
Recommended Directory Structure
src/main/java/com/example/miniagent/
├── cli/
│   ├── Main.java
│   └── CommandParser.java
├── agent/
│   ├── Agent.java
│   └── PlanExecuteAgent.java
├── plan/
│   ├── Planner.java
│   ├── ExecutionPlan.java
│   └── Task.java
├── memory/
│   ├── MemoryManager.java
│   ├── ConversationMemory.java
│   ├── LongTermMemoryStore.java
│   ├── MemoryEntry.java
│   ├── MemoryRetriever.java
│   └── ContextCompressor.java
├── llm/
│   ├── LlmClient.java
│   └── OpenAiCompatibleClient.java
├── tool/
│   ├── ToolRegistry.java
│   ├── ReadFileTool.java
│   ├── WriteFileTool.java
│   ├── ListDirTool.java
│   └── ExecuteCommandTool.java
└── prompt/
    ├── PromptRepository.java
    └── PromptAssembler.java
Error Handling
ReAct
set a maximum loop count
tool exceptions must be surfaced back to the model
repeated tool-call loops should be interrupted
Plan
if planner JSON parsing fails, allow one regeneration attempt
if DAG cycle exists, fail fast
if one task fails, fail the plan
Memory
if the memory file is corrupted, rebuild it safely
if compression fails, keep original content
if retrieval fails, do not block the main flow
Testing Suggestions
ReAct
direct answer with no tool call
single tool call
multi-turn tool calling
tool failure handling
Memory
user message stored into short-term memory
tool result truncation
/save persists long-term memory
relevant memory retrieval
compression triggers at threshold
Plan
planner JSON parsing
topological sorting
cycle detection
execution in dependency order
task failure propagation
CLI
/plan
/save
/memory
/memory clear
/exit
Suggested Build Order
Phase 1: Minimal ReAct Loop
CLI main loop
LlmClient
ToolRegistry
4 base tools
Agent ReAct loop
Phase 2: Memory
short-term memory
long-term memory JSON persistence
keyword retrieval
compression
Phase 3: Plan + DAG
planner prompt
JSON plan parsing
ExecutionPlan / Task
DAG executor
task executor
Implementation Principles
Build a working MVP first, not an overdesigned framework
Keep ReAct, Memory, and Plan clearly separated
All tool execution goes through ToolRegistry
Long-term memory is explicit, not automatic
Strictly distinguish short-term memory compression from conversation history compression
Make DAG execution correct first, then optimize parallelism later
Prioritize clarity of structure over feature breadth
One-Sentence Summary
This is a minimal local Agent CLI that uses ReAct for default execution, Plan + DAG for complex multi-step tasks, and Memory for context and long-lived user preferences, with a simple structure designed for steady future expansion.
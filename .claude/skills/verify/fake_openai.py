import json
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def chunk(content=None, finish_reason=None, tool_calls=None):
    delta = {}
    if content is not None:
        delta["content"] = content
    if tool_calls is not None:
        delta["tool_calls"] = tool_calls
    return {
        "id": "chatcmpl-local-verification",
        "object": "chat.completion.chunk",
        "created": 1762176000,
        "model": "deepseek-chat",
        "choices": [{"index": 0, "delta": delta, "finish_reason": finish_reason}],
    }


def embedding_vector(text):
    vector = [0.0] * 8
    if "微波炉" in text:
        vector[7] = 1.0
    elif "梨制品" in text or "梨膏" in text:
        vector[3] = 1.0
    elif "储存" in text or "保存" in text or "冷藏" in text:
        vector[1] = 1.0
    elif "食用" in text or "配料" in text or "保质期" in text:
        vector[2] = 1.0
    elif "恶意指令示例" in text:
        vector[0] = 1.0
    elif any(keyword in text for keyword in ["售后", "坏果", "品质", "退款", "补发", "审核"]):
        vector[0] = 0.8
        vector[6] = 0.6
    else:
        vector[6] = 1.0
    return vector


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        #1.读取本机应用发来的 OpenAI 兼容请求，不转发到外部服务
        length = int(self.headers.get("Content-Length", "0"))
        request = json.loads(self.rfile.read(length))
        if self.path.endswith("/embeddings"):
            inputs = request["input"] if isinstance(request["input"], list) else [request["input"]]
            data = [
                {"object": "embedding", "embedding": embedding_vector(text), "index": index}
                for index, text in enumerate(inputs)
            ]
            response = {
                "object": "list",
                "data": data,
                "model": request["model"],
                "usage": {"prompt_tokens": 1, "total_tokens": 1},
            }
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(response, ensure_ascii=False).encode())
            return

        user_messages = [message for message in request["messages"] if message["role"] == "user"]
        latest = user_messages[-1]["content"] if user_messages else ""
        tool_messages = [message for message in request["messages"] if message["role"] == "tool"]

        #2.按消息场景模拟工具调用、上游故障、超时、上下文观察或断连
        if "故障" in latest or "fault" in latest:
            self.send_response(500)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            error = {"error": {"message": "local verification failure", "type": "server_error"}}
            self.wfile.write(json.dumps(error).encode())
            return

        if "超时" in latest or "timeout" in latest:
            time.sleep(1)

        #3.通过标准 SSE 协议返回可控模型结果
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-cache")
        self.end_headers()

        if ("价格" in latest or "price" in latest) and not tool_messages:
            tool_call = {
                "index": 0,
                "id": "call-get-sku-1",
                "type": "function",
                "function": {"name": "getSku", "arguments": "{\"skuId\":1}"},
            }
            events = [chunk(finish_reason="tool_calls", tool_calls=[tool_call])]
        elif tool_messages:
            events = [
                chunk(content="实时工具结果：" + tool_messages[-1]["content"]),
                chunk(finish_reason="stop"),
            ]
        elif "context" in latest:
            system_text = "".join(
                message.get("content", "") for message in request["messages"] if message["role"] == "system"
            )
            context = "contextMessages=" + str(len(request["messages"])) + ";hasSummary=" + str(
                "较早对话摘要" in system_text
            )
            events = [chunk(content=context), chunk(finish_reason="stop")]
        elif "disconnect" in latest:
            events = [chunk(content="partial")]
        else:
            events = [
                chunk(content="你好，"),
                chunk(content="这是本地模型替身。"),
                chunk(finish_reason="stop"),
            ]

        for event in events:
            self.wfile.write(("data: " + json.dumps(event, ensure_ascii=False) + "\n\n").encode())
            self.wfile.flush()
            if "disconnect" in latest:
                time.sleep(1)
        self.wfile.write(b"data: [DONE]\n\n")
        self.wfile.flush()

    def log_message(self, format_string, *args):
        print("local model substitute: " + format_string % args, flush=True)


ThreadingHTTPServer(("127.0.0.1", 18081), Handler).serve_forever()

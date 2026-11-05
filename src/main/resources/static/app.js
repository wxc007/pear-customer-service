const categorySelect = document.querySelector('#category');
const spuList = document.querySelector('#spu-list');
const detail = document.querySelector('#detail');
const errorBox = document.querySelector('#error');
const chatMessages = document.querySelector('#chat-messages');
const chatForm = document.querySelector('#chat-form');
const chatInput = document.querySelector('#chat-input');
const chatSend = document.querySelector('#chat-send');
const chatStatus = document.querySelector('#chat-status');
let guestSession = null;

async function problemMessage(response, fallback) {
    const problem = await response.json();
    return problem.detail || `${fallback}：${response.status}`;
}

async function request(path, options = {}) {
    const response = await fetch(path, options);
    if (!response.ok) {
        throw new Error(await problemMessage(response, '请求失败'));
    }
    return response.json();
}

function showError(error) {
    errorBox.textContent = error.message;
    errorBox.hidden = false;
}

function clearError() {
    errorBox.hidden = true;
    errorBox.textContent = '';
}

async function ensureGuestSession() {
    const cached = sessionStorage.getItem('pear.guestSession');
    if (cached) {
        try {
            const parsed = JSON.parse(cached);
            if (Date.parse(parsed.expiresAt) > Date.now()) {
                guestSession = parsed;
                return guestSession;
            }
        } catch (ignored) {
            sessionStorage.removeItem('pear.guestSession');
        }
    }

    guestSession = await request('/api/v1/conversations/guest', {method: 'POST'});
    sessionStorage.setItem('pear.guestSession', JSON.stringify(guestSession));
    return guestSession;
}

function appendMessage(role, content, status = 'COMPLETED') {
    const message = document.createElement('div');
    message.className = `chat-message ${role === 'USER' ? 'from-user' : 'from-agent'}`;
    const author = document.createElement('strong');
    author.textContent = role === 'USER' ? '我' : '智能客服';
    const text = document.createElement('p');
    text.textContent = content;
    message.append(author, text);
    if (status === 'FAILED') {
        message.classList.add('failed');
    }
    chatMessages.append(message);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    return message;
}

async function loadConversation() {
    const session = await ensureGuestSession();
    const messages = await request(`/api/v1/conversations/${session.id}/messages`, {
        headers: {'X-Guest-Token': session.guestToken}
    });
    chatMessages.replaceChildren();
    messages.forEach((message) => {
        const content = message.status === 'STREAMING' ? '上次回复在页面刷新时中断，请重新提问。' : message.content;
        appendMessage(message.role, content, message.status);
    });
    chatStatus.textContent = `游客会话 #${session.id} · 凭证 24 小时内有效`;
}

function handleChatEvent(event, assistantText) {
    if (event.name === 'delta') {
        chatStatus.textContent = '正在接收回复…';
        assistantText.textContent += event.data.content;
    } else if (event.name === 'complete') {
        chatStatus.textContent = '回复完成';
        assistantText.textContent = event.data.content;
    } else if (event.name === 'error') {
        chatStatus.textContent = '回复失败，可稍后重试';
        assistantText.textContent = event.data.content;
        assistantText.parentElement.classList.add('failed');
    }
}

function parseSseBlock(block) {
    let name = 'message';
    const dataLines = [];
    block.split(/\r?\n/).forEach((line) => {
        if (line.startsWith('event:')) {
            name = line.slice(6).trim();
        } else if (line.startsWith('data:')) {
            dataLines.push(line.slice(5).trimStart());
        }
    });
    if (dataLines.length === 0) {
        return null;
    }
    return {name, data: JSON.parse(dataLines.join('\n'))};
}

async function sendChatMessage(content) {
    const session = await ensureGuestSession();
    const response = await fetch(`/api/v1/conversations/${session.id}/messages/stream`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'text/event-stream',
            'X-Guest-Token': session.guestToken
        },
        body: JSON.stringify({content})
    });
    if (!response.ok) {
        throw new Error(await problemMessage(response, '发送失败'));
    }

    const assistant = appendMessage('ASSISTANT', '');
    const assistantText = assistant.querySelector('p');
    const reader = response.body.getReader();
    const decoder = new TextDecoder('utf-8');
    let buffer = '';
    while (true) {
        const {done, value} = await reader.read();
        if (done) {
            break;
        }
        buffer += decoder.decode(value, {stream: true});
        const blocks = buffer.split(/\r?\n\r?\n/);
        buffer = blocks.pop();
        blocks.forEach((block) => {
            const event = parseSseBlock(block);
            if (event) {
                handleChatEvent(event, assistantText);
            }
        });
    }
}

chatForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    const content = chatInput.value.trim();
    if (!content) {
        return;
    }

    clearError();
    chatSend.disabled = true;
    chatInput.disabled = true;
    appendMessage('USER', content);
    chatInput.value = '';
    chatStatus.textContent = '正在等待智能客服…';
    try {
        await sendChatMessage(content);
    } catch (error) {
        chatStatus.textContent = '连接中断，请核对历史记录后重试';
        showError(error);
    } finally {
        chatSend.disabled = false;
        chatInput.disabled = false;
        chatInput.focus();
        try {
            await loadConversation();
        } catch (error) {
            showError(error);
        }
    }
});

document.querySelector('#new-conversation').addEventListener('click', async () => {
    clearError();
    try {
        sessionStorage.removeItem('pear.guestSession');
        guestSession = null;
        await ensureGuestSession();
        chatMessages.replaceChildren();
        chatStatus.textContent = `已创建游客会话 #${guestSession.id}`;
    } catch (error) {
        showError(error);
    }
});

function statusBadge(onSale) {
    const badge = document.createElement('span');
    badge.className = `status ${onSale ? 'on-sale' : 'off-sale'}`;
    badge.textContent = onSale ? '可售' : '已下架';
    return badge;
}

function appendAttributes(container, attributes) {
    const grid = document.createElement('div');
    grid.className = 'attributes';
    Object.entries(attributes).forEach(([name, value]) => {
        const item = document.createElement('div');
        item.className = 'attribute';
        const label = document.createElement('span');
        label.textContent = name;
        const content = document.createElement('strong');
        content.textContent = value;
        item.append(label, content);
        grid.append(item);
    });
    container.append(grid);
}

async function showSku(skuId) {
    clearError();
    try {
        const sku = await request(`/api/v1/catalog/skus/${skuId}`);
        detail.replaceChildren();

        const heading = document.createElement('div');
        heading.className = 'sku-heading';
        const title = document.createElement('h2');
        title.textContent = sku.name;
        heading.append(title, statusBadge(sku.onSale));

        const price = document.createElement('p');
        price.className = 'price';
        price.textContent = `¥${Number(sku.price).toFixed(2)}`;
        const type = document.createElement('p');
        type.textContent = sku.productType === 'FRESH_PEAR' ? '鲜梨 SKU' : '梨制品 SKU';

        detail.append(heading, type, price);
        appendAttributes(detail, sku.attributes);

        const back = document.createElement('button');
        back.type = 'button';
        back.textContent = '返回 SPU 规格列表';
        back.addEventListener('click', () => showSpu(sku.spuId));
        detail.append(back);
    } catch (error) {
        showError(error);
    }
}

async function showSpu(spuId) {
    clearError();
    try {
        const spu = await request(`/api/v1/catalog/spus/${spuId}`);
        detail.replaceChildren();

        const heading = document.createElement('div');
        heading.className = 'sku-heading';
        const title = document.createElement('h2');
        title.textContent = spu.name;
        heading.append(title, statusBadge(spu.onSale));

        const description = document.createElement('p');
        description.textContent = spu.description;
        detail.append(heading, description);
        appendAttributes(detail, spu.attributes);

        spu.skus.forEach((sku) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'sku-card';
            const name = document.createElement('h3');
            name.textContent = sku.name;
            const price = document.createElement('p');
            price.className = 'price';
            price.textContent = `¥${Number(sku.price).toFixed(2)}`;
            button.append(name, price, statusBadge(sku.onSale));
            button.addEventListener('click', () => showSku(sku.id));
            detail.append(button);
        });
    } catch (error) {
        showError(error);
    }
}

async function loadSpus() {
    clearError();
    const categoryId = categorySelect.value;
    const query = categoryId ? `?categoryId=${encodeURIComponent(categoryId)}` : '';
    try {
        const spus = await request(`/api/v1/catalog/spus${query}`);
        spuList.replaceChildren();
        document.querySelector('#spu-count').textContent = `${spus.length} 项`;
        detail.replaceChildren();
        const empty = document.createElement('p');
        empty.className = 'empty';
        empty.textContent = '请从左侧选择一个 SPU。';
        detail.append(empty);

        spus.forEach((spu) => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'spu-card';
            const name = document.createElement('h3');
            name.textContent = spu.name;
            const description = document.createElement('p');
            description.textContent = spu.description;
            button.append(name, description, statusBadge(spu.onSale));
            button.addEventListener('click', () => showSpu(spu.id));
            spuList.append(button);
        });
    } catch (error) {
        showError(error);
    }
}

async function loadCategories() {
    const categories = await request('/api/v1/catalog/categories');
    categories.forEach((category) => {
        const option = document.createElement('option');
        option.value = category.id;
        option.textContent = category.name;
        categorySelect.append(option);
    });
}

categorySelect.addEventListener('change', loadSpus);
document.querySelector('#refresh').addEventListener('click', loadSpus);

Promise.all([
    loadCategories().then(loadSpus),
    loadConversation()
]).catch(showError);

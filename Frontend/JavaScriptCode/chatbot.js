// Chatbot Widget JavaScript
const CHATBOT_API_BASE = "http://localhost:8080/api/chatbot";

class ChatbotWidget {
    constructor() {
        this.isOpen = false;
        this.isMaximized = false;
        this.currentClientId = null;
        this.currentPage = 'dashboard';
        this.visualizationContext = null;
        this.chatHistory = [];
        this.init();
    }

    init() {
        this.createWidget();
        this.attachEventListeners();
        this.loadChatHistory();
    }

    createWidget() {
        const widget = document.createElement('div');
        widget.className = 'chatbot-widget';
        widget.innerHTML = `
            <div class="chatbot-container" id="chatbotContainer">
                <div class="chatbot-header">
                    <h3>🤖 Portfolio Assistant</h3>
                    <div class="chatbot-header-actions">
                        <button class="header-btn" id="chatbotMaximize" title="Maximize">⛶</button>
                        <button class="close-btn" id="chatbotClose" title="Close">&times;</button>
                    </div>
                </div>
                <div class="chatbot-messages" id="chatbotMessages">
                    <div class="message assistant">
                        <div class="message-bubble">
                            <h3>👋 Hello! I'm your Portfolio Intelligence Assistant</h3>
                            <p>I can help you:</p>
                            <ul>
                                <li>Analyze portfolio performance</li>
                                <li>Explain visualizations and charts</li>
                                <li>Identify risks and opportunities</li>
                                <li>Answer questions about your holdings</li>
                            </ul>
                            <p><strong>Try asking:</strong> "Explain this graph" or "What are the risks in my portfolio?"</p>
                        </div>
                    </div>
                </div>
                <div class="suggested-questions" id="suggestedQuestions"></div>
                <div class="chatbot-input-area">
                    <textarea 
                        class="chatbot-input" 
                        id="chatbotInput" 
                        placeholder="Ask me anything about your portfolio..."
                        rows="1"
                    ></textarea>
                    <button class="chatbot-send-btn" id="chatbotSend">Send</button>
                </div>
            </div>
            <button class="chatbot-toggle" id="chatbotToggle">
                💬
            </button>
        `;
        document.body.appendChild(widget);
    }

    attachEventListeners() {
        const toggle = document.getElementById('chatbotToggle');
        const close = document.getElementById('chatbotClose');
        const send = document.getElementById('chatbotSend');
        const input = document.getElementById('chatbotInput');

        const maximizeBtn = document.getElementById('chatbotMaximize');
        toggle.addEventListener('click', () => this.toggle());
        close.addEventListener('click', () => this.toggle());
        if (maximizeBtn) maximizeBtn.addEventListener('click', (e) => { e.stopPropagation(); this.toggleMaximize(); });
        send.addEventListener('click', () => this.sendMessage());
        
        input.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                this.sendMessage();
            }
        });

        input.addEventListener('input', () => {
            input.style.height = 'auto';
            input.style.height = Math.min(input.scrollHeight, 100) + 'px';
        });
    }

    toggle() {
        this.isOpen = !this.isOpen;
        const container = document.getElementById('chatbotContainer');
        const toggleBtn = document.getElementById('chatbotToggle');
        
        if (this.isOpen) {
            container.classList.add('active');
            toggleBtn.classList.add('active');
            document.getElementById('chatbotInput').focus();
        } else {
            container.classList.remove('active');
            toggleBtn.classList.remove('active');
            this.isMaximized = false;
            container.classList.remove('maximized');
            this.updateMaximizeButton();
        }
    }

    toggleMaximize() {
        this.isMaximized = !this.isMaximized;
        const container = document.getElementById('chatbotContainer');
        if (this.isMaximized) {
            container.classList.add('maximized');
        } else {
            container.classList.remove('maximized');
        }
        this.updateMaximizeButton();
    }

    updateMaximizeButton() {
        const btn = document.getElementById('chatbotMaximize');
        if (!btn) return;
        btn.title = this.isMaximized ? 'Restore' : 'Maximize';
        btn.textContent = this.isMaximized ? '⛶' : '⛶';
        btn.classList.toggle('maximized', this.isMaximized);
    }

    async sendMessage() {
        const input = document.getElementById('chatbotInput');
        const message = input.value.trim();
        
        if (!message) return;

        // Add user message to chat
        this.addMessage('user', message);
        input.value = '';
        input.style.height = 'auto';

        // Show typing indicator
        const typingId = this.showTypingIndicator();

        try {
            // Update current context
            this.updateContext();

            const response = await fetch(`${CHATBOT_API_BASE}/chat`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    message: message,
                    clientId: this.currentClientId != null ? this.currentClientId : null,
                    currentPage: this.currentPage || 'dashboard',
                    visualizationContext: this.visualizationContext
                })
            });

            const data = await response.json();
            
            // Remove typing indicator
            this.removeTypingIndicator(typingId);

            // Add assistant response
            this.addMessage('assistant', data.response, data.confidence, data.type);
            
            // Show suggested questions
            if (data.suggestedQuestions && data.suggestedQuestions.length > 0) {
                this.showSuggestedQuestions(data.suggestedQuestions);
            }

            // Clear visualization context after use
            this.visualizationContext = null;

        } catch (error) {
            console.error('Chatbot error:', error);
            this.removeTypingIndicator(typingId);
            this.addMessage('assistant', 
                '## Error\n\nI encountered an error processing your request. Please check your connection and try again.',
                'LOW',
                'INFORMATIONAL'
            );
        }
    }

    addMessage(role, content, confidence, type) {
        const messagesContainer = document.getElementById('chatbotMessages');
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${role}`;

        const bubble = document.createElement('div');
        bubble.className = 'message-bubble';
        
        // Render markdown-like content
        bubble.innerHTML = this.renderMarkdown(content);

        const meta = document.createElement('div');
        meta.className = 'message-meta';
        if (confidence) {
            const badge = document.createElement('span');
            badge.className = `confidence-badge ${confidence.toLowerCase()}`;
            badge.textContent = confidence;
            meta.appendChild(badge);
        }
        if (type) {
            const typeSpan = document.createElement('span');
            typeSpan.textContent = ` • ${type}`;
            meta.appendChild(typeSpan);
        }

        messageDiv.appendChild(bubble);
        if (meta.children.length > 0) {
            messageDiv.appendChild(meta);
        }
        messagesContainer.appendChild(messageDiv);

        // Scroll to bottom
        messagesContainer.scrollTop = messagesContainer.scrollHeight;

        // Save to history
        this.chatHistory.push({ role, content, timestamp: new Date() });
        this.saveChatHistory();
    }

    renderMarkdown(text) {
        // Simple markdown rendering
        let html = text;
        
        // Headers
        html = html.replace(/## (.*?)(\n|$)/g, '<h2>$1</h2>');
        html = html.replace(/### (.*?)(\n|$)/g, '<h3>$1</h3>');
        
        // Bold and italic
        html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
        
        // Lists - handle bullet points
        const lines = html.split('\n');
        let inList = false;
        let result = [];
        
        for (let line of lines) {
            if (line.trim().startsWith('•') || line.trim().startsWith('-')) {
                if (!inList) {
                    result.push('<ul>');
                    inList = true;
                }
                result.push('<li>' + line.trim().substring(1).trim() + '</li>');
            } else {
                if (inList) {
                    result.push('</ul>');
                    inList = false;
                }
                if (line.trim()) {
                    result.push(line);
                }
            }
        }
        if (inList) {
            result.push('</ul>');
        }
        
        html = result.join('\n');
        
        // Convert remaining newlines to breaks
        html = html.replace(/\n/g, '<br>');
        
        return html;
    }

    showTypingIndicator() {
        const messagesContainer = document.getElementById('chatbotMessages');
        const typingDiv = document.createElement('div');
        typingDiv.className = 'message assistant';
        typingDiv.id = 'typingIndicator';
        typingDiv.innerHTML = `
            <div class="message-bubble">
                <div class="typing-indicator">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
            </div>
        `;
        messagesContainer.appendChild(typingDiv);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
        return 'typingIndicator';
    }

    removeTypingIndicator(id) {
        const indicator = document.getElementById(id);
        if (indicator) {
            indicator.remove();
        }
    }

    showSuggestedQuestions(questions) {
        const container = document.getElementById('suggestedQuestions');
        container.innerHTML = '';
        
        questions.forEach(question => {
            const btn = document.createElement('button');
            btn.className = 'suggested-question';
            btn.textContent = question;
            btn.addEventListener('click', () => {
                document.getElementById('chatbotInput').value = question;
                this.sendMessage();
            });
            container.appendChild(btn);
        });
    }

    updateContext() {
        
        const clientSelect = document.getElementById('globalClientSelect');
        if (clientSelect) {
            const val = clientSelect.value;
            if (val && val.trim() !== '') {
                const id = parseInt(val, 10);
                this.currentClientId = isNaN(id) ? null : id;
            } else {
                this.currentClientId = null;
            }
        }

      
        const activePage = document.querySelector('.page-view.active');
        if (activePage) {
            this.currentPage = activePage.id || 'dashboard';
        }
    }

    setVisualizationContext(chartType, chartId, metadata = {}) {
        this.visualizationContext = {
            chartType: chartType,
            chartId: chartId,
            xAxis: metadata.xAxis,
            yAxis: metadata.yAxis,
            assetSymbol: metadata.assetSymbol,
            portfolioId: metadata.portfolioId,
            timeRange: metadata.timeRange,
            calculatedMetrics: metadata.calculatedMetrics,
            hoverData: metadata.hoverData
        };
    }

    loadChatHistory() {
        const saved = localStorage.getItem('chatbot_history');
        if (saved) {
            try {
                this.chatHistory = JSON.parse(saved);
                // Optionally restore chat history UI
            } catch (e) {
                console.error('Error loading chat history', e);
            }
        }
    }

    saveChatHistory() {
        // Keep only last 50 messages
        const recentHistory = this.chatHistory.slice(-50);
        localStorage.setItem('chatbot_history', JSON.stringify(recentHistory));
    }
}

// Initialize chatbot when DOM is ready
let chatbotInstance = null;
document.addEventListener('DOMContentLoaded', () => {
    chatbotInstance = new ChatbotWidget();
    
    // Make it globally accessible for visualization integration
    window.chatbot = chatbotInstance;
});

// Helper function for visualization integration
function explainVisualization(chartType, chartId, metadata = {}) {
    if (window.chatbot) {
        window.chatbot.setVisualizationContext(chartType, chartId, metadata);
        window.chatbot.toggle();
        // Auto-send explanation request
        setTimeout(() => {
            document.getElementById('chatbotInput').value = 'Explain this graph';
            window.chatbot.sendMessage();
        }, 300);
    }
}

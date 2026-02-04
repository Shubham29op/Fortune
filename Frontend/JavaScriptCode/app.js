const API_BASE = "http://localhost:8080/api";

// State Management
let currentHoldings = [];
let assetCatalog = [];
let watchlist = JSON.parse(localStorage.getItem('mgr_watchlist')) || [];
let historyLog = JSON.parse(localStorage.getItem('mgr_history')) || [];
let liveInterval = null;
let chartInstance = null;
let allocationChart = null;
let monthlyChart = null;
let recentTxChart = null;
let topHoldingsChart = null;
let assetTrendInstance = null;
let assetShareInstance = null;
let currentAssetHoldingId = null;
let compChart1 = null;
let compChart2 = null;
let pendingSellHoldingId = null;

// --- Init ---
document.addEventListener('DOMContentLoaded', async () => {
    updateTime();
    await fetchAssets();
    await fetchClients();
    renderWatchlist();
    startLiveEngine();

    // Ensure modals are hidden on load
    const tm = document.getElementById('tradeModal'); if (tm) tm.style.display = 'none';
    const wm = document.getElementById('wlModal'); if (wm) wm.style.display = 'none';
    const am = document.getElementById('assetModal'); if (am) am.style.display = 'none';
    const sm = document.getElementById('sellModal'); if (sm) sm.style.display = 'none';
    
    // Auto-load history
    renderHistoryTable();
    updateRealizedKPI();
});

// --- Navigation ---
function switchPage(pageId) {
    document.querySelectorAll('.page-view').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-menu li').forEach(l => l.classList.remove('active'));
    document.getElementById(pageId).classList.add('active');

    if (pageId === 'risk') calculateRiskProfile();
}

// --- Data Fetching ---
async function fetchAssets() {
    try {
        const res = await fetch(`${API_BASE}/assets`);
        assetCatalog = await res.json();
        
        // Populate Selects
        const tradeSel = document.getElementById('tradeAsset');
        const wlSel = document.getElementById('wlAssetSelect');
        tradeSel.innerHTML = wlSel.innerHTML = '';
        
        assetCatalog.forEach(a => {
            const opt = `<option value="${a.assetId}" data-symbol="${a.symbol}">${a.symbol} - ${a.assetName}</option>`;
            tradeSel.innerHTML += opt;
            wlSel.innerHTML += opt;
        });
    } catch(e) { console.error("Asset API Error", e); }
}

async function fetchClients() {
    try {
        const res = await fetch(`${API_BASE}/clients`);
        const clients = await res.json();
        
        // 1. Global Selector
        const sel = document.getElementById('globalClientSelect');
        sel.innerHTML = '<option value="">-- Select Account --</option>';
        
        // 2. Comparison Selectors
        const comp1 = document.getElementById('compClient1');
        const comp2 = document.getElementById('compClient2');
        if(comp1) comp1.innerHTML = '<option value="">-- Client A --</option>';
        if(comp2) comp2.innerHTML = '<option value="">-- Client B --</option>';

        clients.forEach(c => {
            const opt = `<option value="${c.clientId}">${c.fullName}</option>`;
            sel.innerHTML += opt;
            if(comp1) comp1.innerHTML += opt;
            if(comp2) comp2.innerHTML += opt;
        });
    } catch(e) { console.error("Client API Error", e); }
}

async function loadGlobalContext() {
    const clientId = document.getElementById('globalClientSelect').value;
    if (!clientId) return;

    try {
        const res = await fetch(`${API_BASE}/portfolio/${clientId}`);
        const rawData = await res.json();
        
        // MOCK LIVE MARKET: Apply random noise
        currentHoldings = rawData.map(h => {
            const volatility = h.asset.category === 'COMMODITY' ? 0.08 : 0.04; 
            const noise = (Math.random() * volatility * 2) - volatility; 
            const curPrice = h.avgBuyPrice * (1 + noise);
            
            return {
                ...h,
                curPrice: curPrice,
                mktValue: curPrice * h.quantity,
                invested: h.avgBuyPrice * h.quantity,
                pnl: (curPrice - h.avgBuyPrice) * h.quantity,
                pnlPct: noise * 100
            };
        });

        updateDashboard();
        renderHoldings();
        renderCharts();

        // If Risk Page is open, refresh it
        if(document.getElementById('risk').classList.contains('active')) calculateRiskProfile();

    } catch(e) { console.error("Portfolio Error", e); }
}

// --- Dashboard Logic ---
function updateDashboard() {
    let totalInvested = 0, totalVal = 0, totalUnrealized = 0;

    currentHoldings.forEach(h => {
        totalInvested += h.invested;
        totalVal += h.mktValue;
        totalUnrealized += h.pnl;
    });

    const fmt = n => n.toLocaleString('en-US', {style:'currency', currency:'USD'});

    document.getElementById('kpiNetWorth').innerText = fmt(totalVal);
    document.getElementById('kpiInvested').innerText = fmt(totalInvested);
    
    const pnlEl = document.getElementById('kpiUnrealized');
    pnlEl.innerText = (totalUnrealized>=0?"+":"") + fmt(totalUnrealized);
    pnlEl.className = totalUnrealized >= 0 ? "text-up" : "text-down";
    
    const badge = document.getElementById('kpiUnrealizedBadge');
    badge.innerText = (totalInvested>0 ? (totalUnrealized/totalInvested)*100 : 0).toFixed(2) + "%";
    badge.style.background = totalUnrealized >= 0 ? "rgba(16,185,129,0.2)" : "rgba(239,68,68,0.2)";
    badge.style.color = totalUnrealized >= 0 ? "#10b981" : "#ef4444";
}

function updateRealizedKPI() {
    const totalRealized = historyLog.reduce((sum, item) => sum + item.profit, 0);
    const el = document.getElementById('kpiRealized');
    el.innerText = (totalRealized>=0?"+":"") + totalRealized.toLocaleString('en-US', {style:'currency', currency:'USD'});
    el.className = totalRealized >= 0 ? "text-up" : "text-down";
}

// --- RISK ANALYSIS ENGINE (Preserved & Merged) ---
function calculateRiskProfile() {
    // Standard Beta Weights
    const weights = { 'COMMODITY': 1.5, 'NSE': 1.0, 'MF': 0.7 };
    
    let totalValue = 0;
    let weightedBetaSum = 0;
    let categoryExposure = { 'COMMODITY': 0, 'NSE': 0, 'MF': 0 };
    
    // Risk Contribution Table Data
    let riskContributors = [];

    currentHoldings.forEach(h => {
        const val = h.mktValue;
        const cat = h.asset.category;
        const assetBeta = weights[cat] || 1.0;
        
        totalValue += val;
        categoryExposure[cat] = (categoryExposure[cat] || 0) + val;
        weightedBetaSum += (val * assetBeta);

        riskContributors.push({
            symbol: h.asset.symbol,
            val: val,
            beta: assetBeta,
            contrib: val * assetBeta
        });
    });

    if (totalValue === 0) {
        document.getElementById('riskContent').innerHTML = '<div class="panel"><p style="color:#aaa">No assets found for this client. Please add holdings to calculate risk.</p></div>';
        return;
    }

    const portfolioBeta = weightedBetaSum / totalValue;
    
    // Determine Risk Label
    let riskLabel = "MODERATE";
    let riskColor = "#f59e0b"; // Orange
    if(portfolioBeta < 0.8) { riskLabel = "CONSERVATIVE"; riskColor = "#10b981"; } // Green
    if(portfolioBeta > 1.2) { riskLabel = "AGGRESSIVE"; riskColor = "#ef4444"; }   // Red

    // Sort contributors by risk impact
    riskContributors.sort((a, b) => b.contrib - a.contrib);

    // Generate Risk Contribution Rows
    const contribRows = riskContributors.slice(0, 3).map(r => `
        <tr>
            <td style="color:var(--text-main)">${r.symbol}</td>
            <td style="text-align:right">$${r.val.toLocaleString()}</td>
            <td style="text-align:right; color:${r.beta > 1 ? '#ef4444' : '#10b981'}">${r.beta.toFixed(2)}</td>
            <td style="text-align:right; font-weight:bold">${((r.contrib / weightedBetaSum) * 100).toFixed(1)}%</td>
        </tr>
    `).join('');

    const riskHtml = `
        <div class="kpi-row">
            <div class="kpi-card" style="border-left: 4px solid ${riskColor}">
                <span class="kpi-label">Portfolio Beta</span>
                <h2>${portfolioBeta.toFixed(2)}</h2>
                <small style="color:${riskColor}; font-weight:bold">${riskLabel}</small>
            </div>
            <div class="kpi-card">
                <span class="kpi-label">Commodity (High Vol)</span>
                <h2>${((categoryExposure['COMMODITY'] / totalValue) * 100).toFixed(1)}%</h2>
            </div>
            <div class="kpi-card">
                <span class="kpi-label">VaR (95% Daily)</span>
                <h2 class="text-down">-$${(totalValue * 0.0165 * portfolioBeta).toFixed(2)}</h2>
                <small class="neutral">Est. Max Daily Loss</small>
            </div>
            <div class="kpi-card">
                <span class="kpi-label">Diversification Score</span>
                <h2>${(100 - (portfolioBeta * 20)).toFixed(0)}/100</h2>
            </div>
        </div>

        <div style="display:grid; grid-template-columns: 2fr 1fr; gap:20px;">
            <div class="panel">
                <h3>⚠️ Top Risk Contributors</h3>
                <table class="trade-table" style="margin-top:15px">
                    <thead>
                        <tr><th style="padding-left:0">Asset</th><th style="text-align:right">Value</th><th style="text-align:right">Beta</th><th style="text-align:right">Risk Contrib %</th></tr>
                    </thead>
                    <tbody>
                        ${contribRows}
                    </tbody>
                </table>
            </div>
            <div class="panel">
                <h3>Analyst Summary</h3>
                <p style="color:#94a3b8; font-size:13px; line-height: 1.6; margin-top:10px;">
                    The portfolio is currently <b>${riskLabel}</b> with a Beta of <b>${portfolioBeta.toFixed(2)}</b> relative to the benchmark. 
                    <br><br>
                    The largest risk concentration is in <b>${riskContributors[0].symbol}</b>, accounting for over 
                    ${((riskContributors[0].contrib / weightedBetaSum) * 100).toFixed(0)}% of the total volatility exposure.
                    <br><br>
                    Recommendation: ${portfolioBeta > 1.1 ? 'Consider hedging with Fixed Income or Gold.' : 'Portfolio is well positioned for stable growth.'}
                </p>
            </div>
        </div>
    `;
    
    document.getElementById('riskContent').innerHTML = riskHtml;
}

// --- Watchlist Logic ---
function addToWatchlist() {
    const sel = document.getElementById('wlAssetSelect');
    const symbol = sel.options[sel.selectedIndex].text.split(" - ")[0];
    const assetId = sel.value;
    
    if(!watchlist.some(w => w.symbol === symbol)) {
        const basePrice = Math.floor(Math.random() * 1900) + 100;
        watchlist.push({ id: assetId, symbol: symbol, price: basePrice, change: 0 });
        saveWatchlist();
        renderWatchlist();
    }
    closeModal('wlModal');
}

function renderWatchlist() {
    const container = document.getElementById('watchlistContainer');
    container.innerHTML = '';
    
    watchlist.forEach(item => {
        const color = item.change >= 0 ? 'text-up' : 'text-down';
        const arrow = item.change >= 0 ? '▲' : '▼';
        
        container.innerHTML += `
            <div class="wl-item">
                <div>
                    <div class="wl-symbol">${item.symbol}</div>
                    <small style="font-size:9px; color:#666">NYSE</small>
                </div>
                <div style="text-align:right">
                    <div class="wl-price">$${item.price.toFixed(2)}</div>
                    <small class="${color}" style="font-size:9px;">${arrow} ${item.change.toFixed(2)}%</small>
                </div>
            </div>
        `;
    });
}

function saveWatchlist() { localStorage.setItem('mgr_watchlist', JSON.stringify(watchlist)); }
function openWatchlistModal() { document.getElementById('wlModal').style.display = 'block'; }

// --- Realized Profit & History Logic ---
function sellAsset(holdingId) { openSellModal(holdingId); }

function renderHistoryTable() {
    const tbody = document.getElementById('historyTable');
    tbody.innerHTML = '';
    
    if(historyLog.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:20px; color:#555">No realized trades yet.</td></tr>';
        // Update charts to reflect empty state
        renderCharts();
        return;
    }

    historyLog.forEach(tx => {
        const color = tx.profit >= 0 ? 'text-up' : 'text-down';
        tbody.innerHTML += `
            <tr>
                <td style="color:#666">${tx.date}</td>
                <td style="font-weight:bold">${tx.symbol}</td>
                <td><span style="background:#cf3030; color:white; font-size:10px; padding:2px 4px; border-radius:3px">SELL</span></td>
                <td>${tx.qty}</td>
                <td>$${tx.buy.toFixed(2)}</td>
                <td>$${tx.sell.toFixed(2)}</td>
                <td class="${color}" style="font-weight:bold">${tx.profit >= 0 ? '+' : ''}$${tx.profit.toFixed(2)}</td>
            </tr>
        `;
    });
}

function clearHistory() {
    if(confirm("Clear local trade history?")) {
        historyLog = [];
        localStorage.removeItem('mgr_history');
        renderHistoryTable();
        updateRealizedKPI();
        renderCharts();
    }
}

// --- Live Engine ---
function startLiveEngine() {
    if(liveInterval) clearInterval(liveInterval);
    setInterval(() => {
        // 1. Tick Watchlist
        watchlist.forEach(w => {
            const move = (Math.random() * 2) - 1; // -1% to +1%
            w.price = w.price * (1 + move/100);
            w.change = move;
        });
        saveWatchlist(); 
        renderWatchlist();

        // 2. Tick Ticker Tape
        const ticker = document.getElementById('tickerTape');
        let html = '';
        watchlist.forEach(w => {
            const c = w.change >= 0 ? 'text-up' : 'text-down';
            const s = w.change >= 0 ? '▲' : '▼';
            html += `<div class="ticker-item">${w.symbol}: <span class="${c}">${s} ${Math.abs(w.change).toFixed(2)}%</span></div>`;
        });
        ticker.innerHTML = html;

        // 3. Update Time
        updateTime();
    }, 2000);
}

// --- Charting ---
function renderCharts() {
    // Allocation
    const ctxAlloc = document.getElementById('allocationChart').getContext('2d');
    const categories = {};
    currentHoldings.forEach(h => {
        categories[h.asset.category] = (categories[h.asset.category] || 0) + h.mktValue;
    });

    if (allocationChart) allocationChart.destroy();
    allocationChart = new Chart(ctxAlloc, {
        type: 'doughnut',
        data: {
            labels: Object.keys(categories),
            datasets: [{
                data: Object.values(categories),
                backgroundColor: ['#3b82f6', '#f59e0b', '#10b981'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { 
                legend: { position: 'right', labels: { color: '#94a3b8' } },
                tooltip: {
                    callbacks: {
                        afterLabel: (context) => {
                            // Add chatbot integration hint
                            return '💬 Click to explain this chart';
                        }
                    }
                }
            },
            onClick: (event, elements) => {
                if (elements.length > 0 && window.chatbot) {
                    const element = elements[0];
                    const label = allocationChart.data.labels[element.index];
                    const value = allocationChart.data.datasets[0].data[element.index];
                    const total = Object.values(categories).reduce((a, b) => a + b, 0);
                    const percentage = ((value / total) * 100).toFixed(1);
                    
                    window.chatbot.setVisualizationContext('doughnut', 'allocationChart', {
                        xAxis: 'Category',
                        yAxis: 'Allocation Percentage',
                        calculatedMetrics: {
                            [label]: percentage + '%',
                            'Total Value': '$' + total.toLocaleString()
                        }
                    });
                    if (window.chatbot.isOpen) {
                        document.getElementById('chatbotInput').value = 'Explain this allocation chart';
                        window.chatbot.sendMessage();
                    } else {
                        window.chatbot.toggle();
                        setTimeout(() => {
                            document.getElementById('chatbotInput').value = 'Explain this allocation chart';
                            window.chatbot.sendMessage();
                        }, 300);
                    }
                }
            }
        }
    });

    // Main Chart (Simple Mock)
    const ctxMain = document.getElementById('mainChart').getContext('2d');
    if (chartInstance) chartInstance.destroy();

    chartInstance = new Chart(ctxMain, {
        type: 'line',
        data: {
            labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
            datasets: [{
                label: 'Performance',
                data: [10000, 10500, 10200, 10800, 11000],
                borderColor: '#3b82f6',
                fill: true,
                backgroundColor: 'rgba(59, 130, 246, 0.1)',
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { 
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        afterLabel: () => '💬 Click to explain this trend'
                    }
                }
            },
            scales: { x: { grid: { display: false } }, y: { grid: { color: '#1e293b' } } },
            onClick: (event, elements) => {
                if (elements.length > 0 && window.chatbot) {
                    const element = elements[0];
                    const label = chartInstance.data.labels[element.index];
                    const value = chartInstance.data.datasets[0].data[element.index];
                    
                    window.chatbot.setVisualizationContext('line', 'mainChart', {
                        xAxis: 'Time',
                        yAxis: 'Portfolio Value',
                        timeRange: '5 days',
                        hoverData: {
                            'Date': label,
                            'Value': '$' + value.toLocaleString()
                        }
                    });
                    if (window.chatbot.isOpen) {
                        document.getElementById('chatbotInput').value = 'Explain this performance trend';
                        window.chatbot.sendMessage();
                    } else {
                        window.chatbot.toggle();
                        setTimeout(() => {
                            document.getElementById('chatbotInput').value = 'Explain this performance trend';
                            window.chatbot.sendMessage();
                        }, 300);
                    }
                }
            }
        }
    });

    // Monthly Realized P&L (last 12 months)
    const monthlyCtx = document.getElementById('monthlyChart').getContext('2d');
    const now = new Date();
    const monthLabels = [];
    const monthKeys = [];
    for (let i = 11; i >= 0; i--) {
        const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
        const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;
        monthKeys.push(key);
        monthLabels.push(d.toLocaleString('en-US', { month: 'short' }) + ' ' + String(d.getFullYear()).slice(-2));
    }
    const monthlyAgg = monthKeys.reduce((acc, k) => (acc[k] = 0, acc), {});
    historyLog.forEach(tx => {
        const d = new Date(tx.date);
        if (!isNaN(d)) {
            const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;
            if (monthlyAgg[key] !== undefined) monthlyAgg[key] += tx.profit;
        }
    });
    if (monthlyChart) monthlyChart.destroy();
    monthlyChart = new Chart(monthlyCtx, {
        type: 'bar',
        data: {
            labels: monthLabels,
            datasets: [{
                label: 'Realized P&L',
                data: monthKeys.map(k => monthlyAgg[k]),
                backgroundColor: monthKeys.map(k => monthlyAgg[k] >= 0 ? 'rgba(16,185,129,0.7)' : 'rgba(239,68,68,0.7)')
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { 
                x: { grid: { display: false }, ticks: { color: '#94a3b8' } },
                y: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } }
            }
        }
    });

    // Recent Transactions (last 8)
    const recentCtx = document.getElementById('recentTxChart').getContext('2d');
    const recent = historyLog.slice(0, 8).reverse();
    if (recentTxChart) recentTxChart.destroy();
    recentTxChart = new Chart(recentCtx, {
        type: 'bar',
        data: {
            labels: recent.map(r => `${r.symbol}`),
            datasets: [{
                label: 'P&L',
                data: recent.map(r => r.profit),
                backgroundColor: recent.map(r => r.profit >= 0 ? 'rgba(16,185,129,0.7)' : 'rgba(239,68,68,0.7)')
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { display: false }, ticks: { color: '#94a3b8' } },
                y: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } }
            }
        }
    });

    // Top 5 Holdings by Market Value
    const topCtx = document.getElementById('topHoldingsChart').getContext('2d');
    const top = [...currentHoldings].sort((a,b) => b.mktValue - a.mktValue).slice(0,5);
    if (topHoldingsChart) topHoldingsChart.destroy();
    topHoldingsChart = new Chart(topCtx, {
        type: 'bar',
        data: {
            labels: top.map(t => t.asset.symbol),
            datasets: [{
                label: 'Market Value',
                data: top.map(t => t.mktValue),
                backgroundColor: '#3b82f6'
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } },
                y: { grid: { display: false }, ticks: { color: '#94a3b8' } }
            }
        }
    });
}

// --- Utils & Client Onboard/Compare ---
function updateTime() { document.getElementById('marketTime').innerText = new Date().toLocaleTimeString(); }

function renderHoldings() {
    const tbody = document.getElementById('holdingsTable');
    tbody.innerHTML = '';
    currentHoldings.forEach(h => {
        const pnlC = h.pnl >= 0 ? 'text-up' : 'text-down';
        tbody.innerHTML += `
            <tr onclick="openAssetDetail(${h.holdingId})">
                <td style="font-weight:bold; color:var(--primary)">${h.asset.symbol}</td>
                <td style="color:#888">${h.asset.assetName}</td>
                <td>${h.quantity}</td>
                <td>$${h.avgBuyPrice.toFixed(2)}</td>
                <td style="font-weight:bold">$${h.curPrice.toFixed(2)}</td>
                <td>$${h.mktValue.toLocaleString(undefined, {minimumFractionDigits:2})}</td>
                <td class="${pnlC}">${h.pnl.toFixed(2)}</td>
                <td class="${pnlC}">${h.pnlPct.toFixed(2)}%</td>
                <td><button class="btn-trade sell" onclick="event.stopPropagation(); sellAsset(${h.holdingId})" style="padding:4px 8px; font-size:11px">CLOSE</button></td>
            </tr>
        `;
    });
}

async function handleOnboard(e) {
    e.preventDefault();
    const name = document.getElementById('newClientName').value;
    const email = document.getElementById('newClientEmail').value;
    try {
        await fetch(`${API_BASE}/clients`, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({fullName:name, email:email, managerId:1})});
        alert("Client Onboarded");
        fetchClients();
    } catch(err) { console.error(err); }
}

function processHoldingsData(holdings) {
    return holdings.map(h => {
        // Simulate market volatility based on asset category
        const volatility = h.asset.category === 'COMMODITY' ? 0.08 : 0.04; 
        const noise = (Math.random() * volatility * 2) - volatility; 
        const curPrice = h.avgBuyPrice * (1 + noise);
        
        return {
            ...h,
            curPrice: curPrice,
            mktValue: curPrice * h.quantity,
            invested: h.avgBuyPrice * h.quantity,
            pnl: (curPrice - h.avgBuyPrice) * h.quantity,
            pnlPct: noise * 100
        };
    });
}

async function runComparison() {
    const c1Id = document.getElementById('compClient1').value;
    const c2Id = document.getElementById('compClient2').value;
    
    if(!c1Id || !c2Id) return alert("Please select two distinct clients to compare.");
    if(c1Id === c2Id) return alert("Select different clients for comparison.");

    try {
        // 1. Parallel Fetch of both portfolios
        const [res1, res2] = await Promise.all([
            fetch(`${API_BASE}/portfolio/${c1Id}`),
            fetch(`${API_BASE}/portfolio/${c2Id}`)
        ]);

        const raw1 = await res1.json();
        const raw2 = await res2.json();

        // 2. Process Data (Calculate Market Values)
        const p1 = processHoldingsData(raw1);
        const p2 = processHoldingsData(raw2);

        // 3. Calculate Aggregates
        const getTotal = (p) => p.reduce((acc, h) => acc + h.mktValue, 0);
        const getAllocation = (p, cat) => p.filter(h => h.asset.category === cat).reduce((acc, h) => acc + h.mktValue, 0);

        const v1 = getTotal(p1);
        const v2 = getTotal(p2);

        // 4. Render Value Comparison Chart (Bar)
        const ctxVal = document.getElementById('compValueChart').getContext('2d');
        if(compChart1) compChart1.destroy();

        compChart1 = new Chart(ctxVal, {
            type: 'bar',
            data: {
                labels: ['Net Liquidation Value', 'Commodity Exposure', 'Equity Exposure'],
                datasets: [
                    {
                        label: 'Client A',
                        data: [v1, getAllocation(p1, 'COMMODITY'), getAllocation(p1, 'NSE')],
                        backgroundColor: '#3b82f6'
                    },
                    {
                        label: 'Client B',
                        data: [v2, getAllocation(p2, 'COMMODITY'), getAllocation(p2, 'NSE')],
                        backgroundColor: '#f59e0b'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { labels: { color: '#94a3b8' } } },
                scales: { 
                    y: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } },
                    x: { ticks: { color: '#94a3b8' } }
                }
            }
        });

        // 5. Render Risk/Return Scatter (Simulated)
        // We calculate a weighted beta for X-axis and a simulated YTD return for Y-axis
        const calcBeta = (p, val) => {
            if(val === 0) return 0;
            const weights = { 'COMMODITY': 1.5, 'NSE': 1.0, 'MF': 0.7 };
            return p.reduce((acc, h) => acc + (h.mktValue * (weights[h.asset.category] || 1)), 0) / val;
        };

        const beta1 = calcBeta(p1, v1);
        const beta2 = calcBeta(p2, v2);

        const ctxScatter = document.getElementById('compScatterChart').getContext('2d');
        if(compChart2) compChart2.destroy();

        compChart2 = new Chart(ctxScatter, {
            type: 'bubble',
            data: {
                datasets: [
                    {
                        label: 'Client A',
                        data: [{ x: beta1, y: (beta1 * 8) + (Math.random()*5), r: 15 }], // Simulated CAPM-like return
                        backgroundColor: '#3b82f6'
                    },
                    {
                        label: 'Client B',
                        data: [{ x: beta2, y: (beta2 * 8) + (Math.random()*5), r: 15 }],
                        backgroundColor: '#f59e0b'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { title: { display: true, text: 'Portfolio Beta (Risk)', color: '#64748b' }, grid: { color: '#1e293b' } },
                    y: { title: { display: true, text: 'Expected Return (%)', color: '#64748b' }, grid: { color: '#1e293b' } }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: (ctx) => `${ctx.dataset.label}: Beta ${ctx.raw.x.toFixed(2)}, Return ${ctx.raw.y.toFixed(2)}%`
                        }
                    }
                }
            }
        });

    } catch(e) { 
        console.error("Comparison Error", e); 
        alert("Failed to run comparison. Ensure both clients have data."); 
    }
}

function openTradeModal(type) { document.getElementById('tradeModal').style.display='block'; document.getElementById('modalTitle').innerText = type+' ORDER'; }
function closeModal(id) { document.getElementById(id).style.display='none'; }
async function executeTrade(e) {
    e.preventDefault();
    const payload = {
        clientId: document.getElementById('globalClientSelect').value,
        assetId: document.getElementById('tradeAsset').value,
        quantity: document.getElementById('tradeQty').value,
        price: document.getElementById('tradePrice').value
    };
    if(!payload.clientId) return alert("Select Client First");
    await fetch(`${API_BASE}/portfolio/buy`, { method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify(payload)});
    closeModal('tradeModal');
    loadGlobalContext();
}

// --- Sell Modal ---
function openSellModal(holdingId) {
    const holding = currentHoldings.find(h => h.holdingId === holdingId);
    if (!holding) return;
    pendingSellHoldingId = holdingId;

    const labelEl = document.getElementById('sellAssetLabel');
    const qtyEl = document.getElementById('sellQty');
    const mktEl = document.getElementById('sellMarketPrice');
    const customEl = document.getElementById('sellCustomPrice');

    if (labelEl) labelEl.value = `${holding.asset.symbol} — ${holding.asset.assetName}`;
    if (qtyEl) qtyEl.value = holding.quantity;
    if (mktEl) mktEl.innerText = fmtUSD(holding.curPrice);
    if (customEl) { customEl.value = ''; customEl.disabled = true; }

    const radios = document.getElementsByName('sellPriceMode');
    if (radios && radios.length) {
        for (const r of radios) {
            r.checked = (r.value === 'market');
            r.onchange = () => {
                const mode = document.querySelector('input[name="sellPriceMode"]:checked')?.value || 'market';
                if (customEl) customEl.disabled = mode !== 'custom';
            };
        }
    }

    const modal = document.getElementById('sellModal');
    if (modal) modal.style.display = 'block';
}

async function executeSellFromModal() {
    if (pendingSellHoldingId == null) return;
    const holding = currentHoldings.find(h => h.holdingId === pendingSellHoldingId);
    if (!holding) return;

    const mode = document.querySelector('input[name="sellPriceMode"]:checked')?.value || 'market';
    let sellPrice = holding.curPrice;
    if (mode === 'custom') {
        const customEl = document.getElementById('sellCustomPrice');
        const val = parseFloat(customEl && customEl.value ? customEl.value : '');
        if (!isFinite(val) || val <= 0) { alert('Enter a valid custom price'); return; }
        sellPrice = val;
    }

    const realizedPnL = (sellPrice - holding.avgBuyPrice) * holding.quantity;
    const record = {
        date: new Date().toLocaleDateString(),
        symbol: holding.asset.symbol,
        type: 'SELL',
        qty: holding.quantity,
        buy: holding.avgBuyPrice,
        sell: sellPrice,
        profit: realizedPnL
    };
    historyLog.unshift(record);
    localStorage.setItem('mgr_history', JSON.stringify(historyLog));

    try {
        await fetch(`${API_BASE}/portfolio/${holding.holdingId}`, { method: 'DELETE' });
        updateRealizedKPI();
        renderHistoryTable();
        loadGlobalContext();
        closeModal('sellModal');
        pendingSellHoldingId = null;
    } catch (e) {
        alert('Execution Failed');
    }
}

// --- Asset Detail Modal ---
function closeAssetDetail() {
    const modal = document.getElementById('assetModal');
    if (modal) modal.style.display = 'none';
    if (assetTrendInstance) { assetTrendInstance.destroy(); assetTrendInstance = null; }
    if (assetShareInstance) { assetShareInstance.destroy(); assetShareInstance = null; }
}

async function openAssetDetail(holdingId) {
    const holding = currentHoldings.find(h => h.holdingId === holdingId);
    if (!holding) return;
    currentAssetHoldingId = holdingId;

    // Header and KPIs
    const invested = holding.avgBuyPrice * holding.quantity;
    const current = holding.curPrice * holding.quantity;
    const pnlAbs = current - invested;
    const pnlPct = invested ? (pnlAbs / invested) * 100 : 0;
    const deltaPct = holding.avgBuyPrice ? ((holding.curPrice - holding.avgBuyPrice) / holding.avgBuyPrice) * 100 : 0;

    setText('assetTitle', `${holding.asset.symbol} — ${holding.asset.assetName}`);
    setText('assetLtp', fmtUSD(holding.curPrice));
    const deltaEl = document.getElementById('assetDelta');
    if (deltaEl) {
        deltaEl.innerText = `${deltaPct >= 0 ? '+' : ''}${deltaPct.toFixed(2)}%`;
        deltaEl.className = deltaPct >= 0 ? 'text-up' : 'text-down';
    }
    setText('assetInvested', fmtUSD(invested));
    setText('assetCurrent', fmtUSD(current));
    const pnlEl = document.getElementById('assetPnL');
    if (pnlEl) {
        pnlEl.innerText = `${pnlAbs >= 0 ? '+' : ''}${fmtUSD(pnlAbs)}`;
        pnlEl.className = pnlAbs >= 0 ? 'text-up' : 'text-down';
    }
    const pnlPctEl = document.getElementById('assetPnLPct');
    if (pnlPctEl) {
        pnlPctEl.innerText = `${pnlPct.toFixed(2)}%`;
        pnlPctEl.style.background = pnlAbs >= 0 ? 'rgba(16,185,129,0.2)' : 'rgba(239,68,68,0.2)';
        pnlPctEl.style.color = pnlAbs >= 0 ? '#10b981' : '#ef4444';
    }

    setText('assetQty', String(holding.quantity));
    setText('assetAvgPrice', fmtUSD(holding.avgBuyPrice));
    setText('assetCategory', holding.asset.category || '-');

    // Wire buttons
    const addBtn = document.getElementById('assetAddBtn');
    if (addBtn) {
        addBtn.onclick = () => {
            openTradeModal('BUY');
            const sel = document.getElementById('tradeAsset');
            if (sel) {
                // Match by data-symbol attribute populated during fetchAssets()
                for (const opt of sel.options) {
                    if (opt.getAttribute('data-symbol') === holding.asset.symbol) {
                        sel.value = opt.value; break;
                    }
                }
            }
        };
    }
    const exitBtn = document.getElementById('assetExitBtn');
    if (exitBtn) {
        exitBtn.onclick = () => sellAsset(holding.holdingId);
    }

    // Show modal first
    const modal = document.getElementById('assetModal');
    if (modal) modal.style.display = 'block';

    // Render charts
    await renderAssetDetailCharts(holding);
}

async function renderAssetDetailCharts(holding) {
    // Trend: try backend endpoint, fallback to synthetic
    let labels = [];
    let series = [];
    try {
        const res = await fetch(`${API_BASE}/market/prices?symbol=${encodeURIComponent(holding.asset.symbol)}&range=52w`);
        if (res.ok) {
            const data = await res.json();
            if (data && Array.isArray(data.labels) && Array.isArray(data.prices)) {
                labels = data.labels;
                series = data.prices;
            }
        }
    } catch(e) { /* ignore, fallback below */ }

    if (series.length === 0) {
        const s = generateSyntheticSeries(holding.avgBuyPrice || holding.curPrice || 100, 52);
        labels = s.labels;
        series = s.values;
    }

    const trendCtxEl = document.getElementById('assetTrendChart');
    if (trendCtxEl) {
        const ctx = trendCtxEl.getContext('2d');
        if (assetTrendInstance) assetTrendInstance.destroy();
        assetTrendInstance = new Chart(ctx, {
            type: 'line',
            data: { labels, datasets: [{
                label: 'Price',
                data: series,
                borderColor: '#3b82f6',
                fill: true,
                backgroundColor: 'rgba(59,130,246,0.08)',
                tension: 0.35
            }]},
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: { x: { grid: { display:false } }, y: { grid: { color:'#1e293b' } } }
            }
        });
    }

    // Breakdown doughnut: asset vs rest of portfolio
    const total = currentHoldings.reduce((acc, h) => acc + h.mktValue, 0);
    const me = holding.mktValue;
    const others = Math.max(total - me, 0);

    const shareCtxEl = document.getElementById('assetShareChart');
    if (shareCtxEl) {
        const ctx = shareCtxEl.getContext('2d');
        if (assetShareInstance) assetShareInstance.destroy();
        assetShareInstance = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['This Asset', 'Rest of Portfolio'],
                datasets: [{ data: [me, others], backgroundColor: ['#10b981', '#334155'], borderWidth: 0 }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8' } } }
            }
        });
    }
}

function generateSyntheticSeries(base, points) {
    const labels = [];
    const values = [];
    let price = base;
    for (let i = points - 1; i >= 0; i--) {
        const d = new Date();
        d.setDate(d.getDate() - i * 7); // weekly points for 52 weeks
        labels.push(d.toLocaleDateString());
        const move = (Math.random() * 0.06) - 0.03; // +/-3%
        price = Math.max(0.1, price * (1 + move));
        values.push(Number(price.toFixed(2)));
    }
    return { labels, values };
}

function setText(id, text) { const el = document.getElementById(id); if (el) el.innerText = text; }
function fmtUSD(n) { return (n||0).toLocaleString('en-US', { style:'currency', currency:'USD' }); }
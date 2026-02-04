const API_BASE = "http://localhost:8080/api";

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
let pnlDistributionChart = null;
let categoryPerformanceChart = null;
let winRateChart = null;
let profitByAssetChart = null;

document.addEventListener('DOMContentLoaded', async () => {
    updateTime();
    await fetchAssets();
    await fetchClients();
    renderWatchlist();
    startLiveEngine();

    const tm = document.getElementById('tradeModal'); if (tm) tm.style.display = 'none';
    const wm = document.getElementById('wlModal'); if (wm) wm.style.display = 'none';
    const am = document.getElementById('assetModal'); if (am) am.style.display = 'none';
    const sm = document.getElementById('sellModal'); if (sm) sm.style.display = 'none';
    
    renderHistoryTable();
    updateRealizedKPI();
});

function switchPage(pageId) {
    document.querySelectorAll('.page-view').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-menu li').forEach(l => l.classList.remove('active'));
    document.getElementById(pageId).classList.add('active');

    if (pageId === 'risk') calculateRiskProfile();
    if (pageId === 'holdings') renderHoldingsCharts();
    if (pageId === 'history') renderHistoryCharts();
}

async function fetchAssets() {
    try {
        const res = await fetch(`${API_BASE}/assets`);
        assetCatalog = await res.json();
        
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
        
        const sel = document.getElementById('globalClientSelect');
        sel.innerHTML = '<option value="">-- Select Account --</option>';
        
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
        renderHistoryCharts();

        if(document.getElementById('risk').classList.contains('active')) calculateRiskProfile();

    } catch(e) { console.error("Portfolio Error", e); }
}

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

function calculateRiskProfile() {
    const weights = { 'COMMODITY': 1.5, 'NSE': 1.0, 'MF': 0.7 };
    
    let totalValue = 0;
    let weightedBetaSum = 0;
    let categoryExposure = { 'COMMODITY': 0, 'NSE': 0, 'MF': 0 };
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
    
    let riskLabel = "MODERATE";
    let riskColor = "#f59e0b";
    if(portfolioBeta < 0.8) { riskLabel = "CONSERVATIVE"; riskColor = "#10b981"; }
    if(portfolioBeta > 1.2) { riskLabel = "AGGRESSIVE"; riskColor = "#ef4444"; }

    riskContributors.sort((a, b) => b.contrib - a.contrib);

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

function sellAsset(holdingId) { openSellModal(holdingId); }

function renderHistoryTable() {
    const tbody = document.getElementById('historyTable');
    tbody.innerHTML = '';
    
    if(historyLog.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:20px; color:#555">No realized trades yet.</td></tr>';
        renderCharts();
        renderHistoryCharts();
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
    
    renderHistoryCharts();
}

function renderHistoryCharts() {
    if (historyLog.length === 0) {
        if (winRateChart) { winRateChart.destroy(); winRateChart = null; }
        if (profitByAssetChart) { profitByAssetChart.destroy(); profitByAssetChart = null; }
        return;
    }
    
    const wins = historyLog.filter(tx => tx.profit > 0).length;
    const losses = historyLog.filter(tx => tx.profit < 0).length;
    const neutral = historyLog.filter(tx => tx.profit === 0).length;
    const winRate = historyLog.length > 0 ? ((wins / historyLog.length) * 100).toFixed(1) : 0;
    
    const winRateCtx = document.getElementById('winRateChart');
    if (winRateCtx) {
        if (winRateChart) winRateChart.destroy();
        winRateChart = new Chart(winRateCtx.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Wins', 'Losses', 'Neutral'],
                datasets: [{
                    data: [wins, losses, neutral],
                    backgroundColor: ['#10b981', '#ef4444', '#64748b']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { color: '#94a3b8' } },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const pct = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                                return `${label}: ${value} (${pct}%)`;
                            }
                        }
                    },
                    title: {
                        display: true,
                        text: `Win Rate: ${winRate}%`,
                        color: '#94a3b8',
                        font: { size: 14 }
                    }
                }
            }
        });
    }
    
    const assetProfitMap = {};
    historyLog.forEach(tx => {
        if (!assetProfitMap[tx.symbol]) {
            assetProfitMap[tx.symbol] = 0;
        }
        assetProfitMap[tx.symbol] += tx.profit;
    });
    
    const sortedAssets = Object.entries(assetProfitMap)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 10);
    
    const profitCtx = document.getElementById('profitByAssetChart');
    if (profitCtx && sortedAssets.length > 0) {
        if (profitByAssetChart) profitByAssetChart.destroy();
        profitByAssetChart = new Chart(profitCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: sortedAssets.map(a => a[0]),
                datasets: [{
                    label: 'Total Profit',
                    data: sortedAssets.map(a => a[1]),
                    backgroundColor: sortedAssets.map(a => a[1] >= 0 ? '#10b981' : '#ef4444')
                }]
            },
            options: {
                indexAxis: 'y',
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                return `Profit: $${context.parsed.x.toFixed(2)}`;
                            }
                        }
                    }
                },
                scales: {
                    x: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } },
                    y: { grid: { display: false }, ticks: { color: '#94a3b8' } }
                }
            }
        });
    }
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

function startLiveEngine() {
    if(liveInterval) clearInterval(liveInterval);
    setInterval(() => {
        watchlist.forEach(w => {
            const move = (Math.random() * 2) - 1;
            w.price = w.price * (1 + move/100);
            w.change = move;
        });
        saveWatchlist(); 
        renderWatchlist();

        const ticker = document.getElementById('tickerTape');
        let html = '';
        watchlist.forEach(w => {
            const c = w.change >= 0 ? 'text-up' : 'text-down';
            const s = w.change >= 0 ? '▲' : '▼';
            html += `<div class="ticker-item">${w.symbol}: <span class="${c}">${s} ${Math.abs(w.change).toFixed(2)}%</span></div>`;
        });
        ticker.innerHTML = html;

        updateTime();
    }, 2000);
}

function renderCharts() {
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
            plugins: { 
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        afterLabel: () => '💬 Click to explain this bar'
                    }
                }
            },
            scales: { 
                x: { grid: { display: false }, ticks: { color: '#94a3b8' } },
                y: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } }
            },
            onClick: (event, elements) => {
                if (elements.length > 0 && window.chatbot) {
                    const element = elements[0];
                    const label = monthlyChart.data.labels[element.index];
                    const value = monthlyChart.data.datasets[0].data[element.index];

                    window.chatbot.setVisualizationContext('bar', 'monthlyChart', {
                        xAxis: 'Month',
                        yAxis: 'Realized P&L',
                        hoverData: {
                            'Month': label,
                            'Realized P&L': '$' + value.toLocaleString()
                        }
                    });

                    const inputEl = document.getElementById('chatbotInput');
                    const triggerMessage = 'Explain this monthly performance chart';

                    if (window.chatbot.isOpen) {
                        if (inputEl) inputEl.value = triggerMessage;
                        window.chatbot.sendMessage();
                    } else {
                        window.chatbot.toggle();
                        setTimeout(() => {
                            const inputEl2 = document.getElementById('chatbotInput');
                            if (inputEl2) inputEl2.value = triggerMessage;
                            window.chatbot.sendMessage();
                        }, 300);
                    }
                }
            }
        }
    });

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
            plugins: { 
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        afterLabel: () => ' Click to explain this bar'
                    }
                }
            },
            scales: {
                x: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } },
                y: { grid: { display: false }, ticks: { color: '#94a3b8' } }
            },
            onClick: (event, elements) => {
                if (elements.length > 0 && window.chatbot) {
                    const element = elements[0];
                    const label = topHoldingsChart.data.labels[element.index];
                    const value = topHoldingsChart.data.datasets[0].data[element.index];

                    window.chatbot.setVisualizationContext('bar', 'topHoldingsChart', {
                        xAxis: 'Asset',
                        yAxis: 'Market Value',
                        hoverData: {
                            'Asset': label,
                            'Market Value': '$' + value.toLocaleString()
                        }
                    });

                    const inputEl = document.getElementById('chatbotInput');
                    const triggerMessage = 'Explain this top holdings chart';

                    if (window.chatbot.isOpen) {
                        if (inputEl) inputEl.value = triggerMessage;
                        window.chatbot.sendMessage();
                    } else {
                        window.chatbot.toggle();
                        setTimeout(() => {
                            const inputEl2 = document.getElementById('chatbotInput');
                            if (inputEl2) inputEl2.value = triggerMessage;
                            window.chatbot.sendMessage();
                        }, 300);
                    }
                }
            }
        }
    });
}

function updateTime() { document.getElementById('marketTime').innerText = new Date().toLocaleTimeString(); }

function renderHoldings() {
    const tbody = document.getElementById('holdingsTable');
    tbody.innerHTML = '';
    
    if (currentHoldings.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center; padding:20px; color:#555">No active holdings.</td></tr>';
        return;
    }
    
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
    
    renderHoldingsCharts();
}

function renderHoldingsCharts() {
    if (currentHoldings.length === 0) {
        if (pnlDistributionChart) { pnlDistributionChart.destroy(); pnlDistributionChart = null; }
        if (categoryPerformanceChart) { categoryPerformanceChart.destroy(); categoryPerformanceChart = null; }
        return;
    }
    
    const profitHoldings = currentHoldings.filter(h => h.pnl > 0);
    const lossHoldings = currentHoldings.filter(h => h.pnl < 0);
    const neutralHoldings = currentHoldings.filter(h => h.pnl === 0);
    
    const profitValue = profitHoldings.reduce((sum, h) => sum + h.pnl, 0);
    const lossValue = Math.abs(lossHoldings.reduce((sum, h) => sum + h.pnl, 0));
    
    const pnlCtx = document.getElementById('pnlDistributionChart');
    if (pnlCtx) {
        if (pnlDistributionChart) pnlDistributionChart.destroy();
        pnlDistributionChart = new Chart(pnlCtx.getContext('2d'), {
            type: 'doughnut',
            data: {
                labels: ['Profitable', 'Loss', 'Neutral'],
                datasets: [{
                    data: [profitHoldings.length, lossHoldings.length, neutralHoldings.length],
                    backgroundColor: ['#10b981', '#ef4444', '#64748b']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom', labels: { color: '#94a3b8' } },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const label = context.label || '';
                                const value = context.parsed || 0;
                                const total = context.dataset.data.reduce((a, b) => a + b, 0);
                                const pct = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
                                return `${label}: ${value} (${pct}%)`;
                            }
                        }
                    }
                }
            }
        });
    }
    
    const categoryMap = {};
    currentHoldings.forEach(h => {
        const cat = h.asset.category || 'UNKNOWN';
        if (!categoryMap[cat]) {
            categoryMap[cat] = { totalPnl: 0, count: 0, totalValue: 0 };
        }
        categoryMap[cat].totalPnl += h.pnl;
        categoryMap[cat].count += 1;
        categoryMap[cat].totalValue += h.mktValue;
    });
    
    const catCtx = document.getElementById('categoryPerformanceChart');
    if (catCtx) {
        if (categoryPerformanceChart) categoryPerformanceChart.destroy();
        const categories = Object.keys(categoryMap);
        categoryPerformanceChart = new Chart(catCtx.getContext('2d'), {
            type: 'bar',
            data: {
                labels: categories,
                datasets: [{
                    label: 'Total P&L',
                    data: categories.map(cat => categoryMap[cat].totalPnl),
                    backgroundColor: categories.map(cat => categoryMap[cat].totalPnl >= 0 ? '#10b981' : '#ef4444')
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const cat = categories[context.dataIndex];
                                const data = categoryMap[cat];
                                return [
                                    `P&L: $${data.totalPnl.toFixed(2)}`,
                                    `Holdings: ${data.count}`,
                                    `Value: $${data.totalValue.toFixed(2)}`
                                ];
                            }
                        }
                    }
                },
                scales: {
                    y: { grid: { color: '#1e293b' }, ticks: { color: '#94a3b8' } },
                    x: { grid: { display: false }, ticks: { color: '#94a3b8' } }
                }
            }
        });
    }
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
        const [res1, res2] = await Promise.all([
            fetch(`${API_BASE}/portfolio/${c1Id}`),
            fetch(`${API_BASE}/portfolio/${c2Id}`)
        ]);

        const raw1 = await res1.json();
        const raw2 = await res2.json();

        const p1 = processHoldingsData(raw1);
        const p2 = processHoldingsData(raw2);

        const getTotal = (p) => p.reduce((acc, h) => acc + h.mktValue, 0);
        const getAllocation = (p, cat) => p.filter(h => h.asset.category === cat).reduce((acc, h) => acc + h.mktValue, 0);

        const v1 = getTotal(p1);
        const v2 = getTotal(p2);

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
                        data: [{ x: beta1, y: (beta1 * 8) + (Math.random()*5), r: 15 }],
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

    const addBtn = document.getElementById('assetAddBtn');
    if (addBtn) {
        addBtn.onclick = () => {
            openTradeModal('BUY');
            const sel = document.getElementById('tradeAsset');
            if (sel) {
                // Match by data-symbol attribute populated during fetchAssets()
                for (const opt of sel.options) {
                    if (opt.getAttribute('data-symbol') === holding.asset.symbol) {
                        sel.value = opt.value;
                        break;
                    }
                }
            }
        };
    }
    const exitBtn = document.getElementById('assetExitBtn');
    if (exitBtn) {
        exitBtn.onclick = () => sellAsset(holding.holdingId);
    }

    const modal = document.getElementById('assetModal');
    if (modal) modal.style.display = 'block';

    await renderAssetDetailCharts(holding);
}

async function renderAssetDetailCharts(holding) {
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
    } catch(e) {}

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
        d.setDate(d.getDate() - i * 7);
        labels.push(d.toLocaleDateString());
        const move = (Math.random() * 0.06) - 0.03;
        price = Math.max(0.1, price * (1 + move));
        values.push(Number(price.toFixed(2)));
    }
    return { labels, values };
}

function setText(id, text) { const el = document.getElementById(id); if (el) el.innerText = text; }
function fmtUSD(n) { return (n||0).toLocaleString('en-US', { style:'currency', currency:'USD' }); }

function exportHoldingsCSV() {
    if (currentHoldings.length === 0) {
        alert('No holdings to export');
        return;
    }
    
    const headers = ['Symbol', 'Asset Name', 'Category', 'Quantity', 'Avg Buy Price', 'Market Price', 'Market Value', 'P&L ($)', 'P&L (%)'];
    const rows = currentHoldings.map(h => [
        h.asset.symbol || '',
        h.asset.assetName || '',
        h.asset.category || '',
        h.quantity || 0,
        (h.avgBuyPrice || 0).toFixed(2),
        (h.curPrice || 0).toFixed(2),
        (h.mktValue || 0).toFixed(2),
        (h.pnl || 0).toFixed(2),
        (h.pnlPct || 0).toFixed(2)
    ]);
    
    const csvContent = [
        headers.join(','),
        ...rows.map(row => row.join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `holdings_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

function exportHistoryCSV() {
    if (historyLog.length === 0) {
        alert('No trade history to export');
        return;
    }
    
    const headers = ['Date', 'Symbol', 'Type', 'Quantity', 'Buy Price', 'Sell Price', 'Realized P&L'];
    const rows = historyLog.map(tx => [
        tx.date || '',
        tx.symbol || '',
        tx.type || 'SELL',
        tx.qty || 0,
        (tx.buy || 0).toFixed(2),
        (tx.sell || 0).toFixed(2),
        (tx.profit || 0).toFixed(2)
    ]);
    
    const csvContent = [
        headers.join(','),
        ...rows.map(row => row.join(','))
    ].join('\n');
    
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', `trade_history_${new Date().toISOString().split('T')[0]}.csv`);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}
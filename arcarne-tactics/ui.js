/* ============================================
   ARCARNE TACTICS - UI CONTROLLER
   DOM manipulation, drag & drop, rendering
   ============================================ */

const game = new GameEngine();
let draggedUnit = null;
let dragSource = null;
let tooltip = null;

// ===================== INITIALIZATION =====================
document.addEventListener('DOMContentLoaded', () => {
    initBoard();
    initBench();
    bindEvents();
    bindGameEvents();
    game.init();
    updateAllUI();
});

// ===================== BOARD INITIALIZATION =====================
function initBoard() {
    const playerBoard = document.getElementById('player-board');
    const opponentBoard = document.getElementById('opponent-board');

    playerBoard.querySelectorAll('.board-cell').forEach(c => c.remove());
    opponentBoard.querySelectorAll('.board-cell').forEach(c => c.remove());

    for (let r = 0; r < 3; r++) {
        for (let c = 0; c < 7; c++) {
            const cell = createBoardCell(r, c, 'player');
            playerBoard.appendChild(cell);

            const oCell = createBoardCell(r, c, 'opponent');
            oCell.classList.add('opponent-cell');
            opponentBoard.appendChild(oCell);
        }
    }
}

function createBoardCell(row, col, owner) {
    const cell = document.createElement('div');
    cell.className = 'board-cell';
    cell.dataset.row = row;
    cell.dataset.col = col;
    cell.dataset.owner = owner;

    if (owner === 'player') {
        cell.dataset.zone = row === 0 ? 'front' : row === 2 ? 'back' : 'mid';
        cell.addEventListener('dragover', onDragOver);
        cell.addEventListener('drop', onDropBoard);
        cell.addEventListener('dragenter', onDragEnter);
        cell.addEventListener('dragleave', onDragLeave);
    }

    return cell;
}

function initBench() {
    const benchSlots = document.getElementById('bench-slots');
    benchSlots.innerHTML = '';
    for (let i = 0; i < 9; i++) {
        const slot = document.createElement('div');
        slot.className = 'bench-slot';
        slot.dataset.index = i;
        slot.addEventListener('dragover', onDragOver);
        slot.addEventListener('drop', onDropBench);
        slot.addEventListener('dragenter', onDragEnter);
        slot.addEventListener('dragleave', onDragLeave);
        benchSlots.appendChild(slot);
    }
}

// ===================== EVENT BINDING =====================
function bindEvents() {
    document.getElementById('btn-buy-xp').addEventListener('click', () => game.buyXP());
    document.getElementById('btn-refresh').addEventListener('click', () => game.refreshShop());

    document.getElementById('btn-start-battle').addEventListener('click', () => {
        if (game.state.phase === 'prep') game.startBattle();
    });

    document.getElementById('btn-continue').addEventListener('click', () => {
        document.getElementById('result-modal').classList.add('hidden');
        game.nextRound();
    });
}

function bindGameEvents() {
    game.on('shopChanged', renderShop);
    game.on('boardChanged', renderBoard);
    game.on('benchChanged', renderBench);
    game.on('synergiesChanged', renderSynergies);
    game.on('goldChanged', updatePlayerInfo);
    game.on('levelChanged', updatePlayerInfo);
    game.on('roundChanged', updatePlayerInfo);
    game.on('battleEnd', onBattleEnd);
    game.on('augmentOffer', showAugmentModal);
    game.on('unitUpgraded', onUnitUpgraded);
    game.on('itemDropped', onItemDropped);
    game.on('gameInit', () => updateAllUI());
}

// ===================== CARD RENDERING =====================
function createCardElement(unit, context) {
    const card = document.createElement('div');
    card.className = context === 'shop' ? 'shop-card' : 'card';
    card.dataset.rarity = unit.rarity;
    card.dataset.unitId = unit.id;
    card.draggable = context !== 'shop' && context !== 'opponent';

    // Full-bleed image background
    const img = document.createElement('div');
    img.className = 'card-image';
    img.style.background = unit.avatar;
    card.appendChild(img);

    // Gradient overlay for text readability
    const overlay = document.createElement('div');
    overlay.className = 'card-overlay';
    card.appendChild(overlay);

    // Cost badge
    const cost = document.createElement('div');
    cost.className = 'card-cost';
    cost.textContent = unit.cost;
    card.appendChild(cost);

    // Star level
    if (unit.starLevel > 1 || context !== 'shop') {
        const stars = document.createElement('div');
        stars.className = 'card-stars';
        for (let i = 0; i < (unit.starLevel || 1); i++) {
            const star = document.createElement('span');
            star.className = 'star';
            star.textContent = '\u2605';
            stars.appendChild(star);
        }
        card.appendChild(stars);
    }

    // Card info at bottom
    const info = document.createElement('div');
    info.className = 'card-info';

    const name = document.createElement('div');
    name.className = 'card-name';
    name.textContent = unit.name;
    info.appendChild(name);

    // Synergy badges
    const synergies = document.createElement('div');
    synergies.className = 'card-synergies';
    unit.synergies.forEach(synId => {
        const badge = document.createElement('span');
        badge.className = 'card-synergy-badge';
        const synData = SYNERGIES[synId];
        badge.textContent = synData ? synData.icon + ' ' + synData.name : synId;
        if (synData) badge.style.borderLeft = '2px solid ' + synData.color;
        synergies.appendChild(badge);
    });
    info.appendChild(synergies);

    // Shop-specific stats display
    if (context === 'shop') {
        const stats = document.createElement('div');
        stats.className = 'shop-stats';
        stats.innerHTML = '<span>\u2764\uFE0F ' + unit.stats.hp + '</span>' +
            '<span>\u2694\uFE0F ' + unit.stats.atk + '</span>' +
            '<span>\uD83D\uDEE1\uFE0F ' + unit.stats.def + '</span>';
        info.appendChild(stats);
    }

    card.appendChild(info);

    // HP/Mana bars for board units
    if (context === 'board' || context === 'opponent') {
        const bars = document.createElement('div');
        bars.className = 'card-bars';

        const hpBar = document.createElement('div');
        hpBar.className = 'card-hp-bar';
        const hpFill = document.createElement('div');
        hpFill.className = 'fill';
        hpFill.style.width = ((unit.currentHp / unit.stats.hp) * 100) + '%';
        hpBar.appendChild(hpFill);
        bars.appendChild(hpBar);

        const manaBar = document.createElement('div');
        manaBar.className = 'card-mana-bar';
        const manaFill = document.createElement('div');
        manaFill.className = 'fill';
        manaFill.style.width = ((unit.currentMana / unit.stats.mana) * 100) + '%';
        manaBar.appendChild(manaFill);
        bars.appendChild(manaBar);

        card.appendChild(bars);
    }

    // Drag events
    if (card.draggable) {
        card.addEventListener('dragstart', function(e) { onDragStart(e, unit, context); });
        card.addEventListener('dragend', onDragEnd);
    }

    // Click to buy from shop
    if (context === 'shop') {
        card.addEventListener('click', function() {
            if (game.buyHero(unit.shopIndex)) {
                card.classList.add('sold');
            }
        });
    }

    // Tooltip
    card.addEventListener('mouseenter', function(e) { showTooltip(e, unit); });
    card.addEventListener('mouseleave', hideTooltip);
    card.addEventListener('mousemove', moveTooltip);

    return card;
}

// ===================== RENDERING =====================
function renderShop() {
    var container = document.getElementById('shop-cards');
    container.innerHTML = '';
    game.state.shop.forEach(function(hero, idx) {
        if (hero.sold) {
            var placeholder = document.createElement('div');
            placeholder.className = 'shop-card sold';
            placeholder.innerHTML = '<div class="card-image" style="background:#1a2332"></div>';
            container.appendChild(placeholder);
        } else {
            var cardData = Object.assign({}, hero, { shopIndex: idx });
            var card = createCardElement(cardData, 'shop');
            container.appendChild(card);
        }
    });
}

function renderBoard() {
    var playerBoard = document.getElementById('player-board');
    var opponentBoard = document.getElementById('opponent-board');

    var playerCells = playerBoard.querySelectorAll('.board-cell[data-owner="player"]');
    playerCells.forEach(function(cell) {
        var r = parseInt(cell.dataset.row);
        var c = parseInt(cell.dataset.col);
        var unit = game.state.board[r][c];

        var existing = cell.querySelector('.card');
        if (existing) existing.remove();

        cell.classList.toggle('occupied', !!unit);
        if (unit) {
            var card = createCardElement(unit, 'board');
            cell.appendChild(card);
        }
    });

    var opponentCells = opponentBoard.querySelectorAll('.board-cell[data-owner="opponent"]');
    opponentCells.forEach(function(cell) {
        var r = parseInt(cell.dataset.row);
        var c = parseInt(cell.dataset.col);
        var unit = game.state.opponentBoard[r] ? game.state.opponentBoard[r][c] : null;

        var existing = cell.querySelector('.card');
        if (existing) existing.remove();

        cell.classList.toggle('occupied', !!unit);
        if (unit) {
            var card = createCardElement(unit, 'opponent');
            cell.appendChild(card);
        }
    });
}

function renderBench() {
    var slots = document.querySelectorAll('.bench-slot');
    slots.forEach(function(slot, idx) {
        var existing = slot.querySelector('.card');
        if (existing) existing.remove();

        var unit = game.state.bench[idx];
        if (unit) {
            var card = createCardElement(unit, 'bench');
            slot.appendChild(card);
        }
    });
}

function renderSynergies(synergies) {
    var container = document.getElementById('synergy-list');
    container.innerHTML = '';

    var entries = Object.entries(synergies || {});
    entries.sort(function(a, b) {
        var aActive = a[1].activeBonus ? 1 : 0;
        var bActive = b[1].activeBonus ? 1 : 0;
        return bActive - aActive || b[1].count - a[1].count;
    });

    entries.forEach(function(entry) {
        var synId = entry[0];
        var synData = entry[1];

        var item = document.createElement('div');
        item.className = 'synergy-item' + (synData.activeBonus ? ' active' : '');

        var icon = document.createElement('div');
        icon.className = 'synergy-icon';
        icon.style.background = synData.color + '33';
        icon.textContent = synData.icon;
        item.appendChild(icon);

        var info = document.createElement('div');
        info.style.flex = '1';

        var nameRow = document.createElement('div');
        nameRow.style.display = 'flex';
        nameRow.style.justifyContent = 'space-between';

        var nameEl = document.createElement('span');
        nameEl.className = 'synergy-name';
        nameEl.textContent = synData.name;
        nameRow.appendChild(nameEl);

        var count = document.createElement('span');
        count.className = 'synergy-count';
        var tierNums = synData.tiers.map(function(t) { return t.count; });
        count.textContent = synData.count + '/' + tierNums.join('/');
        nameRow.appendChild(count);

        info.appendChild(nameRow);

        if (synData.activeBonus) {
            var bonus = document.createElement('div');
            bonus.className = 'synergy-bonus';
            bonus.textContent = synData.activeBonus.bonus;
            info.appendChild(bonus);
        }

        item.appendChild(info);
        container.appendChild(item);
    });
}

function updatePlayerInfo() {
    var s = game.state.player;

    var hpFill = document.getElementById('player-hp-fill');
    hpFill.style.width = s.hp + '%';
    if (s.hp < 30) hpFill.style.background = 'linear-gradient(90deg, #ef4444, #f87171)';
    else if (s.hp < 60) hpFill.style.background = 'linear-gradient(90deg, #f59e0b, #fbbf24)';
    else hpFill.style.background = 'linear-gradient(90deg, #22c55e, #4ade80)';
    document.getElementById('player-hp-text').textContent = 'HP: ' + s.hp;

    var nextXp = LEVEL_XP[Math.min(s.level + 1, 8)];
    document.getElementById('player-level').textContent = 'Lv. ' + s.level +
        ' (' + s.xp + '/' + (s.level >= 8 ? 'MAX' : nextXp) + ' XP)';

    document.getElementById('gold-amount').textContent = s.gold;
    document.getElementById('round-info').textContent = 'Vong ' + game.state.round;
}

function updateAllUI() {
    updatePlayerInfo();
    renderShop();
    renderBoard();
    renderBench();
    renderSynergies(game.activeSynergies || {});
    renderItems();
}

function renderItems() {
    var container = document.getElementById('item-list');
    container.innerHTML = '';
    game.state.items.forEach(function(item, idx) {
        var slot = document.createElement('div');
        slot.className = 'item-slot';
        slot.textContent = item.icon;
        slot.title = item.name + ': +' + item.value + ' ' + item.stat;
        slot.draggable = true;
        slot.addEventListener('dragstart', function(e) {
            draggedUnit = { type: 'item', item: item, index: idx };
            e.dataTransfer.effectAllowed = 'move';
        });
        container.appendChild(slot);
    });
}

// ===================== DRAG & DROP =====================
function onDragStart(e, unit, source) {
    draggedUnit = { unit: unit, source: source };
    dragSource = e.target.closest('.board-cell, .bench-slot');
    e.target.classList.add('dragging');
    e.dataTransfer.effectAllowed = 'move';
}

function onDragEnd(e) {
    e.target.classList.remove('dragging');
    document.querySelectorAll('.drag-over, .highlight').forEach(function(el) {
        el.classList.remove('drag-over', 'highlight');
    });
    draggedUnit = null;
    dragSource = null;
}

function onDragOver(e) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
}

function onDragEnter(e) {
    e.preventDefault();
    e.currentTarget.classList.add('drag-over');
}

function onDragLeave(e) {
    e.currentTarget.classList.remove('drag-over');
}

function onDropBoard(e) {
    e.preventDefault();
    e.currentTarget.classList.remove('drag-over');
    if (!draggedUnit) return;

    var toRow = parseInt(e.currentTarget.dataset.row);
    var toCol = parseInt(e.currentTarget.dataset.col);

    if (draggedUnit.type === 'item') {
        var targetUnit = game.state.board[toRow][toCol];
        if (targetUnit && targetUnit.items.length < 3) {
            targetUnit.items.push(draggedUnit.item);
            var itm = draggedUnit.item;
            if (itm.stat === 'atk') targetUnit.stats.atk += itm.value;
            if (itm.stat === 'def') targetUnit.stats.def += itm.value;
            if (itm.stat === 'hp') { targetUnit.stats.hp += itm.value; targetUnit.currentHp += itm.value; }
            if (itm.stat === 'atkSpd') targetUnit.stats.atkSpd += itm.value;
            game.state.items.splice(draggedUnit.index, 1);
            renderItems();
            renderBoard();
        }
        return;
    }

    if (draggedUnit.source === 'bench') {
        var benchIdx = parseInt(dragSource.dataset.index);
        var unit = game.removeFromBench(benchIdx);
        if (unit) {
            if (!game.placeUnit(unit, toRow, toCol)) {
                game.addToBench(unit);
            }
        }
    } else if (draggedUnit.source === 'board') {
        var fromRow = parseInt(dragSource.dataset.row);
        var fromCol = parseInt(dragSource.dataset.col);
        game.moveUnit(fromRow, fromCol, toRow, toCol);
    }
}

function onDropBench(e) {
    e.preventDefault();
    e.currentTarget.classList.remove('drag-over');
    if (!draggedUnit || draggedUnit.type === 'item') return;

    var toBenchIdx = parseInt(e.currentTarget.dataset.index);

    if (draggedUnit.source === 'board') {
        var fromRow = parseInt(dragSource.dataset.row);
        var fromCol = parseInt(dragSource.dataset.col);
        var unit = game.removeUnitFromBoard(fromRow, fromCol);
        if (unit) {
            if (game.state.bench[toBenchIdx]) {
                var benchUnit = game.state.bench[toBenchIdx];
                game.state.bench[toBenchIdx] = unit;
                game.placeUnit(benchUnit, fromRow, fromCol);
            } else {
                game.state.bench[toBenchIdx] = unit;
            }
            game.emit('benchChanged');
        }
    } else if (draggedUnit.source === 'bench') {
        var fromIdx = parseInt(dragSource.dataset.index);
        var temp = game.state.bench[toBenchIdx];
        game.state.bench[toBenchIdx] = game.state.bench[fromIdx];
        game.state.bench[fromIdx] = temp;
        game.emit('benchChanged');
    }
}

// ===================== TOOLTIP =====================
function showTooltip(e, unit) {
    hideTooltip();
    tooltip = document.createElement('div');
    tooltip.className = 'card-tooltip fade-in';

    var synBadges = unit.synergies.map(function(synId) {
        var syn = SYNERGIES[synId];
        return '<span class="card-synergy-badge" style="border-left:2px solid ' + syn.color + '">' + syn.icon + ' ' + syn.name + '</span>';
    }).join(' ');

    var itemsHtml = '';
    if (unit.items && unit.items.length > 0) {
        itemsHtml = '<div style="margin-top:6px;padding-top:6px;border-top:1px solid #2d3748;font-size:11px;">' +
            'Trang bi: ' + unit.items.map(function(i) { return i.icon + ' ' + i.name; }).join(', ') + '</div>';
    }

    tooltip.innerHTML =
        '<div class="tt-name">' + unit.name + (unit.starLevel > 1 ? ' ' + '\u2605'.repeat(unit.starLevel) : '') + '</div>' +
        '<div class="tt-synergies">' + synBadges + '</div>' +
        '<div class="tt-stats">' +
            '<span>\u2764\uFE0F HP: ' + unit.stats.hp + '</span>' +
            '<span>\u2694\uFE0F ATK: ' + unit.stats.atk + '</span>' +
            '<span>\uD83D\uDEE1\uFE0F DEF: ' + unit.stats.def + '</span>' +
            '<span>\u26A1 SPD: ' + unit.stats.atkSpd + '</span>' +
            '<span>\uD83D\uDCCF Range: ' + unit.stats.range + '</span>' +
            '<span>\uD83D\uDC8E Mana: ' + unit.stats.mana + '</span>' +
        '</div>' +
        '<div class="tt-ability">' +
            '<div class="tt-ability-name">' + unit.ability.name + '</div>' +
            '<div class="tt-ability-desc">' + unit.ability.desc + '</div>' +
        '</div>' +
        itemsHtml;

    document.body.appendChild(tooltip);
    positionTooltip(e);
}

function moveTooltip(e) {
    if (tooltip) positionTooltip(e);
}

function positionTooltip(e) {
    var x = e.clientX + 16;
    var y = e.clientY - 10;
    tooltip.style.left = Math.min(x, window.innerWidth - 220) + 'px';
    tooltip.style.top = Math.min(y, window.innerHeight - 200) + 'px';
}

function hideTooltip() {
    if (tooltip) {
        tooltip.remove();
        tooltip = null;
    }
}

// ===================== BATTLE EVENTS =====================
function onBattleEnd(result) {
    var modal = document.getElementById('result-modal');
    var title = document.getElementById('result-title');
    var detail = document.getElementById('result-detail');

    if (result.playerWin) {
        title.textContent = '\uD83C\uDFC6 Chien Thang!';
        title.style.color = '#22c55e';
        detail.textContent = 'Con ' + result.playerAlive + ' don vi song sot. Chuoi thang: ' + game.state.player.streak;
    } else {
        title.textContent = '\uD83D\uDC80 That Bai!';
        title.style.color = '#ef4444';
        detail.textContent = 'Mat ' + result.damage + ' HP. Doi thu con ' + result.opponentAlive + ' don vi. HP con lai: ' + game.state.player.hp;
    }

    if (game.state.player.hp <= 0) {
        title.textContent = '\u2620\uFE0F Tro Choi Ket Thuc';
        title.style.color = '#ef4444';
        detail.textContent = 'Ban bi loai o vong ' + game.state.round + '. Hay thu lai!';
        document.getElementById('btn-continue').textContent = 'Choi lai';
        document.getElementById('btn-continue').onclick = function() { location.reload(); };
    }

    modal.classList.remove('hidden');
    renderBattleLog(result.log);
}

function renderBattleLog(log) {
    var panel = document.getElementById('info-panel');
    var logContainer = document.getElementById('battle-log');
    logContainer.innerHTML = '';
    panel.classList.add('visible');

    var importantEvents = log.filter(function(e) {
        return e.type === 'ability' || e.type === 'death';
    }).slice(-15);

    importantEvents.forEach(function(entry) {
        var div = document.createElement('div');
        div.className = 'log-entry';
        if (entry.type === 'ability') {
            div.className += ' log-ability';
            div.textContent = '\u2728 ' + entry.caster + ' dung ' + entry.abilityName +
                (entry.damage ? ' (' + entry.damage + ' dmg)' : '');
        } else if (entry.type === 'death') {
            div.className += ' log-death';
            div.textContent = '\uD83D\uDC80 ' + entry.unit + ' bi ha guc';
        }
        logContainer.appendChild(div);
    });

    setTimeout(function() { panel.classList.remove('visible'); }, 10000);
}

// ===================== AUGMENT MODAL =====================
function showAugmentModal(choices) {
    var modal = document.getElementById('augment-modal');
    var container = document.getElementById('augment-choices');
    container.innerHTML = '';

    choices.forEach(function(aug) {
        var card = document.createElement('div');
        card.className = 'augment-card';
        var tierColor = aug.tier === 'prismatic' ? '#f59e0b' : aug.tier === 'gold' ? '#eab308' : '#94a3b8';

        card.innerHTML =
            '<div class="augment-icon">' + aug.icon + '</div>' +
            '<div class="augment-name" style="color:' + tierColor + '">' + aug.name + '</div>' +
            '<div class="augment-desc">' + aug.desc + '</div>';

        card.addEventListener('click', function() {
            game.selectAugment(aug);
            modal.classList.add('hidden');
        });

        container.appendChild(card);
    });

    modal.classList.remove('hidden');
}

// ===================== UPGRADE / ITEM EFFECTS =====================
function onUnitUpgraded(unit) {
    var cards = document.querySelectorAll('[data-unit-id="' + unit.id + '"]');
    cards.forEach(function(card) { card.classList.add('upgrading'); });
    setTimeout(function() {
        renderBoard();
        renderBench();
    }, 600);
}

function onItemDropped(item) {
    renderItems();
    var log = document.getElementById('battle-log');
    log.innerHTML += '<div class="log-entry" style="color:#f59e0b">\uD83C\uDF81 Nhan duoc: ' + item.icon + ' ' + item.name + '</div>';
    document.getElementById('info-panel').classList.add('visible');
    setTimeout(function() { document.getElementById('info-panel').classList.remove('visible'); }, 3000);
}

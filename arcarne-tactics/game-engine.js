/* ============================================
   ARCARNE TACTICS - GAME ENGINE
   Core game logic: state, economy, battle, synergies
   ============================================ */

class GameEngine {
    constructor() {
        this.state = {
            round: 1,
            phase: 'prep', // 'prep' | 'battle' | 'result'
            player: {
                hp: 100,
                gold: 5,
                level: 1,
                xp: 0,
                streak: 0, // win/loss streak
                augments: []
            },
            board: this.createEmptyBoard(7, 3),     // 7 cols x 3 rows
            opponentBoard: this.createEmptyBoard(7, 3),
            bench: new Array(9).fill(null),
            shop: [],
            items: [],
            battleLog: []
        };
        this.listeners = {};
    }

    // ===================== EVENT SYSTEM =====================
    on(event, callback) {
        if (!this.listeners[event]) this.listeners[event] = [];
        this.listeners[event].push(callback);
    }

    emit(event, data) {
        if (this.listeners[event]) {
            this.listeners[event].forEach(cb => cb(data));
        }
    }

    // ===================== BOARD MANAGEMENT =====================
    createEmptyBoard(cols, rows) {
        const board = [];
        for (let r = 0; r < rows; r++) {
            board[r] = new Array(cols).fill(null);
        }
        return board;
    }

    getUnitsOnBoard() {
        const units = [];
        for (let r = 0; r < 3; r++) {
            for (let c = 0; c < 7; c++) {
                if (this.state.board[r][c]) {
                    units.push({ ...this.state.board[r][c], row: r, col: c });
                }
            }
        }
        return units;
    }

    getUnitCount() {
        return this.getUnitsOnBoard().length;
    }

    canPlaceUnit() {
        return this.getUnitCount() < MAX_UNITS[this.state.player.level];
    }

    placeUnit(unit, row, col) {
        if (this.state.board[row][col]) return false;
        if (!this.canPlaceUnit()) return false;
        this.state.board[row][col] = unit;
        this.checkUpgrade(unit.heroId);
        this.updateSynergies();
        this.emit('boardChanged');
        return true;
    }

    removeUnitFromBoard(row, col) {
        const unit = this.state.board[row][col];
        this.state.board[row][col] = null;
        this.updateSynergies();
        this.emit('boardChanged');
        return unit;
    }

    moveUnit(fromRow, fromCol, toRow, toCol) {
        if (this.state.board[toRow][toCol]) {
            // Swap
            const temp = this.state.board[toRow][toCol];
            this.state.board[toRow][toCol] = this.state.board[fromRow][fromCol];
            this.state.board[fromRow][fromCol] = temp;
        } else {
            this.state.board[toRow][toCol] = this.state.board[fromRow][fromCol];
            this.state.board[fromRow][fromCol] = null;
        }
        this.emit('boardChanged');
    }

    addToBench(unit) {
        const emptySlot = this.state.bench.findIndex(s => s === null);
        if (emptySlot === -1) return false;
        this.state.bench[emptySlot] = unit;
        this.checkUpgrade(unit.heroId);
        this.emit('benchChanged');
        return true;
    }

    removeFromBench(index) {
        const unit = this.state.bench[index];
        this.state.bench[index] = null;
        this.emit('benchChanged');
        return unit;
    }

    // ===================== UNIT CREATION =====================
    createUnit(heroData) {
        return {
            id: `${heroData.id}_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
            heroId: heroData.id,
            name: heroData.name,
            cost: heroData.cost,
            rarity: heroData.rarity,
            synergies: [...heroData.synergies],
            stats: { ...heroData.stats },
            ability: { ...heroData.ability },
            avatar: heroData.avatar,
            starLevel: 1,
            items: [],
            currentHp: heroData.stats.hp,
            currentMana: 0
        };
    }

    // ===================== UPGRADE SYSTEM =====================
    // 3 cùng loại cùng star -> nâng cấp
    checkUpgrade(heroId) {
        const allUnits = [];
        // Collect from board
        for (let r = 0; r < 3; r++) {
            for (let c = 0; c < 7; c++) {
                if (this.state.board[r][c] && this.state.board[r][c].heroId === heroId) {
                    allUnits.push({ unit: this.state.board[r][c], location: 'board', row: r, col: c });
                }
            }
        }
        // Collect from bench
        this.state.bench.forEach((unit, idx) => {
            if (unit && unit.heroId === heroId) {
                allUnits.push({ unit, location: 'bench', index: idx });
            }
        });

        // Group by star level
        const byStarLevel = {};
        allUnits.forEach(entry => {
            const sl = entry.unit.starLevel;
            if (!byStarLevel[sl]) byStarLevel[sl] = [];
            byStarLevel[sl].push(entry);
        });

        for (const starLevel in byStarLevel) {
            const group = byStarLevel[starLevel];
            if (group.length >= 3) {
                // Upgrade! Keep the first, remove the other 2
                const keep = group[0];
                const toRemove = group.slice(1, 3);

                // Remove units
                toRemove.forEach(entry => {
                    if (entry.location === 'board') {
                        this.state.board[entry.row][entry.col] = null;
                    } else {
                        this.state.bench[entry.index] = null;
                    }
                });

                // Upgrade kept unit
                keep.unit.starLevel = parseInt(starLevel) + 1;
                const baseHero = HEROES.find(h => h.id === heroId);
                const mult = keep.unit.starLevel === 2 ? 1.8 : 3.0;
                keep.unit.stats.hp = Math.round(baseHero.stats.hp * mult);
                keep.unit.stats.atk = Math.round(baseHero.stats.atk * mult);
                keep.unit.currentHp = keep.unit.stats.hp;

                // Transfer items from removed units
                toRemove.forEach(entry => {
                    entry.unit.items.forEach(item => {
                        if (keep.unit.items.length < 3) {
                            keep.unit.items.push(item);
                        } else {
                            this.state.items.push(item);
                        }
                    });
                });

                this.emit('unitUpgraded', keep.unit);
                // Recursive check for 3-star
                this.checkUpgrade(heroId);
                return;
            }
        }
    }

    // ===================== SHOP SYSTEM =====================
    generateShop() {
        const level = this.state.player.level;
        const odds = SHOP_ODDS[level];
        const shopSize = this.getAugmentEffect('shopSize') || 5;
        const shop = [];

        for (let i = 0; i < shopSize; i++) {
            const roll = Math.random();
            let rarity;
            let cumulative = 0;
            for (const [r, chance] of Object.entries(odds)) {
                cumulative += chance;
                if (roll <= cumulative) { rarity = r; break; }
            }
            rarity = rarity || 'common';

            const pool = HEROES.filter(h => h.rarity === rarity);
            const hero = pool[Math.floor(Math.random() * pool.length)];
            shop.push({ ...hero, shopIndex: i, sold: false });
        }

        this.state.shop = shop;
        this.emit('shopChanged');
    }

    buyHero(shopIndex) {
        const hero = this.state.shop[shopIndex];
        if (!hero || hero.sold) return false;
        if (this.state.player.gold < hero.cost) return false;

        const unit = this.createUnit(hero);
        const added = this.addToBench(unit);
        if (!added) return false;

        this.state.player.gold -= hero.cost;
        this.state.shop[shopIndex].sold = true;
        this.emit('goldChanged');
        this.emit('shopChanged');
        return true;
    }

    refreshShop() {
        if (this.state.player.gold < 2) return false;
        this.state.player.gold -= 2;
        this.generateShop();
        this.emit('goldChanged');
        return true;
    }

    // ===================== ECONOMY =====================
    buyXP() {
        if (this.state.player.gold < 4) return false;
        if (this.state.player.level >= 8) return false;
        this.state.player.gold -= 4;
        this.state.player.xp += 4;
        this.checkLevelUp();
        this.emit('goldChanged');
        this.emit('levelChanged');
        return true;
    }

    checkLevelUp() {
        while (this.state.player.level < 8 && this.state.player.xp >= LEVEL_XP[this.state.player.level + 1]) {
            this.state.player.xp -= LEVEL_XP[this.state.player.level + 1];
            this.state.player.level++;
            this.emit('levelUp', this.state.player.level);
        }
    }

    calculateIncome() {
        const base = Math.min(5, this.state.round);
        const interest = this.getAugmentEffect('noInterest') ? 0 : Math.min(5, Math.floor(this.state.player.gold / 10));
        const streak = Math.min(3, Math.abs(this.state.player.streak));
        const flatBonus = this.getAugmentEffect('flatGold') || 0;
        return base + interest + streak + flatBonus;
    }

    // ===================== SYNERGY SYSTEM =====================
    updateSynergies() {
        const units = this.getUnitsOnBoard();
        const synergyCount = {};

        units.forEach(unit => {
            unit.synergies.forEach(syn => {
                if (!synergyCount[syn]) synergyCount[syn] = 0;
                synergyCount[syn]++;
            });
        });

        // Apply augment bonus
        const bonusCount = this.getAugmentEffect('synergyBonus') || 0;
        if (bonusCount > 0) {
            for (const syn in synergyCount) {
                synergyCount[syn] += bonusCount;
            }
        }

        this.activeSynergies = {};
        for (const [synId, count] of Object.entries(synergyCount)) {
            const synData = SYNERGIES[synId];
            if (!synData) continue;

            let activeBonus = null;
            for (let i = synData.tiers.length - 1; i >= 0; i--) {
                if (count >= synData.tiers[i].count) {
                    activeBonus = synData.tiers[i];
                    break;
                }
            }

            this.activeSynergies[synId] = {
                ...synData,
                count,
                activeBonus,
                tiers: synData.tiers
            };
        }

        this.emit('synergiesChanged', this.activeSynergies);
    }

    getAugmentEffect(effectKey) {
        for (const aug of this.state.player.augments) {
            if (aug.effect[effectKey] !== undefined) return aug.effect[effectKey];
        }
        return null;
    }

    // ===================== BATTLE SYSTEM =====================
    startBattle() {
        if (this.state.phase !== 'prep') return;
        this.state.phase = 'battle';
        this.state.battleLog = [];

        // Generate opponent
        this.generateOpponent();

        // Clone units for battle (don't modify originals)
        const playerUnits = this.getUnitsOnBoard().map(u => this.createBattleUnit(u, 'player'));
        const opponentUnits = this.getOpponentUnits().map(u => this.createBattleUnit(u, 'opponent'));

        // Apply synergy bonuses
        this.applySynergyBonuses(playerUnits);
        this.applyAugmentBonuses(playerUnits);

        // Run battle simulation
        this.simulateBattle(playerUnits, opponentUnits);
    }

    createBattleUnit(unit, team) {
        return {
            ...unit,
            team,
            maxHp: unit.stats.hp,
            currentHp: unit.stats.hp,
            currentMana: 0,
            alive: true,
            effects: []
        };
    }

    applySynergyBonuses(units) {
        for (const [synId, synData] of Object.entries(this.activeSynergies || {})) {
            if (!synData.activeBonus) continue;
            const effect = synData.activeBonus.effect;

            units.forEach(u => {
                const hasSynergy = u.synergies.includes(synId);

                // Armor bonuses
                if (effect.armorBonus && hasSynergy) u.stats.def += effect.armorBonus;
                if (effect.armorBonusAll) u.stats.def += effect.armorBonusAll;

                // HP bonuses
                if (effect.hpBonus && hasSynergy) {
                    u.stats.hp += effect.hpBonus;
                    u.maxHp += effect.hpBonus;
                    u.currentHp += effect.hpBonus;
                }

                // ATK bonuses
                if (effect.atkDmgBonus) u.stats.atk = Math.round(u.stats.atk * (1 + effect.atkDmgBonus));

                // Spell damage
                if (effect.spellDmgMult && hasSynergy) u.spellDmgMult = (u.spellDmgMult || 1) * effect.spellDmgMult;
            });
        }
    }

    applyAugmentBonuses(units) {
        this.state.player.augments.forEach(aug => {
            const e = aug.effect;
            units.forEach(u => {
                if (e.teamArmor) u.stats.def += e.teamArmor;
                if (e.teamRegen) u.regenPct = (u.regenPct || 0) + e.teamRegen;

                // Positioning augments
                if (e.frontRowAtk && u.row === 0) {
                    u.stats.atk = Math.round(u.stats.atk * (1 + e.frontRowAtk));
                    u.stats.hp += e.frontRowHp || 0;
                    u.maxHp += e.frontRowHp || 0;
                    u.currentHp += e.frontRowHp || 0;
                }
                if (e.backRowAtkSpd && u.row === 2) {
                    u.stats.atkSpd *= (1 + e.backRowAtkSpd);
                }

                // Adjacency bonus
                if (e.adjacencyAtk) {
                    const adjacentCount = this.countAdjacentUnits(u.row, u.col);
                    u.stats.atk = Math.round(u.stats.atk * (1 + e.adjacencyAtk * adjacentCount));
                }
            });
        });
    }

    countAdjacentUnits(row, col) {
        let count = 0;
        const dirs = [[-1,-1],[-1,0],[-1,1],[0,-1],[0,1],[1,-1],[1,0],[1,1]];
        dirs.forEach(([dr, dc]) => {
            const nr = row + dr;
            const nc = col + dc;
            if (nr >= 0 && nr < 3 && nc >= 0 && nc < 7 && this.state.board[nr][nc]) {
                count++;
            }
        });
        return count;
    }

    simulateBattle(playerUnits, opponentUnits) {
        let tick = 0;
        const maxTicks = 300; // 30 seconds at 10 ticks/sec
        const tickResults = [];

        while (tick < maxTicks) {
            tick++;

            // Process each alive unit
            const allUnits = [...playerUnits, ...opponentUnits].filter(u => u.alive);
            if (playerUnits.filter(u => u.alive).length === 0 || opponentUnits.filter(u => u.alive).length === 0) break;

            allUnits.forEach(unit => {
                if (!unit.alive) return;
                const enemies = (unit.team === 'player' ? opponentUnits : playerUnits).filter(u => u.alive);
                if (enemies.length === 0) return;

                // Regen
                if (unit.regenPct) {
                    unit.currentHp = Math.min(unit.maxHp, unit.currentHp + unit.maxHp * unit.regenPct / 10);
                }

                // Find nearest target
                const target = this.findNearestTarget(unit, enemies);
                if (!target) return;

                // Check if in range
                const dist = Math.abs(unit.row - target.row) + Math.abs(unit.col - target.col);
                if (dist <= unit.stats.range) {
                    // Attack
                    if (tick % Math.round(10 / unit.stats.atkSpd) === 0) {
                        const dmg = this.calculateDamage(unit, target);
                        target.currentHp -= dmg;
                        unit.currentMana = Math.min(unit.stats.mana, unit.currentMana + 10);

                        tickResults.push({
                            tick,
                            type: 'attack',
                            attacker: unit.name,
                            target: target.name,
                            damage: dmg,
                            targetHp: target.currentHp
                        });

                        // Lifesteal
                        if (unit.lifesteal) {
                            unit.currentHp = Math.min(unit.maxHp, unit.currentHp + dmg * unit.lifesteal);
                        }

                        if (target.currentHp <= 0) {
                            target.alive = false;
                            tickResults.push({ tick, type: 'death', unit: target.name, team: target.team });
                        }
                    }

                    // Cast ability when mana full
                    if (unit.currentMana >= unit.stats.mana) {
                        const abilityResult = this.castAbility(unit, enemies, unit.team === 'player' ? playerUnits : opponentUnits);
                        unit.currentMana = 0;
                        tickResults.push({
                            tick,
                            type: 'ability',
                            caster: unit.name,
                            abilityName: unit.ability.name,
                            ...abilityResult
                        });
                    }
                } else {
                    // Move toward target
                    this.moveToward(unit, target);
                }
            });
        }

        // Determine winner
        const playerAlive = playerUnits.filter(u => u.alive).length;
        const opponentAlive = opponentUnits.filter(u => u.alive).length;
        const playerWin = playerAlive > opponentAlive;

        // Calculate damage to HP
        if (!playerWin) {
            const damage = opponentAlive * 2 + this.state.round;
            this.state.player.hp = Math.max(0, this.state.player.hp - damage);
            this.state.player.streak = Math.min(this.state.player.streak - 1, -1);
        } else {
            this.state.player.streak = Math.max(this.state.player.streak + 1, 1);
        }

        this.state.battleLog = tickResults;
        this.state.phase = 'result';

        this.emit('battleEnd', {
            playerWin,
            playerAlive,
            opponentAlive,
            damage: playerWin ? 0 : opponentAlive * 2 + this.state.round,
            log: tickResults
        });
    }

    findNearestTarget(unit, enemies) {
        let nearest = null;
        let minDist = Infinity;
        enemies.forEach(enemy => {
            const dist = Math.abs(unit.row - enemy.row) + Math.abs(unit.col - enemy.col);
            if (dist < minDist) {
                minDist = dist;
                nearest = enemy;
            }
        });
        return nearest;
    }

    calculateDamage(attacker, defender) {
        const baseDmg = attacker.stats.atk;
        const armorReduction = defender.stats.def / (defender.stats.def + 100);
        const dmg = Math.max(1, Math.round(baseDmg * (1 - armorReduction)));

        // Crit check
        const critChance = attacker.critBonus || 0.1;
        if (Math.random() < critChance) {
            const critDmg = attacker.critDmg || 1.5;
            return Math.round(dmg * critDmg);
        }

        return dmg;
    }

    castAbility(caster, enemies, allies) {
        const ability = caster.ability;
        const result = { damage: 0, healed: 0, targets: [] };

        if (ability.damage > 0) {
            // Damage ability - hit nearest enemies
            const targets = enemies.slice(0, 3);
            const spellMult = caster.spellDmgMult || 1;
            targets.forEach(target => {
                const dmg = Math.round(ability.damage * spellMult);
                target.currentHp -= dmg;
                result.damage += dmg;
                result.targets.push(target.name);
                if (target.currentHp <= 0) target.alive = false;
            });
        } else {
            // Heal/buff ability
            const weakest = [...allies].filter(u => u.alive).sort((a, b) => a.currentHp / a.maxHp - b.currentHp / b.maxHp);
            if (weakest.length > 0) {
                const healAmt = Math.round(weakest[0].maxHp * 0.3);
                weakest[0].currentHp = Math.min(weakest[0].maxHp, weakest[0].currentHp + healAmt);
                result.healed = healAmt;
                result.targets.push(weakest[0].name);
            }
        }

        return result;
    }

    moveToward(unit, target) {
        const dr = target.row > unit.row ? 1 : target.row < unit.row ? -1 : 0;
        const dc = target.col > unit.col ? 1 : target.col < unit.col ? -1 : 0;
        unit.row += dr;
        unit.col += dc;
    }

    // ===================== OPPONENT GENERATION =====================
    generateOpponent() {
        const boardSize = Math.min(this.state.round + 1, 8);
        const board = this.createEmptyBoard(7, 3);

        for (let i = 0; i < boardSize; i++) {
            const level = this.state.player.level;
            const odds = SHOP_ODDS[Math.min(level, 8)];
            const roll = Math.random();
            let rarity = 'common';
            let cumulative = 0;
            for (const [r, chance] of Object.entries(odds)) {
                cumulative += chance;
                if (roll <= cumulative) { rarity = r; break; }
            }

            const pool = HEROES.filter(h => h.rarity === rarity);
            const hero = pool[Math.floor(Math.random() * pool.length)];
            const unit = this.createUnit(hero);

            // Random star upgrades for higher rounds
            if (this.state.round > 8 && Math.random() < 0.3) unit.starLevel = 2;
            if (this.state.round > 15 && Math.random() < 0.15) unit.starLevel = 3;
            if (unit.starLevel > 1) {
                const mult = unit.starLevel === 2 ? 1.8 : 3.0;
                unit.stats.hp = Math.round(hero.stats.hp * mult);
                unit.stats.atk = Math.round(hero.stats.atk * mult);
            }

            // Place on board
            let placed = false;
            let attempts = 0;
            while (!placed && attempts < 50) {
                const r = Math.floor(Math.random() * 3);
                const c = Math.floor(Math.random() * 7);
                if (!board[r][c]) {
                    board[r][c] = unit;
                    placed = true;
                }
                attempts++;
            }
        }

        this.state.opponentBoard = board;
    }

    getOpponentUnits() {
        const units = [];
        for (let r = 0; r < 3; r++) {
            for (let c = 0; c < 7; c++) {
                if (this.state.opponentBoard[r][c]) {
                    // Mirror opponent positions
                    units.push({ ...this.state.opponentBoard[r][c], row: 2 - r, col: 6 - c });
                }
            }
        }
        return units;
    }

    // ===================== ROUND MANAGEMENT =====================
    nextRound() {
        this.state.round++;
        this.state.phase = 'prep';

        // Income
        const income = this.calculateIncome();
        this.state.player.gold += income;

        // Free XP
        this.state.player.xp += 2;
        this.checkLevelUp();

        // New shop
        this.generateShop();

        // Item drop on certain rounds
        if (this.state.round % 3 === 0) {
            this.dropItem();
        }

        // Augment selection on round 3, 10, 17
        if ([3, 10, 17].includes(this.state.round)) {
            this.offerAugments();
        }

        // Restore unit HP
        for (let r = 0; r < 3; r++) {
            for (let c = 0; c < 7; c++) {
                if (this.state.board[r][c]) {
                    this.state.board[r][c].currentHp = this.state.board[r][c].stats.hp;
                    this.state.board[r][c].currentMana = 0;
                }
            }
        }

        this.emit('roundChanged');
        this.emit('goldChanged');
        this.emit('levelChanged');
        this.emit('boardChanged');
    }

    dropItem() {
        const item = BASE_ITEMS[Math.floor(Math.random() * BASE_ITEMS.length)];
        this.state.items.push({ ...item });
        this.emit('itemDropped', item);
    }

    offerAugments() {
        const available = AUGMENTS.filter(a => !this.state.player.augments.find(pa => pa.id === a.id));
        const choices = [];
        for (let i = 0; i < 3 && available.length > 0; i++) {
            const idx = Math.floor(Math.random() * available.length);
            choices.push(available.splice(idx, 1)[0]);
        }
        this.emit('augmentOffer', choices);
    }

    selectAugment(augment) {
        this.state.player.augments.push(augment);
        this.emit('augmentSelected', augment);
    }

    // ===================== GAME INITIALIZATION =====================
    init() {
        this.generateShop();
        this.updateSynergies();
        this.emit('gameInit');
    }
}

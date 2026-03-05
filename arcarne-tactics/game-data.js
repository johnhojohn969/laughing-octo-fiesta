/* ============================================
   ARCARNE TACTICS - GAME DATA
   Heroes, Synergies, Items, Augments
   ============================================ */

// ===================== SYNERGIES =====================
// Hệ thống hiệp lực - dễ hiểu nhưng tạo nhiều tổ hợp chiến thuật
const SYNERGIES = {
    // === ORIGINS (Nguồn gốc) ===
    dragon: {
        name: 'Rồng',
        icon: '🐉',
        color: '#ea580c',
        tiers: [
            { count: 2, bonus: 'Miễn nhiễm phép thuật 3 giây đầu', effect: { magicImmuneDuration: 3 } },
            { count: 4, bonus: '+50% sát thương phép', effect: { spellDmgMult: 1.5 } }
        ]
    },
    undead: {
        name: 'Bất Tử',
        icon: '💀',
        color: '#6b7280',
        tiers: [
            { count: 2, bonus: 'Giảm 20% giáp đối thủ', effect: { armorReduction: 0.2 } },
            { count: 4, bonus: 'Giảm 50% giáp đối thủ', effect: { armorReduction: 0.5 } }
        ]
    },
    elemental: {
        name: 'Nguyên Tố',
        icon: '🌀',
        color: '#2563eb',
        tiers: [
            { count: 2, bonus: 'Triệu hồi Golem đá khi bắt đầu', effect: { summonGolem: true } },
            { count: 4, bonus: 'Tất cả đồng minh +30% kháng phép', effect: { magicResist: 0.3 } }
        ]
    },
    beast: {
        name: 'Dã Thú',
        icon: '🐺',
        color: '#65a30d',
        tiers: [
            { count: 2, bonus: '+15% sát thương tấn công', effect: { atkDmgBonus: 0.15 } },
            { count: 4, bonus: '+30% sát thương + hút máu 15%', effect: { atkDmgBonus: 0.3, lifesteal: 0.15 } }
        ]
    },
    spirit: {
        name: 'Linh Hồn',
        icon: '👻',
        color: '#a78bfa',
        tiers: [
            { count: 2, bonus: '+25% tốc độ tấn công sau khi dùng kỹ năng', effect: { atkSpdAfterCast: 0.25 } },
            { count: 3, bonus: '+50% tốc độ tấn công toàn đội sau khi dùng kỹ năng', effect: { atkSpdAfterCastAll: 0.5 } }
        ]
    },

    // === CLASSES (Lớp nhân vật) ===
    warrior: {
        name: 'Chiến Binh',
        icon: '⚔️',
        color: '#dc2626',
        tiers: [
            { count: 3, bonus: '+8 giáp cho Chiến Binh', effect: { armorBonus: 8 } },
            { count: 6, bonus: '+15 giáp cho toàn đội', effect: { armorBonusAll: 15 } }
        ]
    },
    mage: {
        name: 'Pháp Sư',
        icon: '🔮',
        color: '#7c3aed',
        tiers: [
            { count: 3, bonus: 'Giảm 40% kháng phép đối thủ', effect: { magicResReduction: 0.4 } },
            { count: 6, bonus: 'Giảm 100% kháng phép đối thủ', effect: { magicResReduction: 1.0 } }
        ]
    },
    ranger: {
        name: 'Xạ Thủ',
        icon: '🏹',
        color: '#16a34a',
        tiers: [
            { count: 2, bonus: '30% cơ hội tăng gấp đôi tốc độ bắn', effect: { doubleAtkChance: 0.3 } },
            { count: 4, bonus: '65% cơ hội tăng gấp đôi tốc độ bắn', effect: { doubleAtkChance: 0.65 } }
        ]
    },
    assassin: {
        name: 'Sát Thủ',
        icon: '🗡️',
        color: '#be185d',
        tiers: [
            { count: 3, bonus: 'Nhảy ra sau lưng đối thủ + 15% chí mạng', effect: { jumpBackline: true, critBonus: 0.15 } },
            { count: 6, bonus: 'Nhảy + 35% chí mạng + 2x sát thương chí mạng', effect: { jumpBackline: true, critBonus: 0.35, critDmg: 2 } }
        ]
    },
    tank: {
        name: 'Giáp Sĩ',
        icon: '🛡️',
        color: '#d97706',
        tiers: [
            { count: 2, bonus: '+500 HP cho Giáp Sĩ', effect: { hpBonus: 500 } },
            { count: 4, bonus: '+1000 HP + phản đòn 15% sát thương', effect: { hpBonus: 1000, thorns: 0.15 } }
        ]
    },
    support: {
        name: 'Hỗ Trợ',
        icon: '💚',
        color: '#0891b2',
        tiers: [
            { count: 2, bonus: '+15% hồi mana toàn đội', effect: { manaRegenAll: 0.15 } },
            { count: 3, bonus: '+30% hồi mana + khiên 200HP đầu trận', effect: { manaRegenAll: 0.3, shieldStart: 200 } }
        ]
    }
};

// ===================== HEROES =====================
// Mỗi hero có: synergies kép, chỉ số, kỹ năng riêng
const HEROES = [
    // === 1-Cost Common ===
    {
        id: 'goblin_grunt',
        name: 'Lính Goblin',
        cost: 1,
        rarity: 'common',
        synergies: ['beast', 'warrior'],
        stats: { hp: 500, atk: 45, def: 5, atkSpd: 0.7, range: 1, mana: 60 },
        ability: { name: 'Gào Thét', desc: 'Gào thét gây 100 sát thương và giảm 20% giáp kẻ địch gần nhất trong 4 giây', type: 'active', damage: 100 },
        avatar: 'linear-gradient(135deg, #4a7c3a 0%, #2d5a1e 50%, #1a3d10 100%)'
    },
    {
        id: 'shadow_imp',
        name: 'Tiểu Quỷ',
        cost: 1,
        rarity: 'common',
        synergies: ['undead', 'assassin'],
        stats: { hp: 400, atk: 55, def: 3, atkSpd: 0.8, range: 1, mana: 50 },
        ability: { name: 'Đâm Lén', desc: 'Đâm lén gây 150 sát thương cộng thêm 50% nếu đánh sau lưng', type: 'active', damage: 150 },
        avatar: 'linear-gradient(135deg, #1a1a2e 0%, #4a0e4e 50%, #2d0a30 100%)'
    },
    {
        id: 'frost_sprite',
        name: 'Tinh Băng',
        cost: 1,
        rarity: 'common',
        synergies: ['elemental', 'mage'],
        stats: { hp: 380, atk: 40, def: 2, atkSpd: 0.6, range: 3, mana: 50 },
        ability: { name: 'Tia Băng', desc: 'Bắn tia băng gây 120 sát thương phép và làm chậm 30% trong 3 giây', type: 'active', damage: 120 },
        avatar: 'linear-gradient(135deg, #e0f2fe 0%, #38bdf8 40%, #0369a1 100%)'
    },
    {
        id: 'stone_guard',
        name: 'Hộ Vệ Đá',
        cost: 1,
        rarity: 'common',
        synergies: ['elemental', 'tank'],
        stats: { hp: 700, atk: 30, def: 10, atkSpd: 0.5, range: 1, mana: 80 },
        ability: { name: 'Da Đá', desc: 'Tăng 30 giáp trong 5 giây, khiêu khích kẻ địch gần nhất', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #78716c 0%, #57534e 40%, #44403c 100%)'
    },
    {
        id: 'wolf_scout',
        name: 'Sói Trinh Sát',
        cost: 1,
        rarity: 'common',
        synergies: ['beast', 'ranger'],
        stats: { hp: 420, atk: 48, def: 3, atkSpd: 0.75, range: 2, mana: 60 },
        ability: { name: 'Tru Hú', desc: 'Tru hú tăng 20% tốc đánh cho đồng minh Dã Thú gần nhất 4 giây', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #6b7280 0%, #374151 50%, #1f2937 100%)'
    },
    {
        id: 'nature_healer',
        name: 'Đạo Sĩ Rừng',
        cost: 1,
        rarity: 'common',
        synergies: ['beast', 'support'],
        stats: { hp: 450, atk: 35, def: 3, atkSpd: 0.6, range: 2, mana: 55 },
        ability: { name: 'Hồi Sinh', desc: 'Hồi 120 HP cho đồng minh có HP thấp nhất', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #bbf7d0 0%, #22c55e 40%, #166534 100%)'
    },

    // === 2-Cost Uncommon ===
    {
        id: 'flame_knight',
        name: 'Hiệp Sĩ Lửa',
        cost: 2,
        rarity: 'uncommon',
        synergies: ['dragon', 'warrior'],
        stats: { hp: 650, atk: 55, def: 8, atkSpd: 0.7, range: 1, mana: 70 },
        ability: { name: 'Chém Lửa', desc: 'Chém tạo vệt lửa gây 180 sát thương phép cho 2 mục tiêu', type: 'active', damage: 180 },
        avatar: 'linear-gradient(135deg, #fbbf24 0%, #ea580c 40%, #9a3412 100%)'
    },
    {
        id: 'phantom_archer',
        name: 'Cung Ma',
        cost: 2,
        rarity: 'uncommon',
        synergies: ['undead', 'ranger'],
        stats: { hp: 480, atk: 60, def: 3, atkSpd: 0.8, range: 4, mana: 65 },
        ability: { name: 'Mũi Tên Ma', desc: 'Bắn mũi tên xuyên qua 3 kẻ địch gây 100 sát thương mỗi mục tiêu', type: 'active', damage: 100 },
        avatar: 'linear-gradient(135deg, #334155 0%, #1e293b 40%, #475569 80%, #1e293b 100%)'
    },
    {
        id: 'wind_dancer',
        name: 'Vũ Công Gió',
        cost: 2,
        rarity: 'uncommon',
        synergies: ['spirit', 'assassin'],
        stats: { hp: 450, atk: 65, def: 4, atkSpd: 0.85, range: 1, mana: 55 },
        ability: { name: 'Lốc Xoáy', desc: 'Xoáy gây 90 sát thương cho tất cả kẻ địch trong phạm vi 2 ô', type: 'active', damage: 90 },
        avatar: 'linear-gradient(135deg, #dbeafe 0%, #93c5fd 30%, #3b82f6 70%, #1d4ed8 100%)'
    },
    {
        id: 'iron_golem',
        name: 'Golem Sắt',
        cost: 2,
        rarity: 'uncommon',
        synergies: ['elemental', 'tank'],
        stats: { hp: 900, atk: 35, def: 12, atkSpd: 0.45, range: 1, mana: 90 },
        ability: { name: 'Đập Đất', desc: 'Đập đất gây 150 sát thương và choáng 1.5 giây trong phạm vi 2 ô', type: 'active', damage: 150 },
        avatar: 'linear-gradient(135deg, #a1a1aa 0%, #71717a 40%, #52525b 100%)'
    },
    {
        id: 'blood_shaman',
        name: 'Pháp Sư Máu',
        cost: 2,
        rarity: 'uncommon',
        synergies: ['undead', 'support'],
        stats: { hp: 520, atk: 40, def: 4, atkSpd: 0.6, range: 3, mana: 65 },
        ability: { name: 'Nghi Lễ Máu', desc: 'Hy sinh 10% HP để hồi 200 HP cho đồng minh yếu nhất', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #7f1d1d 0%, #991b1b 40%, #450a0a 100%)'
    },
    {
        id: 'spark_mage',
        name: 'Thuật Sĩ Sét',
        cost: 2,
        rarity: 'uncommon',
        synergies: ['elemental', 'mage'],
        stats: { hp: 420, atk: 50, def: 3, atkSpd: 0.65, range: 3, mana: 55 },
        ability: { name: 'Sét Dây Chuyền', desc: 'Sét bật nảy qua 3 mục tiêu, mỗi lần gây 100 sát thương', type: 'active', damage: 100 },
        avatar: 'linear-gradient(135deg, #fef08a 0%, #eab308 40%, #a16207 100%)'
    },

    // === 3-Cost Rare ===
    {
        id: 'dragon_mage',
        name: 'Long Pháp Sư',
        cost: 3,
        rarity: 'rare',
        synergies: ['dragon', 'mage'],
        stats: { hp: 600, atk: 70, def: 5, atkSpd: 0.65, range: 3, mana: 80 },
        ability: { name: 'Hỏa Cầu Rồng', desc: 'Phun cầu lửa gây 300 sát thương phép trong vùng 3x3', type: 'active', damage: 300 },
        avatar: 'linear-gradient(135deg, #ff6b35 0%, #c2185b 40%, #7b1fa2 100%)'
    },
    {
        id: 'beast_lord',
        name: 'Chúa Dã Thú',
        cost: 3,
        rarity: 'rare',
        synergies: ['beast', 'warrior'],
        stats: { hp: 800, atk: 65, def: 8, atkSpd: 0.7, range: 1, mana: 70 },
        ability: { name: 'Triệu Hồi Bầy', desc: 'Triệu hồi 2 sói nhỏ (300HP, 40 ATK) chiến đấu 6 giây', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #854d0e 0%, #a16207 40%, #713f12 100%)'
    },
    {
        id: 'shadow_blade',
        name: 'Bóng Kiếm',
        cost: 3,
        rarity: 'rare',
        synergies: ['spirit', 'assassin'],
        stats: { hp: 550, atk: 80, def: 5, atkSpd: 0.9, range: 1, mana: 60 },
        ability: { name: 'Bước Bóng Tối', desc: 'Dịch chuyển đến kẻ địch yếu nhất, gây 250 sát thương + tàng hình 2 giây', type: 'active', damage: 250 },
        avatar: 'linear-gradient(135deg, #1e1b4b 0%, #312e81 40%, #1e1b4b 80%, #4338ca 100%)'
    },
    {
        id: 'crystal_sage',
        name: 'Hiền Giả Pha Lê',
        cost: 3,
        rarity: 'rare',
        synergies: ['elemental', 'support'],
        stats: { hp: 560, atk: 45, def: 5, atkSpd: 0.6, range: 3, mana: 70 },
        ability: { name: 'Khiên Pha Lê', desc: 'Tạo khiên 300HP cho 2 đồng minh yếu nhất, kéo dài 5 giây', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #e0e7ff 0%, #818cf8 40%, #4f46e5 100%)'
    },
    {
        id: 'bone_titan',
        name: 'Khổng Lồ Xương',
        cost: 3,
        rarity: 'rare',
        synergies: ['undead', 'tank'],
        stats: { hp: 1100, atk: 40, def: 15, atkSpd: 0.4, range: 1, mana: 100 },
        ability: { name: 'Vùng Chết', desc: 'Tạo vùng chết 3x3, gây 80 sát thương/giây trong 4 giây', type: 'active', damage: 320 },
        avatar: 'linear-gradient(135deg, #f5f5f4 0%, #a8a29e 30%, #57534e 70%, #292524 100%)'
    },

    // === 4-Cost Epic ===
    {
        id: 'storm_dragon',
        name: 'Long Bão',
        cost: 4,
        rarity: 'epic',
        synergies: ['dragon', 'ranger'],
        stats: { hp: 750, atk: 85, def: 7, atkSpd: 0.8, range: 4, mana: 90 },
        ability: { name: 'Bão Sét Rồng', desc: 'Gọi bão sét gây 400 sát thương phép cho 3 kẻ địch ngẫu nhiên', type: 'active', damage: 400 },
        avatar: 'linear-gradient(135deg, #fbbf24 0%, #ea580c 30%, #7c3aed 70%, #1e1b4b 100%)'
    },
    {
        id: 'void_assassin',
        name: 'Sát Thủ Hư Không',
        cost: 4,
        rarity: 'epic',
        synergies: ['spirit', 'assassin'],
        stats: { hp: 600, atk: 95, def: 5, atkSpd: 0.95, range: 1, mana: 70 },
        ability: { name: 'Chém Hư Không', desc: 'Chém liên hoàn 4 lần, mỗi lần 120 sát thương, bỏ qua 50% giáp', type: 'active', damage: 480 },
        avatar: 'linear-gradient(135deg, #0f0f23 0%, #581c87 30%, #1e1b4b 60%, #7e22ce 100%)'
    },
    {
        id: 'ancient_guardian',
        name: 'Thủ Hộ Cổ Đại',
        cost: 4,
        rarity: 'epic',
        synergies: ['elemental', 'tank'],
        stats: { hp: 1400, atk: 45, def: 20, atkSpd: 0.35, range: 1, mana: 100 },
        ability: { name: 'Pháo Đài', desc: 'Hóa đá 3 giây: miễn sát thương + phản 30% sát thương nhận được', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #92400e 0%, #78350f 30%, #a16207 60%, #451a03 100%)'
    },
    {
        id: 'arcane_oracle',
        name: 'Tiên Tri Huyền Bí',
        cost: 4,
        rarity: 'epic',
        synergies: ['spirit', 'mage'],
        stats: { hp: 550, atk: 75, def: 4, atkSpd: 0.6, range: 3, mana: 80 },
        ability: { name: 'Thiên Thạch', desc: 'Thiên thạch rơi gây 500 sát thương phép vùng 2x2 + choáng 1.5 giây', type: 'active', damage: 500 },
        avatar: 'linear-gradient(135deg, #ddd6fe 0%, #8b5cf6 30%, #5b21b6 70%, #1e1b4b 100%)'
    },

    // === 5-Cost Legendary ===
    {
        id: 'dragon_emperor',
        name: 'Long Đế',
        cost: 5,
        rarity: 'legendary',
        synergies: ['dragon', 'warrior'],
        stats: { hp: 1200, atk: 100, def: 15, atkSpd: 0.75, range: 1, mana: 100 },
        ability: { name: 'Nộ Long', desc: 'Biến hình rồng 6 giây: +50% chỉ số, phun lửa gây 200 sát thương/giây vùng rộng', type: 'active', damage: 1200 },
        avatar: 'linear-gradient(135deg, #fbbf24 0%, #dc2626 25%, #ea580c 50%, #b91c1c 75%, #fbbf24 100%)'
    },
    {
        id: 'death_lord',
        name: 'Chúa Tể Chết',
        cost: 5,
        rarity: 'legendary',
        synergies: ['undead', 'mage'],
        stats: { hp: 900, atk: 90, def: 8, atkSpd: 0.6, range: 3, mana: 100 },
        ability: { name: 'Chết Chóc', desc: 'Hồi sinh 2 đồng minh đã chết với 50% HP + gây 400 sát thương vùng', type: 'active', damage: 400 },
        avatar: 'linear-gradient(135deg, #0f172a 0%, #1e1b4b 25%, #4c1d95 50%, #581c87 75%, #0f172a 100%)'
    },
    {
        id: 'nature_goddess',
        name: 'Nữ Thần Rừng',
        cost: 5,
        rarity: 'legendary',
        synergies: ['beast', 'support'],
        stats: { hp: 800, atk: 65, def: 6, atkSpd: 0.55, range: 3, mana: 90 },
        ability: { name: 'Phục Sinh', desc: 'Hồi 40% HP toàn đội + triệu hồi 3 tinh linh rừng (200HP, 50ATK)', type: 'active', damage: 0 },
        avatar: 'linear-gradient(135deg, #bbf7d0 0%, #22c55e 25%, #15803d 50%, #166534 75%, #a7f3d0 100%)'
    }
];

// ===================== ITEMS =====================
// Trang bị tạo chiều sâu chiến thuật - kết hợp 2 vật phẩm cơ bản = 1 vật phẩm nâng cao
const BASE_ITEMS = [
    { id: 'sword', name: 'Kiếm', icon: '🗡️', stat: 'atk', value: 15 },
    { id: 'shield', name: 'Khiên', icon: '🛡️', stat: 'def', value: 10 },
    { id: 'staff', name: 'Gậy', icon: '🪄', stat: 'spellDmg', value: 0.2 },
    { id: 'bow', name: 'Cung', icon: '🏹', stat: 'atkSpd', value: 0.15 },
    { id: 'cloak', name: 'Áo Choàng', icon: '🧥', stat: 'magicRes', value: 0.2 },
    { id: 'belt', name: 'Đai', icon: '⚡', stat: 'hp', value: 200 }
];

const COMBINED_ITEMS = [
    { id: 'fire_sword', name: 'Kiếm Lửa', icon: '🔥', recipe: ['sword', 'sword'], desc: '+30 ATK, 25% đòn đánh gây cháy 3 giây', stats: { atk: 30, burnChance: 0.25 } },
    { id: 'blood_blade', name: 'Huyết Kiếm', icon: '🩸', recipe: ['sword', 'belt'], desc: '+15 ATK, +200 HP, hút máu 15%', stats: { atk: 15, hp: 200, lifesteal: 0.15 } },
    { id: 'phantom_edge', name: 'Kiếm Hồn Ma', icon: '👻', recipe: ['sword', 'staff'], desc: '+15 ATK, đòn đánh gây thêm 50 sát thương phép', stats: { atk: 15, onHitMagic: 50 } },
    { id: 'fortress', name: 'Pháo Đài', icon: '🏰', recipe: ['shield', 'shield'], desc: '+20 giáp, giảm 10% sát thương nhận', stats: { def: 20, dmgReduction: 0.1 } },
    { id: 'magic_shield', name: 'Khiên Phép', icon: '💠', recipe: ['shield', 'cloak'], desc: '+10 giáp, +20% kháng phép, phản 50 sát thương phép', stats: { def: 10, magicRes: 0.2, magicThorns: 50 } },
    { id: 'rapid_bow', name: 'Cung Tốc', icon: '⚡', recipe: ['bow', 'bow'], desc: '+30% tốc đánh, đòn thứ 3 gây x2 sát thương', stats: { atkSpd: 0.3, tripleHit: true } },
    { id: 'healing_staff', name: 'Gậy Chữa Lành', icon: '💚', recipe: ['staff', 'belt'], desc: '+200 HP, kỹ năng hồi 15% HP cho đồng minh gần nhất', stats: { hp: 200, healOnCast: 0.15 } },
    { id: 'spell_amp', name: 'Khuếch Đại', icon: '✨', recipe: ['staff', 'staff'], desc: '+40% sát thương phép, giảm 20% mana cần', stats: { spellDmg: 0.4, manaReduction: 0.2 } },
];

// ===================== AUGMENTS =====================
// Tăng cường chiến thuật - chọn mỗi vài vòng, tạo hướng build đa dạng
const AUGMENTS = [
    // Offensive
    { id: 'berserker', name: 'Cuồng Chiến', icon: '🔥', desc: 'Đơn vị dưới 50% HP tăng 40% tốc đánh', tier: 'silver', effect: { lowHpAtkSpd: 0.4 } },
    { id: 'first_blood', name: 'Khát Máu', icon: '🩸', desc: 'Đơn vị hạ gục đối thủ đầu tiên +30% ATK còn lại trận', tier: 'silver', effect: { firstKillAtk: 0.3 } },
    { id: 'arcane_surge', name: 'Sóng Phép', icon: '🌊', desc: 'Kỹ năng gây thêm 25% sát thương sau mỗi lần dùng (cộng dồn)', tier: 'gold', effect: { stackingSpellDmg: 0.25 } },

    // Defensive
    { id: 'iron_skin', name: 'Da Sắt', icon: '🛡️', desc: '+5 giáp toàn đội', tier: 'silver', effect: { teamArmor: 5 } },
    { id: 'last_stand', name: 'Trụ Cuối', icon: '💪', desc: 'Đơn vị cuối cùng còn sống tăng x2 tất cả chỉ số', tier: 'gold', effect: { lastStandMult: 2 } },
    { id: 'regeneration', name: 'Tái Sinh', icon: '💚', desc: 'Toàn đội hồi 3% HP tối đa mỗi giây', tier: 'silver', effect: { teamRegen: 0.03 } },

    // Economy
    { id: 'rich_get_richer', name: 'Giàu Càng Giàu', icon: '💰', desc: '+3 vàng mỗi vòng nhưng không nhận lãi', tier: 'gold', effect: { flatGold: 3, noInterest: true } },
    { id: 'trade_sector', name: 'Chợ Đen', icon: '🏪', desc: 'Cửa hàng hiển thị 6 hero thay vì 5', tier: 'silver', effect: { shopSize: 6 } },

    // Tactical / Positioning
    { id: 'front_line', name: 'Tiên Phong', icon: '⚔️', desc: 'Hàng trước +20% ATK và +300 HP', tier: 'silver', effect: { frontRowAtk: 0.2, frontRowHp: 300 } },
    { id: 'back_line', name: 'Hậu Phương', icon: '🏹', desc: 'Hàng sau +30% tốc đánh và +15% sát thương phép', tier: 'silver', effect: { backRowAtkSpd: 0.3, backRowSpellDmg: 0.15 } },
    { id: 'formation_master', name: 'Bậc Thầy Đội Hình', icon: '♟️', desc: 'Mỗi đơn vị kề nhau +8% ATK (cộng dồn)', tier: 'gold', effect: { adjacencyAtk: 0.08 } },

    // Synergy boosters
    { id: 'synergy_crown', name: 'Vương Miện Hiệp Lực', icon: '👑', desc: 'Tất cả hiệp lực được tính thêm +1 đơn vị', tier: 'prismatic', effect: { synergyBonus: 1 } },
];

// ===================== SHOP ODDS =====================
// Tỉ lệ xuất hiện hero theo cấp người chơi
const SHOP_ODDS = {
    1: { common: 1.00, uncommon: 0.00, rare: 0.00, epic: 0.00, legendary: 0.00 },
    2: { common: 0.70, uncommon: 0.25, rare: 0.05, epic: 0.00, legendary: 0.00 },
    3: { common: 0.55, uncommon: 0.30, rare: 0.15, epic: 0.00, legendary: 0.00 },
    4: { common: 0.40, uncommon: 0.30, rare: 0.25, epic: 0.05, legendary: 0.00 },
    5: { common: 0.25, uncommon: 0.30, rare: 0.30, epic: 0.13, legendary: 0.02 },
    6: { common: 0.15, uncommon: 0.25, rare: 0.30, epic: 0.23, legendary: 0.07 },
    7: { common: 0.10, uncommon: 0.20, rare: 0.25, epic: 0.30, legendary: 0.15 },
    8: { common: 0.05, uncommon: 0.15, rare: 0.20, epic: 0.35, legendary: 0.25 }
};

// ===================== LEVEL / XP =====================
const LEVEL_XP = {
    1: 0,
    2: 2,
    3: 6,
    4: 10,
    5: 20,
    6: 36,
    7: 56,
    8: 80
};

// Max units on board per level
const MAX_UNITS = {
    1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8
};

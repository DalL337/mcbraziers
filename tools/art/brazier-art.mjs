// Generates the brazier art that is clearer as code than by hand: the brazier part models
// (one model per part, each side baked for its direction) and the original 16x16 textures.
//
//   node tools/art/brazier-art.mjs [--preview <pack dir>]
//
// Writes into src/main/resources/assets/mcbraziers. Datagen (McBraziersModelProvider) assembles the
// parts into blockstates and item models for each tier. --preview also writes whole-brazier models with
// concrete textures into a scratch resource pack, for rendering in Blockbench.
//
// The shape is the owner's base model (temp/BrazierBlockGrd.bbmodel) with the changes agreed on
// 2026-10-18 (docs/plans/art/copper-brazier-art.md): the same slats on every side, sides that open into
// fire pits, a grate lid inside the rim, an ember bed and campfire-style crossed flames. The wall and
// hanging braziers are built from the same pieces.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { blank, set, save } from './png.mjs';

const ASSETS = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../src/main/resources/assets/mcbraziers');
const previewArg = process.argv.indexOf('--preview');
const PREVIEW = previewArg > 0 ? process.argv[previewArg + 1] : null;

/** Writes JSON with two-space indent, keeping number arrays (coordinates, UVs) on one line. */
const writeJson = (file, value) => {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const text = JSON.stringify(value, null, 2).replace(/\[\s+([-\d.,\s]+?)\s+\]/g, (_, nums) => `[${nums.split(/,\s*/).join(', ')}]`);
  fs.writeFileSync(file, text + '\n');
};

// --- Textures ---------------------------------------------------------------------------------------

const rgb = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
/** Deterministic per-pixel noise in [0, 1). */
const hash = (x, y, s = 0) => { let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0; h = (h ^ (h >>> 13)) * 1274126177; return ((h ^ (h >>> 16)) >>> 0) / 4294967296; };

// One texture per tier (and per copper stage): five tones, dark to light, close to the palette of the
// tier's vanilla material so braziers sit beside it. Accents are the material's own touches: patina on
// aging copper, glints on diamond, flecks on obsidian and netherite, mottling on prismarine.
const TIER_TEXTURES = {
  copper_brazier:           { ramp: ['#7a3a24', '#904931', '#a95b40', '#bf6a4c', '#d9805f'], seed: 1 },
  exposed_copper_brazier:   { ramp: ['#64574a', '#7b6b58', '#93735e', '#a97c66', '#bf8a78'], patina: ['#4c7264', '#6a8455'], patinaRate: 0.12, seed: 2 },
  weathered_copper_brazier: { ramp: ['#3e5c52', '#4c7264', '#6a8455', '#6f9a62', '#71b082'], patina: ['#93735e', '#a97c66'], patinaRate: 0.14, seed: 3 },
  oxidized_copper_brazier:  { ramp: ['#386350', '#3d725d', '#43866f', '#4f9a88', '#62b49a'], seed: 4 },
  iron_brazier:             { ramp: ['#5e5f60', '#77797a', '#9c9d9d', '#b8b9b9', '#d9dada'], seed: 5 },
  diamond_brazier:          { ramp: ['#0b7e80', '#14a9ac', '#2ccfd2', '#62e9e3', '#b2fbf0'], patina: ['#e6fffb', '#d5fff6'], patinaRate: 0.04, seed: 6 },
  obsidian_brazier:         { ramp: ['#0a0812', '#151020', '#231a35', '#33264c', '#4b3870'], patina: ['#5d3f8c', '#3b2754'], patinaRate: 0.06, seed: 7 },
  prismarine_brazier:       { ramp: ['#3a7462', '#46876f', '#59a08c', '#6db7a6', '#8fd0c3'], patina: ['#2f5f55', '#7fc4b8'], patinaRate: 0.1, seed: 8 },
  netherite_brazier:        { ramp: ['#241f20', '#332c2d', '#443c3d', '#575051', '#6f6767'], patina: ['#4c3a35', '#3b3232'], patinaRate: 0.08, seed: 9 },
};

/** True where the grate has metal: a 4 px tile of [bar, hole, hole, bar], counted from world x = 2. */
const grateBar = (w) => { const p = (((w - 2) % 4) + 4) % 4; return p === 0 || p === 3; };

/**
 * A tier texture. Rows 0-1 rim band, rows 2-3 basin band, cols 0-1 of rows 4-15 a slat, cols 4-15 of
 * rows 4-15 the grate (pixel u maps to world x = u - 2, so its tile lines up across fire pits).
 */
function tierTexture(stage) {
  const img = blank(16, 16);
  const put = painter(img, stage);
  for (let x = 0; x < 16; x++) {
    put(x, 0, x % 4 === 1 ? 4 : 3);
    put(x, 1, x % 4 === 3 ? 2 : 1);
    const rivet = [1, 5, 10, 14].includes(x);
    put(x, 2, rivet ? 4 : 3);
    put(x, 3, rivet ? 0 : 2);
  }
  for (let y = 4; y < 16; y++) {
    put(0, y, y === 4 ? 4 : (y * 7) % 5 === 0 ? 2 : 3);
    put(1, y, y === 4 ? 3 : 1);
    put(2, y, y === 4 ? 4 : y > 12 ? 2 : 3);
    put(3, y, y === 4 ? 3 : y > 12 ? 0 : 1);
  }
  for (let v = 4; v < 16; v++) for (let u = 4; u < 16; u++) {
    const onU = grateBar(u - 2), onV = grateBar(v - 2);
    if (!onU && !onV) continue;
    const lit = (onU && (((u - 4) % 4) + 4) % 4 === 0) || (onV && (((v - 4) % 4) + 4) % 4 === 0);
    put(u, v, onU && onV ? 3 : lit ? 3 : 2);
  }
  return img;
}

const COAL = ['#1c1a19', '#2a2624', '#383330', '#47403b'];
const EMBER_GLOWS = {
  brazier_embers_lit:        ['#7a2a10', '#c4501a', '#f08a2a', '#ffd46a'],
  brazier_soul_embers_lit:   ['#0f4a52', '#1b8a94', '#4fd0d8', '#b8fbff'],
  brazier_copper_embers_lit: ['#1d4a1c', '#3f8f2e', '#7fd34a', '#d4ff9a'],
  brazier_amethyst_embers_lit: ['#3a1260', '#7a2fb8', '#b86cf0', '#ecc8ff'],
};
const EMBER_FRAMES = 4;

function coalPixel(x, y) { return COAL[Math.floor(hash(y, x, 3) * COAL.length)]; }

/** Dead coals with a little ash, for an unlit brazier. */
function deadEmbers() {
  const img = blank(16, 16);
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const n = hash(x, y, 11);
    set(img, x, y, rgb(n < 0.04 ? '#8a847e' : n < 0.09 ? '#6b6560' : coalPixel(x, y)));
  }
  return img;
}

/** Glowing coals as an animation strip: each glowing pixel cycles dim, mid, bright, mid at its own phase. */
function litEmbers(glow) {
  const img = blank(16, 16 * EMBER_FRAMES);
  const cycle = [0, 1, 2, 1];
  for (let f = 0; f < EMBER_FRAMES; f++) for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const n = hash(x, y, 7);
    let c = coalPixel(x, y);
    if (n < 0.26) {
      const level = cycle[(Math.floor(hash(x, y, 9) * 4) + f) % 4] + (n < 0.06 ? 1 : 0);
      c = glow[Math.min(level, glow.length - 1)];
    }
    set(img, x, f * 16 + y, rgb(c));
  }
  return img;
}

/** Paints a pixel in one of the tier's five tones, or now and then in one of its accent colours. */
function painter(img, { ramp, patina, patinaRate = 0, seed }) {
  return (x, y, tone) => {
    const accent = patina && hash(x, y, seed) < patinaRate;
    set(img, x, y, rgb(accent ? patina[hash(y, x, seed) < 0.5 ? 0 : 1] : ramp[tone]));
  };
}

/** The mod's placeable bars, in the matching brazier's palette, so bars and braziers of a tier match. */
const BARS = { diamond_bars: 'diamond_brazier', obsidian_bars: 'obsidian_brazier', prismarine_bars: 'prismarine_brazier',
  netherite_bars: 'netherite_brazier' };

/**
 * A bars texture for vanilla's bars models (template_bars_*): a grille of three vertical bars and two
 * full-width crossbars, which carry on into the next pane. The models draw a post from columns 7-8 and each
 * side panel from one half, so the middle bar sits on those columns and the outer bars line up across panes.
 */
function barsTexture(stage) {
  const img = blank(16, 16);
  const put = painter(img, stage);
  const bars = [2, 7, 12];
  const crossbars = [4, 11];
  for (const x of bars) for (let y = 0; y < 16; y++) {
    put(x, y, (y * 5) % 7 === 0 ? 2 : 3);
    put(x + 1, y, 1);
  }
  for (const y of crossbars) for (let x = 0; x < 16; x++) {
    const onBar = bars.includes(x) || bars.includes(x - 1);
    // A rivet where a crossbar meets a bar: a bright top-left pixel over a dark one
    put(x, y, onBar ? (bars.includes(x) ? 4 : 3) : 3);
    put(x, y + 1, onBar ? (bars.includes(x) ? 2 : 0) : 1);
  }
  return img;
}

// Crying obsidian (PRD Content addendum and erratum): the obsidian palette with still tear stains in the
// purples of vanilla's crying obsidian, dark to bright. The stains don't move; vanilla's falling tear
// particles carry the motion, as on the crying obsidian block.
const TEARS = ['#2b0178', '#6006ab', '#8308e4', '#b04cff'];

/**
 * Paints one tear stain on a 2 px wide bar or slat whose lit column is x (the shade column is x + 1): a bead
 * where the tear formed, a broken trail below it, and a pool at the foot (rows 14-15).
 */
function stain(img, x, { bead, trail, pool, dots = [] }) {
  const at = (px, py, tone) => set(img, px, py, rgb(TEARS[tone]));
  if (trail) for (let y = trail[0]; y <= trail[1]; y++) if (hash(x, y, 17) < 0.75) at(x, y, (y * 5) % 3 === 0 ? 1 : 0);
  for (const y of dots) at(x + (y % 2), y, 1);
  if (bead !== undefined) {
    at(x, bead, 3);
    at(x + 1, bead, 1);
    at(x, bead + 1, 2);
    at(x + 1, bead + 1, 0);
  }
  if (pool === 'small') at(x, 15, 1);
  else if (pool) {
    at(x, 14, 1);
    at(x + 1, 14, 0);
    at(x, 15, 3);
    at(x + 1, 15, 2);
  }
}

/**
 * The crying brazier's slat strips: eight 2 px strips of slat, each stained differently. Slats read their
 * strip from the bottom (rows 16 - height to 15), so the pool at the foot shows on every slat height and the
 * higher stains on the taller slats only (floor 10 px, hanging 8 px, wall 6 px).
 */
const SLAT_STAINS = [
  { bead: 6, trail: [8, 11], pool: true },
  { pool: true, dots: [10, 12] },
  { bead: 8, trail: [10, 12] },
  { bead: 6, trail: [8, 9], pool: true },
  { bead: 10, trail: [12, 12] },
  { trail: [6, 13], pool: true },
  { bead: 11, pool: true },
  { pool: 'small' },
];

function tearsTexture(stage) {
  const img = blank(16, 16);
  const put = painter(img, stage);
  SLAT_STAINS.forEach((spec, k) => {
    for (let y = 0; y < 16; y++) {
      put(2 * k, y, (y * 7 + k) % 5 === 0 ? 2 : 3);
      put(2 * k + 1, y, 1);
    }
    stain(img, 2 * k, spec);
  });
  return img;
}

/** The crying brazier's own texture: obsidian's, with still beads pooled along the rim and the basin's foot. */
function cryingBrazierTexture(stage) {
  const img = tierTexture(stage);
  for (const [x, y, tone] of [[3, 3, 2], [4, 3, 1], [8, 3, 1], [12, 3, 2], [13, 3, 1], [6, 1, 1], [11, 1, 0]]) set(img, x, y, rgb(TEARS[tone]));
  return img;
}

/**
 * Four still stain versions of the crying bars, one per block by its position. Each lists a stain per bar
 * (x = 2, 7, 12); tears gather under the crossbars (rows 6 and 13) and pool at the foot.
 */
const BARS_STAINS = [
  [{ bead: 6, trail: [8, 9] }, { pool: true }, { trail: [1, 3], bead: 13 }],
  [{ bead: 13, pool: true }, { bead: 6, trail: [8, 10] }, {}],
  [{ trail: [0, 3] }, { bead: 13, pool: true }, { bead: 6, trail: [8, 10], pool: true }],
  [{}, { trail: [0, 3], bead: 6 }, { bead: 13 }],
];

function cryingBarsTexture(stage, version) {
  const img = barsTexture(stage);
  BARS_STAINS[version].forEach((spec, i) => stain(img, [2, 7, 12][i], spec));
  return img;
}

/**
 * Amethyst fire's colours (PRD License addendum, recolouring at load): crying obsidian's dark purples up to
 * amethyst's light ones. The game recolours vanilla's soul flame onto this ramp by brightness
 * (RecoloredSprite); the generator writes it into both atlas files so it lives here only.
 */
const AMETHYST_RAMP = ['#3b0890', '#6006ab', '#8308e4', '#a65cf0', '#cfa0f3', '#fdf1ff'];

function writeAtlases() {
  const atlases = path.resolve(ASSETS, '../minecraft/atlases');
  const recolor = (source, sprite) => ({ type: 'mcbraziers:recolor', source, sprite, ramp: AMETHYST_RAMP });
  writeJson(path.join(atlases, 'blocks.json'),
    { sources: [recolor('minecraft:block/soul_campfire_fire', 'mcbraziers:block/amethyst_campfire_fire')] });
  writeJson(path.join(atlases, 'particles.json'),
    { sources: [recolor('minecraft:particle/soul_fire_flame', 'mcbraziers:amethyst_fire_flame')] });
}

function writeTextures() {
  const dir = path.join(ASSETS, 'textures/block');
  for (const [name, stage] of Object.entries(TIER_TEXTURES)) save(path.join(dir, name + '.png'), tierTexture(stage));
  for (const [name, tier] of Object.entries(BARS)) save(path.join(dir, name + '.png'), barsTexture(TIER_TEXTURES[tier]));
  const obsidian = TIER_TEXTURES.obsidian_brazier;
  save(path.join(dir, 'crying_obsidian_brazier.png'), cryingBrazierTexture(obsidian));
  save(path.join(dir, 'crying_obsidian_brazier_tears.png'), tearsTexture(obsidian));
  BARS_STAINS.forEach((_, v) => save(path.join(dir, `crying_obsidian_bars_${v}.png`), cryingBarsTexture(obsidian, v)));
  save(path.join(dir, 'brazier_embers.png'), deadEmbers());
  // Fully transparent: the Copper Campfire hides the flame of vanilla's campfire model with it and draws
  // its own tintable flame instead (models/block/brazier/campfire_fire.json).
  save(path.join(dir, 'hidden_campfire_fire.png'), blank(16, 16));
  for (const [name, glow] of Object.entries(EMBER_GLOWS)) {
    save(path.join(dir, name + '.png'), litEmbers(glow));
    writeJson(path.join(dir, name + '.png.mcmeta'), { animation: { frametime: 8, interpolate: true } });
  }
}

// --- Geometry ---------------------------------------------------------------------------------------

const FACES = ['north', 'east', 'south', 'west', 'up', 'down'];
const round = (v) => Math.round(v * 1000) / 1000;
/** Atlas regions [u0, v0, u1, v1] in the tier texture. */
const REGION = { rim: [0, 0, 16, 2], basin: [0, 2, 16, 4], slat: [0, 4, 2, 16] };

/** A cube whose faces take the top-left corner of an atlas region, sized to the face. */
function atlasCube(from, to, region, extra = {}) {
  const [sx, sy, sz] = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
  const dims = { north: [sx, sy], south: [sx, sy], east: [sz, sy], west: [sz, sy], up: [sx, sz], down: [sx, sz] };
  const faces = {};
  for (const f of extra.faces || FACES) {
    const [w, h] = dims[f], [u0, v0, u1, v1] = region;
    faces[f] = { uv: [u0, v0, round(Math.min(u1, u0 + Math.max(w, 0.01))), round(Math.min(v1, v0 + Math.max(h, 0.01)))], texture: '#body' };
  }
  const e = { from, to, faces };
  if (extra.rotation) e.rotation = extra.rotation;
  return e;
}

/** A slat: an atlas cube from the slat region, marked so the crying brazier can give it its own stain. */
function slatCube(from, to) {
  return { ...atlasCube(from, to, REGION.slat), slat: true };
}

/** A cube with only its top face, textured by world position (the ember bed). */
function bedCube(from, to) {
  return { from, to, faces: { up: { uv: 'world', texture: '#embers' } } };
}

/** A grate slab, its top and bottom textured by world position in the tier texture's grate region. */
function grateCube(from, to) {
  return { from, to, faces: { up: { uv: 'grate', texture: '#body' }, down: { uv: 'grate', texture: '#body' } } };
}

/** Rotates a point a quarter turn clockwise (seen from above) about the block centre, `turns` times. */
function turnPoint([x, y, z], turns) {
  for (let i = 0; i < turns; i++) [x, z] = [16 - z, x];
  return [round(x), y, round(z)];
}
const NEXT_FACE = { north: 'east', east: 'south', south: 'west', west: 'north', up: 'up', down: 'down' };

/** The same element turned clockwise, as a blockstate y rotation without uvlock would show it. */
function turnElement(e, turns) {
  if (turns === 0) return structuredClone(e);
  const a = turnPoint(e.from, turns), b = turnPoint(e.to, turns);
  const out = { from: [Math.min(a[0], b[0]), e.from[1], Math.min(a[2], b[2])], to: [Math.max(a[0], b[0]), e.to[1], Math.max(a[2], b[2])], faces: {} };
  for (const [name, face] of Object.entries(e.faces)) {
    let n = name;
    for (let i = 0; i < turns; i++) n = NEXT_FACE[n];
    const f = { ...face };
    if (Array.isArray(f.uv) && (name === 'up' || name === 'down')) {
      f.rotation = ((f.rotation || 0) + (name === 'up' ? 90 : 270) * turns) % 360;
      if (!f.rotation) delete f.rotation;
    }
    out.faces[n] = f;
  }
  if (e.slat) out.slat = true;
  if (e.rotation) {
    // A turn maps the +x axis to +z and the +z axis to -x.
    let { axis, angle } = e.rotation;
    for (let i = 0; i < turns; i++) {
      if (axis === 'x') axis = 'z';
      else if (axis === 'z') { axis = 'x'; angle = -angle; }
    }
    out.rotation = { origin: turnPoint(e.rotation.origin, turns), axis, angle };
  }
  return out;
}

/** Resolves world-mapped UVs ('world' for the bed, 'grate' for the grate region) from final positions. */
function resolveUvs(e) {
  const out = structuredClone(e);
  for (const [name, face] of Object.entries(out.faces)) {
    if (face.uv === 'world') face.uv = [e.from[0], e.from[2], e.to[0], e.to[2]];
    else if (face.uv === 'grate') {
      // Pixel u shows world x = u - 2; shift by whole 4 px tiles to stay inside the 12 px region.
      const fit = (lo, hi) => { let s = 2; while (lo + s < 4) s += 4; while (hi + s > 16) s -= 4; return [lo + s, hi + s]; };
      const [u0, u1] = fit(e.from[0], e.to[0]), [v0, v1] = fit(e.from[2], e.to[2]);
      face.uv = name === 'down' ? [u0, v1, u1, v0] : [u0, v0, u1, v1];
    }
    face.uv = face.uv.map(round);
  }
  return out;
}

// Parts, drawn for the north side. Basin walls, rims and corner slats form a pinwheel so the four
// turned copies meet without overlapping faces.
const north = {
  side: () => [
    atlasCube([2, 1, 2], [13, 3, 3], REGION.basin),
    ...[[2, 4], [5, 7], [9, 11], [12, 13.7]].map(([x0, x1]) => slatCube([x0, 3, 2], [x1, 13, 2.3])),
    atlasCube([1.5, 11, 1.5], [14, 13, 2], REGION.rim),
    ...[2.1, 5.1, 9.1, 12.1].map((x) => atlasCube([x, 13.7, 1.3], [x + 2, 15.7, 1.55], REGION.slat,
      { rotation: { origin: [x, 13.7, 3.3], axis: 'x', angle: -22.5 } })),
  ],
  bridge: () => [atlasCube([2, 0, 0], [14, 1, 2], REGION.basin)],
  bridgeCcw: () => [atlasCube([2, 1, 0], [3, 3, 3], REGION.basin), atlasCube([1.5, 11, 0], [2, 13, 2], REGION.rim)],
  bridgeCw: () => [atlasCube([13, 1, 0], [14, 3, 2], REGION.basin), atlasCube([14, 11, 0], [14.5, 13, 1.5], REGION.rim)],
  corner: () => [atlasCube([14, 0, 0], [16, 1, 2], REGION.basin)],
  grateBridge: () => [grateCube([2.3, 12, 0], [13.7, 13, 2.3])],
  grateCorner: () => [grateCube([13.7, 12, 0], [16, 13, 2.3])],
  bedBridge: () => [bedCube([3, 1, 0], [13, 1.5, 3])],
  bedCorner: () => [bedCube([13, 1, 0], [16, 1.5, 3])],
};
const centre = {
  base: () => [atlasCube([2, 0, 2], [14, 1, 14], REGION.basin)],
  grate: () => [grateCube([2.3, 12, 2.3], [13.7, 13, 13.7])],
  bed: () => [bedCube([3, 1, 3], [13, 1.5, 13])],
  fire: () => crossedFire(8, 8, 1, 15, 7.2),
};

const DIRS = ['north', 'east', 'south', 'west'];
/** Which texture slot a part uses, for its particle texture. */
const SLOT = { bed: 'embers', bedBridge: 'embers', bedCorner: 'embers', fire: 'fire' };
const FILE = { side: 'side', bridge: 'bridge', bridgeCcw: 'bridge_ccw', bridgeCw: 'bridge_cw', corner: 'corner',
  grate: 'grate', grateBridge: 'grate_bridge', grateCorner: 'grate_corner', base: 'base', bed: 'bed',
  bedBridge: 'bed_bridge', bedCorner: 'bed_corner', fire: 'fire' };

/** Every floor part: name -> elements. Directional parts get one model per direction. */
function floorParts() {
  const parts = {};
  for (const [key, make] of Object.entries(centre)) parts[`floor_${FILE[key]}`] = { slot: SLOT[key] || 'body', elements: make().map(resolveUvs) };
  for (const [key, make] of Object.entries(north)) DIRS.forEach((dir, turns) => {
    parts[`floor_${FILE[key]}_${dir}`] = { slot: SLOT[key] || 'body', elements: make().map((e) => resolveUvs(turnElement(e, turns))) };
  });
  return parts;
}

/**
 * Campfire-style crossed flames centred on (cx, cz), from y0 to y1, `half` px either side of the centre.
 * Tint index 0 lets copper fire colour vanilla's soul campfire flame green (PRD License addendum,
 * 2026-10-18); normal and soul fire draw untinted.
 */
function crossedFire(cx, cz, y0, y1, half, rescale = false) {
  const face = { uv: [0, 0, 16, 16], texture: '#fire', tintindex: 0 };
  const rotation = { origin: [cx, 8, cz], axis: 'y', angle: 45, ...(rescale ? { rescale: true } : {}) };
  return [
    { from: [round(cx - half), y0, cz], to: [round(cx + half), y1, cz], rotation, faces: { north: face, south: face } },
    { from: [cx, y0, round(cz - half)], to: [cx, y1, round(cz + half)], rotation, faces: { west: face, east: face } },
  ];
}

/** A crown tip leaning out over the north side, as the floor brazier's are. */
function tip(x, y, z, originZ) {
  return atlasCube([x, y, z], [x + 2, y + 2, z + 0.25], REGION.slat, { rotation: { origin: [x, y, originZ], axis: 'x', angle: -22.5 } });
}

/**
 * Hanging brazier (approved 2026-10-18): a 10 px basket with a grate lid and crown, hung by a chain loop
 * set up like vanilla's hanging lantern. The loop takes vanilla's chain texture (#chain, the 3 px strip
 * on its left) so it matches the chain above. Hanging braziers never join, so the cage is one part.
 */
function ceilingParts() {
  const side = () => [
    atlasCube([3, 2, 3], [12, 4, 4], REGION.basin),
    ...[[3, 5], [7, 9], [11, 12.7]].map(([a, b]) => slatCube([a, 4, 3], [b, 12, 3.3])),
    atlasCube([2.5, 10, 2.5], [12, 12, 3], REGION.rim),
    ...[3.1, 7.1, 11.1].map((x) => tip(x, 12.7, 2.3, 4.3)),
  ];
  const chain = { uv: [0, 0, 3, 4], texture: '#chain' };
  const cage = [
    atlasCube([3, 1, 3], [13, 2, 13], REGION.basin),
    ...DIRS.flatMap((_, turns) => side().map((e) => turnElement(e, turns))),
    grateCube([3.3, 11, 3.3], [12.7, 12, 12.7]),
    { from: [6.5, 12, 8], to: [9.5, 16, 8], rotation: { origin: [8, 8, 8], axis: 'y', angle: 45 }, faces: { north: chain, south: chain } },
    { from: [8, 12, 6.5], to: [8, 16, 9.5], rotation: { origin: [8, 8, 8], axis: 'y', angle: 45 }, faces: { west: chain, east: chain } },
  ];
  return {
    ceiling_cage: { slot: 'body', elements: cage.map(resolveUvs) },
    ceiling_bed: { slot: 'embers', elements: [bedCube([4, 2, 4], [12, 2.5, 12])].map(resolveUvs) },
    ceiling_fire: { slot: 'fire', elements: crossedFire(8, 8, 2, 14, 5.5) },
  };
}

/** A span across a wall brazier drawn facing north: as given on its west side, mirrored onto its east. */
const wallMirror = (side, x0, x1) => (side === 'west' ? [x0, x1] : [16 - x1, 16 - x0]);

/** The wall bracket: plate with a cap, a foot and two rivets; the arm under the basket; the brace up to the arm. */
function wallBracket() {
  return [
    atlasCube([7, 1, 15], [9, 13, 16], REGION.slat),
    atlasCube([6.5, 13, 14.5], [9.5, 14, 16], REGION.rim),
    atlasCube([6.5, 0.5, 14.5], [9.5, 1.5, 16], REGION.rim),
    ...[3.5, 10.5].map((y) => atlasCube([7.5, y, 14.7], [8.5, y + 1, 15], REGION.rim)),
    atlasCube([7, 3, 6], [9, 4, 15], REGION.slat),
    atlasCube([7, 2.2, 6], [9, 3, 7], REGION.slat),
    atlasCube([7, 0.3, 11.9], [9, 1.3, 15], REGION.slat, { rotation: { origin: [8, 0.8, 15], axis: 'x', angle: 45 } }),
  ];
}

/**
 * Wall brazier, drawn facing north with the wall at z = 16; blockstates turn it for other facings. A
 * basket 12 wide and 8 deep, centred in the block and held off the wall by a bracket (owner's design,
 * 2026-10-18): a 2 px wall plate with a cap, a foot and two rivets, an arm under the basket, and a
 * diagonal brace from the plate up to the arm. Its west and east sides open into the next wall brazier
 * along the same wall.
 */
function wallParts() {
  const mirror = wallMirror;
  const side = (s) => {
    const [w0, w1] = mirror(s, 2, 3), [s0, s1] = mirror(s, 2, 2.3), [r0, r1] = mirror(s, 1.5, 2);
    // The floor brazier's north tips, turned onto this side, two to a side either side of its middle.
    const tips = s === 'west' ? [8.9, 5.1].map((a) => turnElement(tip(a, 13.7, 1.3, 3.3), 3))
      : [5.1, 8.9].map((a) => turnElement(tip(a, 13.7, 1.3, 3.3), 1));
    return [
      atlasCube([w0, 5, 5], [w1, 7, 11], REGION.basin),
      ...[[4.3, 6], [7, 9], [10, 11.7]].map(([a, b]) => slatCube([s0, 7, a], [s1, 13, b])),
      atlasCube([r0, 11, 4], [r1, 13, 12], REGION.rim),
      ...tips,
    ];
  };
  const bridge = (s) => {
    const [f0, f1] = mirror(s, 0, 2), [r0, r1] = mirror(s, 0, 1.5), [g0, g1] = mirror(s, 0, 2.3);
    return [
      atlasCube([f0, 4, 4], [f1, 5, 12], REGION.basin),
      atlasCube([f0, 5, 4], [f1, 7, 5], REGION.basin),
      atlasCube([f0, 5, 11], [f1, 7, 12], REGION.basin),
      atlasCube([r0, 11, 3.5], [r1, 13, 4], REGION.rim),
      atlasCube([r0, 11, 12], [r1, 13, 12.5], REGION.rim),
      grateCube([g0, 12, 4.3], [g1, 13, 11.7]),
    ];
  };
  const slats = [[2, 4], [5, 7], [9, 11], [12, 14]];
  const core = [
    // Basket: floor, front and back walls, slats, rim, grate; tips over the front only, none toward the wall
    atlasCube([2, 4, 4], [14, 5, 12], REGION.basin),
    atlasCube([2, 5, 4], [14, 7, 5], REGION.basin),
    atlasCube([2, 5, 11], [14, 7, 12], REGION.basin),
    ...slats.map(([a, b]) => slatCube([a, 7, 4], [b, 13, 4.3])),
    ...slats.map(([a, b]) => slatCube([a, 7, 11.7], [b, 13, 12])),
    atlasCube([1.5, 11, 3.5], [14.5, 13, 4], REGION.rim),
    atlasCube([1.5, 11, 12], [14.5, 13, 12.5], REGION.rim),
    ...[2.1, 5.1, 9.1, 12.1].map((x) => tip(x, 13.7, 3.3, 5.3)),
    grateCube([2.3, 12, 4.3], [13.7, 13, 11.7]),
    ...wallBracket(),
  ];
  const parts = {
    wall_core: { slot: 'body', elements: core.map(resolveUvs) },
    wall_bed: { slot: 'embers', elements: [bedCube([3, 5, 5], [13, 5.5, 11])].map(resolveUvs) },
    wall_fire: { slot: 'fire', elements: crossedFire(8, 8, 5, 15, 5) },
  };
  for (const s of ['west', 'east']) {
    const [b0, b1] = mirror(s, 0, 3);
    parts[`wall_side_${s}`] = { slot: 'body', elements: side(s).map(resolveUvs) };
    parts[`wall_bridge_${s}`] = { slot: 'body', elements: bridge(s).map(resolveUvs) };
    parts[`wall_bed_bridge_${s}`] = { slot: 'embers', elements: [bedCube([b0, 5, 5], [b1, 5.5, 11])].map(resolveUvs) };
  }
  return parts;
}

/**
 * Inside corner (owner's rulings, 2026-10-25): a wall brazier joined on its front turns its run through a
 * right angle into the run coming along the other wall. Drawn facing north like the rest of the wall
 * brazier, it narrows to an 8 px junction in the middle of the block, as wide as the run coming in from
 * the front. An arm reaches forward into that run, and each side either reaches into the next brazier
 * along this wall or closes with an end wall. Arms are open like the bridges between braziers; the bracket
 * stays on this brazier's own wall.
 *
 * Outside corner (owner, 2026-10-25): the corner block diagonal to a pillar's edge joins the runs on its two
 * faces. It is the same L turned to face its back, which joins the run on the other face, and it has no
 * bracket: its neighbours hold it.
 */
function wallCornerParts() {
  const mirror = wallMirror;
  const arm = (s) => {
    const [a0, a1] = mirror(s, 0, 4), [f0, f1] = mirror(s, 0, 3.5), [g0, g1] = mirror(s, 0, 4.3);
    return [
      atlasCube([a0, 4, 4], [a1, 5, 12], REGION.basin),
      atlasCube([a0, 5, 4], [a1, 7, 5], REGION.basin),
      atlasCube([a0, 5, 11], [a1, 7, 12], REGION.basin),
      atlasCube([f0, 11, 3.5], [f1, 13, 4], REGION.rim),
      atlasCube([a0, 11, 12], [a1, 13, 12.5], REGION.rim),
      grateCube([g0, 12, 4.3], [g1, 13, 11.7]),
    ];
  };
  const end = (s) => {
    const [w0, w1] = mirror(s, 4, 5), [s0, s1] = mirror(s, 4, 4.3), [r0, r1] = mirror(s, 3.5, 4);
    return [
      atlasCube([w0, 5, 5], [w1, 7, 11], REGION.basin),
      ...[[4.3, 6], [7, 9], [10, 11.7]].map(([a, b]) => slatCube([s0, 7, a], [s1, 13, b])),
      atlasCube([r0, 11, 4], [r1, 13, 12.5], REGION.rim),
    ];
  };
  const parts = {
    // Junction: floor, back wall with the two middle slats of a straight basket, back rim, grate, bracket
    // The bracket is its own part: an outside corner (wallOuterParts) shares the arm forward and has none
    wall_corner_core: { slot: 'body', elements: [
      atlasCube([4, 4, 4], [12, 5, 12], REGION.basin),
      atlasCube([4, 5, 11], [12, 7, 12], REGION.basin),
      ...[[5, 7], [9, 11]].map(([a, b]) => slatCube([a, 7, 11.7], [b, 13, 12])),
      atlasCube([4, 11, 12], [12, 13, 12.5], REGION.rim),
      grateCube([4.3, 12, 4.3], [11.7, 13, 11.7]),
    ].map(resolveUvs) },
    wall_corner_bracket: { slot: 'body', elements: wallBracket().map(resolveUvs) },
    // Forward into the brazier in front, whose side bridge meets it at the block edge; its low walls run on
    // to the junction's corners so the inside of the turn has no gap
    wall_corner_front: { slot: 'body', elements: [
      atlasCube([4, 4, 0], [12, 5, 4], REGION.basin),
      atlasCube([4, 5, 0], [5, 7, 5], REGION.basin),
      atlasCube([11, 5, 0], [12, 7, 5], REGION.basin),
      atlasCube([3.5, 11, 0], [4, 13, 4], REGION.rim),
      atlasCube([12, 11, 0], [12.5, 13, 4], REGION.rim),
      grateCube([4.3, 12, 0], [11.7, 13, 4.3]),
    ].map(resolveUvs) },
    wall_corner_bed: { slot: 'embers', elements: [bedCube([5, 5, 5], [11, 5.5, 11])].map(resolveUvs) },
    wall_corner_bed_front: { slot: 'embers', elements: [bedCube([5, 5, 0], [11, 5.5, 5])].map(resolveUvs) },
  };
  for (const s of ['west', 'east']) {
    const [b0, b1] = mirror(s, 0, 5);
    parts[`wall_corner_arm_${s}`] = { slot: 'body', elements: arm(s).map(resolveUvs) };
    parts[`wall_corner_end_${s}`] = { slot: 'body', elements: end(s).map(resolveUvs) };
    parts[`wall_corner_bed_arm_${s}`] = { slot: 'embers', elements: [bedCube([b0, 5, 5], [b1, 5.5, 11])].map(resolveUvs) };
  }
  return { ...parts, ...wallOuterParts(arm) };
}

/**
 * Outside corner (owner's review of the mockup, 2026-10-25), drawn like the inside corner and turned to face
 * its back. Its outer face runs along x = 4 (or 12) from the arm forward round to z = 12 and on along the
 * side arm. Thinner 1 px slats stand every 5 px along it, from the last slat of one run to the first of
 * the other, so the windows are even round the bend and more of the fire shows; the one on the bend is a
 * corner post. Tips lean out over each slat, and two over the post, one out from each face of the turn
 * (owner's review: simpler than a diagonal finial, in the same design language). The inner face, toward the
 * pillar's edge, stays open like the bridges. Which side the run comes in from moves every slat, so the
 * closed side ("bend") and the side arm are drawn per side; the junction and the arm forward are shared.
 */
function wallOuterParts(arm) {
  const mirror = wallMirror;
  const thinSlat = (x0, x1, z0, z1) => slatCube([x0, 7, z0], [x1, 13, z1]);
  // Tips over the north side of a floor brazier, turned to lean out west (3 turns), east (1) or south (2)
  const tipOut = (a, turns) => turnElement(tip(a, 13.7, 3.3, 5.3), turns);
  const bend = (s) => {
    const west = s === 'west';
    const [w0, w1] = mirror(s, 4, 5), [s0, s1] = mirror(s, 4, 4.3), [r0, r1] = mirror(s, 3.5, 4), [p0, p1] = mirror(s, 4, 5);
    const [b0, b1] = mirror(s, 8.5, 9.5);
    return [
      atlasCube([w0, 5, 5], [w1, 7, 11], REGION.basin),
      atlasCube([r0, 11, 4], [r1, 13, 12.5], REGION.rim),
      thinSlat(s0, s1, 1.5, 2.5), thinSlat(s0, s1, 6.5, 7.5),
      slatCube([p0, 7, 11], [p1, 13, 12]),
      thinSlat(b0, b1, 11.7, 12),
      // Over each slat, and over the corner post one leaning out each way, meeting at the rim's outer corner
      ...(west ? [tipOut(13, 3), tipOut(8, 3), tipOut(3.5, 3), tipOut(10.5, 2), tipOut(6, 2)]
        : [tipOut(1, 1), tipOut(6, 1), tipOut(10.5, 1), tipOut(3.5, 2), tipOut(8, 2)]),
    ];
  };
  const parts = {
    wall_outer_core: { slot: 'body', elements: [
      atlasCube([4, 4, 4], [12, 5, 12], REGION.basin),
      atlasCube([4, 5, 11], [12, 7, 12], REGION.basin),
      atlasCube([4, 11, 12], [12, 13, 12.5], REGION.rim),
      grateCube([4.3, 12, 4.3], [11.7, 13, 11.7]),
    ].map(resolveUvs) },
  };
  for (const s of ['west', 'east']) {
    const [a0, a1] = mirror(s, 1.5, 2.5);
    parts[`wall_outer_bend_${s}`] = { slot: 'body', elements: bend(s).map(resolveUvs) };
    parts[`wall_outer_arm_${s}`] = { slot: 'body', elements: [...arm(s), thinSlat(a0, a1, 11.7, 12),
      tipOut(s === 'west' ? 13 : 1, 2)].map(resolveUvs) };
  }
  return parts;
}

/**
 * The Copper Campfire's flame: crossed planes the size of vanilla's campfire flame, which its model can't
 * tint, so the Copper Campfire hides that one and draws this.
 */
function campfireParts() {
  return { campfire_fire: { slot: 'fire', elements: crossedFire(8, 8, 1, 17, 7.2, true) } };
}

/** The crying brazier's slat arrangements; each crying brazier picks one by its position. */
const CRYING_ARRANGEMENTS = ['crying_a', 'crying_b', 'crying_c'];
/** How far each arrangement steps through the strips from one slat to the next; each is coprime to 8. */
const STEPS = [3, 5, 7];

/**
 * The parts with slats, once per crying arrangement, each slat moved onto its own strip of the tears
 * texture (#tears) and read from the bottom of the strip. Consecutive slats of a part step through the
 * strips by a step coprime to 8, so the four slats of a side all differ; parts and arrangements start at
 * different strips and arrangements step differently.
 */
function cryingParts(parts) {
  const out = {};
  const withSlats = Object.entries(parts).filter(([, part]) => part.elements.some((e) => e.slat));
  CRYING_ARRANGEMENTS.forEach((arrangement, r) => withSlats.forEach(([name, part], p) => {
    let j = 0;
    const elements = part.elements.map((e) => {
      if (!e.slat) return e;
      const strip = (p * 5 + r * 2 + STEPS[r] * j++) % SLAT_STAINS.length;
      const c = structuredClone(e);
      for (const face of Object.values(c.faces)) {
        const [u0, v0, u1, v1] = face.uv;
        face.uv = [round(u0 + 2 * strip), round(16 - (v1 - v0)), round(u1 + 2 * strip), 16];
        face.texture = '#tears';
      }
      return c;
    });
    out[`${arrangement}/${name}`] = { ...part, elements };
  }));
  return out;
}

/**
 * Crying bars: the obsidian bars' blockstate and models, with each part a choice of the four stain versions.
 * The game reseeds every part of a multipart model alike, so a block shows one version throughout.
 */
function writeCryingBars() {
  const read = (p) => JSON.parse(fs.readFileSync(path.join(ASSETS, p), 'utf8'));
  const state = read('blockstates/obsidian_bars.json');
  const versions = BARS_STAINS.map((_, v) => v);
  for (const part of state.multipart) {
    const apply = part.apply;
    part.apply = versions.map((v) => ({ ...apply, model: apply.model.replace('block/obsidian_bars', 'block/crying_obsidian_bars') + `_${v}` }));
  }
  writeJson(path.join(ASSETS, 'blockstates/crying_obsidian_bars.json'), state);
  for (const file of fs.readdirSync(path.join(ASSETS, 'models/block')).filter((n) => /^obsidian_bars_.*\.json$/.test(n))) {
    const model = read(`models/block/${file}`);
    for (const v of versions) {
      const textures = Object.fromEntries(Object.entries(model.textures).map(([k, t]) => [k, t.replace('block/obsidian_bars', `block/crying_obsidian_bars_${v}`)]));
      writeJson(path.join(ASSETS, 'models/block', file.replace('obsidian_bars', 'crying_obsidian_bars').replace('.json', `_${v}.json`)), { ...model, textures });
    }
  }
  const item = read('models/item/obsidian_bars.json');
  item.textures = Object.fromEntries(Object.entries(item.textures).map(([k, t]) => [k, t.replace('block/obsidian_bars', 'block/crying_obsidian_bars_0')]));
  writeJson(path.join(ASSETS, 'models/item/crying_obsidian_bars.json'), item);
}

function writeModels(parts) {
  for (const [name, part] of Object.entries(parts)) {
    // Thin cage parts read better without ambient occlusion, as vanilla's bars do.
    const elements = part.elements.map(({ slat, ...e }) => e);
    const model = { parent: 'minecraft:block/block', textures: { particle: `#${part.slot}` }, elements };
    if (part.slot === 'body') model.ambientocclusion = false;
    writeJson(path.join(ASSETS, 'models/block/brazier', name + '.json'), model);
  }
}

/** Whole-brazier models with concrete textures, for rendering states in Blockbench. */
function writePreviews(parts, pack) {
  const textures = (stage, embers, fire) => ({ particle: `mcbraziers:block/${stage}`, body: `mcbraziers:block/${stage}`,
    embers: `mcbraziers:block/${embers}`, fire });
  const compose = (names, offset = 0) => names.flatMap((n) => parts[n].elements.map((e) => {
    const { slat, ...c } = structuredClone(e);
    c.from[0] += offset; c.to[0] += offset;
    if (c.rotation) c.rotation.origin[0] += offset;
    return c;
  }));
  const sides = DIRS.map((d) => `floor_side_${d}`);
  const lit = ['floor_bed', 'floor_fire'];
  const states = {
    preview_open_lit: { stage: 'copper_brazier', embers: 'brazier_embers_lit', fire: 'minecraft:block/campfire_fire', names: ['floor_base', ...sides, ...lit] },
    preview_closed_soul: { stage: 'weathered_copper_brazier', embers: 'brazier_soul_embers_lit', fire: 'minecraft:block/soul_campfire_fire', names: ['floor_base', ...sides, ...lit, 'floor_grate'] },
    preview_unlit: { stage: 'oxidized_copper_brazier', embers: 'brazier_embers', fire: 'minecraft:block/campfire_fire', names: ['floor_base', ...sides, 'floor_bed'] },
  };
  for (const [name, s] of Object.entries(states)) {
    writeJson(path.join(pack, 'assets/mcbraziers/models/block', name + '.json'), { parent: 'minecraft:block/block', textures: textures(s.stage, s.embers, s.fire), elements: compose(s.names) });
  }
  // Two closed braziers joined east-west: the west one's east side bridges, and the east one's west side.
  const joined = (open) => ['floor_base', 'floor_side_north', 'floor_side_south', ...(open === 'east' ? ['floor_side_west'] : ['floor_side_east']),
    `floor_bridge_${open}`, `floor_bridge_ccw_${open}`, `floor_bridge_cw_${open}`, 'floor_bed', `floor_bed_bridge_${open}`, 'floor_fire', 'floor_grate', `floor_grate_bridge_${open}`];
  writeJson(path.join(pack, 'assets/mcbraziers/models/block/preview_fire_pit.json'), { parent: 'minecraft:block/block',
    textures: textures('copper_brazier', 'brazier_embers_lit', 'minecraft:block/campfire_fire'), elements: [...compose(joined('east')), ...compose(joined('west'), 16)] });
}

// Shared with the art mockup scripts, which import these instead of running the generator.
export { atlasCube, bedCube, grateCube, turnElement, resolveUvs, writeJson, REGION, DIRS, floorParts, wallParts, wallCornerParts, ceilingParts };

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  writeTextures();
  writeAtlases();
  writeCryingBars();
  // Crying arrangements number the slatted parts in this order, so later additions go last and the stains
  // already in the world stay put.
  const parts = { ...floorParts(), ...wallParts(), ...ceilingParts(), ...campfireParts(), ...wallCornerParts() };
  const crying = cryingParts(parts);
  writeModels({ ...parts, ...crying });
  if (PREVIEW) writePreviews(parts, PREVIEW);
  const textures = Object.keys(TIER_TEXTURES).length + Object.keys(BARS).length + 4 + BARS_STAINS.length + Object.keys(EMBER_GLOWS).length;
  console.log(`brazier art: ${textures} textures, ${Object.keys(parts).length} part models, ${Object.keys(crying).length} crying part models${PREVIEW ? ', previews in ' + PREVIEW : ''}`);
}

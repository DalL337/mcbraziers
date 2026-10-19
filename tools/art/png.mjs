// Minimal PNG toolkit for the art scripts: decode (8-bit and sub-byte palette/gray, non-interlaced),
// encode RGBA, crop, nearest-neighbour scale, blit and contact sheets. No dependencies.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const CRC = (() => { const t = new Uint32Array(256); for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; })();
const crc32 = (buf) => { let c = 0xffffffff; for (const b of buf) c = CRC[(c ^ b) & 0xff] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td)); return Buffer.concat([len, td, crc]); };

/** Image: { w, h, px: Uint8Array RGBA } */
export function blank(w, h, rgba = [0, 0, 0, 0]) {
  const px = new Uint8Array(w * h * 4);
  for (let i = 0; i < w * h; i++) px.set(rgba, i * 4);
  return { w, h, px };
}

export function decode(file) {
  const buf = fs.readFileSync(file);
  let pos = 8, w, h, depth, ctype, palette, trns, idat = [];
  while (pos < buf.length) {
    const len = buf.readUInt32BE(pos); const type = buf.toString('ascii', pos + 4, pos + 8); const data = buf.subarray(pos + 8, pos + 8 + len);
    if (type === 'IHDR') { w = data.readUInt32BE(0); h = data.readUInt32BE(4); depth = data[8]; ctype = data[9]; if (data[12]) throw new Error('interlaced PNG not supported: ' + file); }
    else if (type === 'PLTE') palette = data;
    else if (type === 'tRNS') trns = data;
    else if (type === 'IDAT') idat.push(data);
    pos += 12 + len;
  }
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[ctype];
  if (depth !== 8 && !(depth < 8 && channels === 1)) throw new Error(`bit depth ${depth} not supported: ${file}`);
  const raw = zlib.inflateSync(Buffer.concat(idat));
  const bpp = Math.max(1, channels * depth / 8), stride = Math.ceil(w * channels * depth / 8), out = new Uint8Array(w * h * channels);
  let prev = new Uint8Array(stride);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)], line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)), cur = new Uint8Array(stride);
    for (let x = 0; x < stride; x++) {
      const a = x >= bpp ? cur[x - bpp] : 0, b = prev[x], c = x >= bpp ? prev[x - bpp] : 0;
      let v = line[x];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      cur[x] = v & 0xff;
    }
    if (depth === 8) out.set(cur, y * stride);
    else for (let x = 0; x < w; x++) {
      const bit = x * depth, v = (cur[bit >> 3] >> (8 - depth - (bit & 7))) & ((1 << depth) - 1);
      out[y * w + x] = ctype === 0 ? Math.round(v * 255 / ((1 << depth) - 1)) : v;
    }
    prev = cur;
  }
  const img = blank(w, h);
  for (let i = 0; i < w * h; i++) {
    let r, g, b, al = 255;
    if (ctype === 6) [r, g, b, al] = out.subarray(i * 4, i * 4 + 4);
    else if (ctype === 2) [r, g, b] = out.subarray(i * 3, i * 3 + 3);
    else if (ctype === 0) r = g = b = out[i];
    else if (ctype === 4) { r = g = b = out[i * 2]; al = out[i * 2 + 1]; }
    else if (ctype === 3) { const k = out[i]; r = palette[k * 3]; g = palette[k * 3 + 1]; b = palette[k * 3 + 2]; al = trns && k < trns.length ? trns[k] : 255; }
    img.px.set([r, g, b, al], i * 4);
  }
  return img;
}

export function encode(img) {
  const raw = Buffer.alloc((img.w * 4 + 1) * img.h);
  for (let y = 0; y < img.h; y++) { raw[y * (img.w * 4 + 1)] = 0; Buffer.from(img.px.buffer, img.px.byteOffset + y * img.w * 4, img.w * 4).copy(raw, y * (img.w * 4 + 1) + 1); }
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(img.w, 0); ihdr.writeUInt32BE(img.h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

export function save(file, img) { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, encode(img)); }

export const get = (img, x, y) => Array.from(img.px.subarray((y * img.w + x) * 4, (y * img.w + x) * 4 + 4));
export const set = (img, x, y, rgba) => { if (x >= 0 && y >= 0 && x < img.w && y < img.h) img.px.set(rgba.length === 3 ? [...rgba, 255] : rgba, (y * img.w + x) * 4); };

/** Crop a frame (for animated strips) */
export function crop(img, x0, y0, w, h) { const o = blank(w, h); for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) set(o, x, y, get(img, x0 + x, y0 + y)); return o; }

export function scale(img, k) {
  const o = blank(img.w * k, img.h * k);
  for (let y = 0; y < o.h; y++) for (let x = 0; x < o.w; x++) o.px.set(img.px.subarray(((y / k | 0) * img.w + (x / k | 0)) * 4, ((y / k | 0) * img.w + (x / k | 0)) * 4 + 4), (y * o.w + x) * 4);
  return o;
}

/** Alpha-composite src onto dst at (dx, dy) over a background. */
export function blit(dst, src, dx, dy) {
  for (let y = 0; y < src.h; y++) for (let x = 0; x < src.w; x++) {
    const [r, g, b, a] = get(src, x, y); if (!a) continue;
    const X = dx + x, Y = dy + y; if (X < 0 || Y < 0 || X >= dst.w || Y >= dst.h) continue;
    const [R, G, B] = get(dst, X, Y), t = a / 255;
    set(dst, X, Y, [Math.round(r * t + R * (1 - t)), Math.round(g * t + G * (1 - t)), Math.round(b * t + B * (1 - t)), 255]);
  }
}

/** Grid of images scaled by k on a checker background, cols wide. */
export function sheet(images, k = 8, cols = 4, gap = 8, bg = [40, 44, 52, 255]) {
  const cw = Math.max(...images.map(i => i.w)) * k, ch = Math.max(...images.map(i => i.h)) * k;
  const rows = Math.ceil(images.length / cols);
  const o = blank(cols * (cw + gap) + gap, rows * (ch + gap) + gap, bg);
  images.forEach((img, n) => {
    const ox = gap + (n % cols) * (cw + gap), oy = gap + Math.floor(n / cols) * (ch + gap);
    for (let y = 0; y < img.h * k; y++) for (let x = 0; x < img.w * k; x++) set(o, ox + x, oy + y, ((x >> 3) + (y >> 3)) & 1 ? [70, 74, 82, 255] : [58, 62, 70, 255]);
    blit(o, scale(img, k), ox, oy);
  });
  return o;
}

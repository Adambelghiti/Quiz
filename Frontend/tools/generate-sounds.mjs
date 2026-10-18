#!/usr/bin/env node
/**
 * Generates the short UI sounds used by the quiz (correct / wrong / tick / win).
 *
 * The files are plain 16-bit mono PCM WAVs written into ../public/sounds, so the
 * Angular assets pipeline serves them at /sounds/*.wav and the app can play them
 * with standard HTML5 Audio. No third-party audio assets or licences involved.
 *
 * Run from the Frontend directory:
 *   node tools/generate-sounds.mjs
 */
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const SAMPLE_RATE = 22050;
const OUT_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '..', 'public', 'sounds');

mkdirSync(OUT_DIR, { recursive: true });

function writeWav(name, samples) {
  const dataSize = samples.length * 2;
  const buffer = Buffer.alloc(44 + dataSize);
  buffer.write('RIFF', 0);
  buffer.writeUInt32LE(36 + dataSize, 4);
  buffer.write('WAVE', 8);
  buffer.write('fmt ', 12);
  buffer.writeUInt32LE(16, 16); // fmt chunk size
  buffer.writeUInt16LE(1, 20); // PCM
  buffer.writeUInt16LE(1, 22); // mono
  buffer.writeUInt32LE(SAMPLE_RATE, 24);
  buffer.writeUInt32LE(SAMPLE_RATE * 2, 28); // byte rate
  buffer.writeUInt16LE(2, 32); // block align
  buffer.writeUInt16LE(16, 34); // bits per sample
  buffer.write('data', 36);
  buffer.writeUInt32LE(dataSize, 40);

  for (let i = 0; i < samples.length; i++) {
    const clamped = Math.max(-1, Math.min(1, samples[i]));
    buffer.writeInt16LE(Math.round(clamped * 32767), 44 + i * 2);
  }

  writeFileSync(join(OUT_DIR, name), buffer);
  console.log(`${name}: ${(buffer.length / 1024).toFixed(1)} KiB`);
}

function frames(ms) {
  return Math.round((ms / 1000) * SAMPLE_RATE);
}

/** A sine tone with a short attack and exponential decay (no clicks). */
function tone(frequency, ms, { gain = 0.5, attackMs = 4, decay = 5, harmonics = [1] } = {}) {
  const total = frames(ms);
  const attack = Math.max(1, frames(attackMs));
  const out = new Float32Array(total);

  for (let i = 0; i < total; i++) {
    const t = i / SAMPLE_RATE;
    let sample = 0;
    harmonics.forEach((amplitude, index) => {
      sample += amplitude * Math.sin(2 * Math.PI * frequency * (index + 1) * t);
    });
    const envelope = i < attack ? i / attack : Math.exp((-decay * (i - attack)) / total);
    out[i] = sample * gain * envelope;
  }

  return out;
}

function concat(...parts) {
  const total = parts.reduce((sum, part) => sum + part.length, 0);
  const out = new Float32Array(total);
  let offset = 0;
  for (const part of parts) {
    out.set(part, offset);
    offset += part.length;
  }
  return out;
}

// Bright rising pair for a correct answer.
writeWav('correct.wav', concat(
  tone(587.33, 95, { gain: 0.45 }),
  tone(880.0, 170, { gain: 0.5, decay: 6 }),
));

// Low descending buzz for a wrong answer.
writeWav('wrong.wav', concat(
  tone(233.08, 120, { gain: 0.4, harmonics: [1, 0.35] }),
  tone(174.61, 220, { gain: 0.45, decay: 4, harmonics: [1, 0.35] }),
));

// A short click for the last-five-seconds countdown.
writeWav('tick.wav', tone(1200, 55, { gain: 0.35, attackMs: 1, decay: 14 }));

// A small fanfare when the game is won.
writeWav('win.wav', concat(
  tone(523.25, 110, { gain: 0.4 }),
  tone(659.25, 110, { gain: 0.42 }),
  tone(783.99, 110, { gain: 0.45 }),
  tone(1046.5, 320, { gain: 0.5, decay: 3 }),
));

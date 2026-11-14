const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../common/src/main/resources/assets/vs_clockwork');
const models = path.join(root, 'models/block/combobulator');
const texture = 'vs_clockwork:block/combobulator';
const read = name => JSON.parse(fs.readFileSync(path.join(models, name + '.json'), 'utf8').replace(/^\uFEFF/, ''));
const write = (name, value) => fs.writeFileSync(path.join(models, name + '.json'), JSON.stringify(value, null, 2) + '\n');
const vectors = {west: [-1,0,0], east: [1,0,0], down: [0,-1,0], up: [0,1,0], north: [0,0,-1], south: [0,0,1]};
const names = Object.keys(vectors);
const same = (a, b) => a.every((n, i) => n === b[i]);
const opposite = (a, b) => vectors[a].some((n, i) => n !== 0 && n === -vectors[b][i]);
const permutations = [[0,1,2],[0,2,1],[1,0,2],[1,2,0],[2,0,1],[2,1,0]];
const rotations = [];
for (const p of permutations) for (const x of [-1,1]) for (const y of [-1,1]) for (const z of [-1,1]) {
  const inversions = p.flatMap((v, i) => p.slice(i + 1).map(w => v > w ? 1 : 0)).reduce((a,b) => a+b, 0);
  if ((inversions % 2 ? -1 : 1) * x*y*z === 1) rotations.push({p, s:[x,y,z]});
}
const vector = (v, r) => r.p.map((axis, i) => v[axis] * r.s[i]);
const point = (v, r) => vector(v.map(n => n-8), r).map(n => n+8);
const faceName = v => names.find(n => same(v, vectors[n]));
function vertices(face, a, b) {
  const [x,y,z] = a, [X,Y,Z] = b;
  return {
    north: [[X,Y,z],[X,y,z],[x,y,z],[x,Y,z]], south: [[x,Y,Z],[x,y,Z],[X,y,Z],[X,Y,Z]],
    west: [[x,Y,z],[x,y,z],[x,y,Z],[x,Y,Z]], east: [[X,Y,Z],[X,y,Z],[X,y,z],[X,Y,z]],
    up: [[x,Y,z],[x,Y,Z],[X,Y,Z],[X,Y,z]], down: [[x,y,Z],[x,y,z],[X,y,z],[X,y,Z]]
  }[face];
}
function rotate(element, r) {
  assert(!element.rotation || element.rotation.angle === 0);
  const a = point(element.from, r), b = point(element.to, r);
  const result = {...structuredClone(element), from: a.map((n,i) => Math.min(n,b[i])), to: a.map((n,i) => Math.max(n,b[i])), faces:{}};
  delete result.rotation;
  for (const [face, data] of Object.entries(element.faces)) {
    const target = faceName(vector(vectors[face], r));
    const first = point(vertices(face, element.from, element.to)[0], r);
    const index = vertices(target, result.from, result.to).findIndex(v => same(v, first));
    assert(index >= 0);
    result.faces[target] = {...data, rotation: ((data.rotation || 0) - index * 90 + 360) % 360};
    if (data.cullface) result.faces[target].cullface = faceName(vector(vectors[data.cullface], r));
  }
  return result;
}
const model = elements => ({textures:{'6':texture,particle:texture}, elements});
const base = read('base'), extension = read('extension'), edge = read('edge'), corner = read('corner');
const multipart = [{apply:{model: texture + '/base'}}];
const framed = [...base.elements];
const add = (name, elements, when) => {
  write(name, model(elements));
  multipart.push({when,apply:{model: texture + '/' + name}});
};
for (const direction of names) {
  const r = rotations.find(r => faceName(vector(vectors.up, r)) === direction);
  add('extension_' + direction, extension.elements.map(e => rotate(e, r)), {[direction]:'true'});
}
const pairs = names.flatMap((a,i) => names.slice(i+1).filter(b => !opposite(a,b)).map(b => [a,b]));
const bounds = directions => {
  const from = [2,2,2], to = [14,14,14];
  for (const d of directions) {
    const axis = vectors[d].findIndex(n => n !== 0);
    from[axis] = vectors[d][axis] < 0 ? 0 : 14;
    to[axis] = from[axis] + 2;
  }
  return {from,to};
};
for (const pair of pairs) {
  const r = rotations.find(r => ['up','north'].every(n => pair.includes(faceName(vector(vectors[n], r)))));
  const elements = edge.elements.map(e => rotate(e,r));
  add('edge_' + pair.join('_'), elements, Object.fromEntries(pair.map(n => [n,'false'])));
  framed.push(...elements);
  add('fill_' + pair.join('_'), [{...structuredClone(base.elements[0]),...bounds(pair)}], Object.fromEntries(pair.map(n => [n,'true'])));
}
for (const x of ['west','east']) for (const y of ['down','up']) for (const z of ['north','south']) {
  const triple = [x,y,z];
  const r = rotations.find(r => ['west','up','north'].every(n => triple.includes(faceName(vector(vectors[n], r)))));
  const elements = corner.elements.map(e => rotate(e,r));
  const conditions = [[x,y],[x,z],[y,z]].map(pair => Object.fromEntries(pair.map(n => [n,'false'])));
  add('corner_' + triple.join('_'), elements, {OR:conditions});
  framed.push(...elements);
  add('fill_' + triple.join('_'), [{...structuredClone(base.elements[0]),...bounds(triple)}], Object.fromEntries(triple.map(n => [n,'true'])));
}
write('empty', model([]));
write('relay', {...model(framed),parent:'minecraft:block/block'});
fs.writeFileSync(path.join(root,'blockstates/thinkable_relay.json'), JSON.stringify({multipart},null,2) + '\n');
assert(multipart.every(part => !Array.isArray(part.apply) && !part.apply.model.includes('/face_')));
for (let i=1;i<=4;i++) {
  const face = read('face_' + i);
  for (const element of face.elements) if (element.light_emission > 0) {
    element.forge_data = {block_light:element.light_emission, sky_light:element.light_emission, ambient_occlusion:false};
  }
  write('face_' + i, face);
}
const orientations = {north:{},east:{y:90},south:{y:180},west:{y:270},up:{x:270},down:{x:90}};
for (let i=0;i<names.length;i++) {
  const direction = names[i];
  const choices = ['empty','face_1','face_2','face_3','face_4'];
  const ordered = choices.slice(i%5).concat(choices.slice(0,i%5));
  multipart.push({when:{[direction]:'false'},apply:ordered.map(name => ({
    model:texture + '/' + name,...orientations[direction],weight:name === 'empty' ? 12 : 2
  }))});
}
write('block', {...model([...framed,...read('face_3').elements]),parent:'minecraft:block/block'});
fs.writeFileSync(path.join(root,'blockstates/combobulator.json'), JSON.stringify({multipart},null,2) + '\n');
assert.equal(pairs.length,12);
assert.equal(framed.length,21);
for (let mask=0;mask<64;mask++) {
  const state = Object.fromEntries(names.map((n,i) => [n,Boolean(mask & (1<<i))]));
  const matches = condition => !condition || (condition.OR ? condition.OR.some(matches) : Object.entries(condition).every(([k,v]) => state[k] === (v === 'true')));
  const selected = multipart.filter(part => matches(part.when));
  const extensions = selected.filter(p => !Array.isArray(p.apply) && p.apply.model.includes('/extension_'));
  assert.equal(extensions.length, Object.values(state).filter(Boolean).length);
  assert.equal(selected.filter(p => Array.isArray(p.apply)).length, 6 - extensions.length);
  for (const part of selected) for (const application of [part.apply].flat()) {
    assert(fs.existsSync(path.join(models,application.model.split('/').at(-1)+'.json')));
  }
}
console.log('Generated and checked all 64 connection layouts, 12 frame edges, 8 frame corners, and six decorative sides.');

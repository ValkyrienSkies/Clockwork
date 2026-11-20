const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '../common/src/main/resources/assets/vs_clockwork');
const directory = path.join(root, 'models/item/gatemakyr');
const source = JSON.parse(fs.readFileSync(path.join(directory, 'gatemakyr_compiled.bbmodel'), 'utf8'));
const elements = new Map(source.elements.map(element => [element.uuid, element]));
const groups = new Map(source.groups.map(group => [group.uuid, group]));
const textures = Object.fromEntries(source.textures.map(texture => [texture.id, 'vs_clockwork:item/' + path.parse(texture.name).name]));
textures.particle = textures[source.textures.find(texture => texture.particle).id];
const display = Object.fromEntries(Object.entries(source.display).filter(([slot]) => slot !== 'on_shelf'));
const round = number => Math.round(number * 1e8) / 1e8;
const write = (name, value) => fs.writeFileSync(path.join(directory, name + '.json'), JSON.stringify(value, null, 2) + '\n');

assert.equal(source.meta.model_format, 'java_block');
for (const group of source.groups) {
    assert(group.rotation.every(angle => angle === 0), 'Rotated group: ' + group.name);
    assert.notEqual(group.export, false);
}
for (const texture of source.textures) {
    const png = fs.readFileSync(path.join(root, 'textures/item', texture.name));
    assert.equal(png.subarray(1, 4).toString(), 'PNG');
    assert.equal(png.readUInt32BE(16), texture.width);
    assert.equal(png.readUInt32BE(20), texture.height);
    assert(png.equals(Buffer.from(texture.source.split(',')[1], 'base64')), 'Texture differs from embedded source: ' + texture.name);
}

function convert(cube) {
    assert.equal(cube.type, 'cube');
    assert.notEqual(cube.export, false);
    const rotation = cube.rotation || [0, 0, 0];
    assert(rotation.filter(angle => angle !== 0).length <= 1);
    const axis = Math.max(0, rotation.findIndex(angle => angle !== 0));
    assert([-45, -22.5, 0, 22.5, 45].includes(rotation[axis]));
    const element = {
        name: cube.name,
        from: cube.from.map(value => round(value - (cube.inflate || 0))),
        to: cube.to.map(value => round(value + (cube.inflate || 0))),
        rotation: {angle: rotation[axis], axis: 'xyz'[axis], origin: cube.origin},
        faces: {}
    };
    assert([...element.from, ...element.to].every(value => Number.isFinite(value) && value >= -16 && value <= 32));
    if (cube.rescale) element.rotation.rescale = true;
    if (cube.shade === false) element.shade = false;
    assert(!cube.light_emission, 'Light emission requires a newer Minecraft model format');
    for (const [direction, face] of Object.entries(cube.faces)) {
        if (face.texture === null) continue;
        assert.notEqual(face.enabled, false);
        const texture = source.textures[face.texture];
        assert(texture, 'Missing texture on ' + cube.name);
        const converted = {
            uv: face.uv.map((value, index) => round(value * 16 / (index % 2 ? source.resolution.height : source.resolution.width))),
            texture: '#' + texture.id
        };
        if (face.rotation) converted.rotation = face.rotation;
        if (face.cullface) converted.cullface = face.cullface;
        if (face.tint >= 0) converted.tintindex = face.tint;
        element.faces[direction] = converted;
    }
    return element;
}

function flatten(node) {
    return typeof node === 'string' ? [node] : node.children.flatMap(flatten);
}

function model(ids) {
    const indices = new Map(ids.map((id, index) => [id, index]));
    function retain(node) {
        if (typeof node === 'string') return indices.get(node);
        const children = node.children.map(retain).filter(child => child !== undefined);
        if (!children.length) return undefined;
        const group = groups.get(node.uuid);
        return {name: group.name, origin: group.origin, color: group.color, children};
    }
    return {
        ...(source.ambientocclusion === false ? {ambientocclusion: false} : {}),
        ...(source.front_gui_light ? {gui_light: 'front'} : {}),
        textures,
        elements: ids.map(id => convert(elements.get(id))),
        display,
        groups: source.outliner.map(retain).filter(group => group !== undefined)
    };
}

const body = source.outliner.find(node => groups.get(node.uuid).name === 'Gatemakyr');
const prongs = source.outliner.find(node => groups.get(node.uuid).name === 'prongs');
const parts = {gatemakyrbase: flatten(body)};
for (const prong of prongs.children) {
    const name = groups.get(prong.uuid).name;
    const arm = prong.children.find(node => typeof node !== 'string');
    assert.equal(groups.get(arm.uuid).name, 'prongarm');
    const direct = prong.children.filter(node => typeof node === 'string');
    parts[name + 'base'] = direct.filter(id => elements.get(id).name === 'prongbase');
    parts[name + 'arm'] = flatten(arm);
    parts[name + 'tip'] = direct.filter(id => elements.get(id).name === 'prongtip');
    assert.equal(parts[name + 'base'].length, 1);
    assert.equal(parts[name + 'arm'].length, 2);
    assert.equal(parts[name + 'tip'].length, 1);
}
const assigned = Object.values(parts).flat();
assert.equal(Object.keys(parts).length, 13);
assert.equal(new Set(assigned).size, assigned.length);
assert.deepEqual([...assigned].sort(), [...elements.keys()].sort());
write('gatemakyrbase', model(parts.gatemakyrbase));
for (const part of ['base', 'arm', 'tip']) write('prong' + part, model(parts['prongtop' + part]));
write('gatemakyrcompiled', model(source.outliner.flatMap(flatten)));
fs.writeFileSync(path.join(directory, '../gatemakyr.json'), JSON.stringify({parent: 'vs_clockwork:item/gatemakyr/gatemakyrbase'}, null, 2) + '\n');
console.log('Exported the body and 3 reusable prong parts; all ' + assigned.length + ' source cubes retained in the assembled model.');

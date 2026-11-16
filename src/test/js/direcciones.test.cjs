const assert = require('node:assert/strict');
const {test} = require('node:test');
const {readFileSync} = require('node:fs');
const vm = require('node:vm');
const source = readFileSync('src/main/resources/static/js/incidentes.js', 'utf8');
const code = source.slice(source.indexOf('    const configurarDireccion'), source.indexOf('    const fillReportDateTime'));
class Select {
    constructor(options, value) { this.options = options; this.value = value; this.events = {}; }
    replaceChildren(...options) { this.options = options; }
    addEventListener(event, callback) { this.events[event] = callback; }
    change(value) { this.value = value; this.events.change(); }
}
const option = (value, dataset = {}) => ({value, dataset});
function setup(prefix, cantonValue, districtValue) {
    const elements = {
        [prefix + 'Provincia']: new Select([option('1'), option('2')], '2'),
        [prefix + 'Canton']: new Select([option('10', {provincia:'1'}), option('20', {provincia:'2'})], cantonValue),
        [prefix + 'Distrito']: new Select([option('100', {canton:'10'}), option('200', {canton:'20'})], districtValue)
    };
    vm.runInNewContext(code, {
        document: {getElementById: id => elements[id]},
        Option: function(text, value) { this.text = text; this.value = value; }
    });
    return [elements[prefix+'Provincia'], elements[prefix+'Canton'], elements[prefix+'Distrito']];
}
for (const prefix of ['cliente', 'incidente']) {
    test(prefix + ': elimina primera opcion ajena y seleccion inconsistente', () => {
        const [p,c,d] = setup(prefix, '10', '100');
        assert.deepEqual(c.options.map(o=>o.value), ['', '20']);
        assert.equal(c.value, '');
        assert.equal(d.value, '');
        assert.equal(d.disabled, true);
    });
    test(prefix + ': conserva direccion valida y reinicia dependencias al cambiar', () => {
        const [p,c,d] = setup(prefix, '20', '200');
        assert.equal(c.value, '20');
        assert.equal(d.value, '200');
        p.change('1');
        assert.equal(c.value, '');
        assert.equal(d.value, '');
        assert.deepEqual(c.options.map(o=>o.value), ['', '10']);
        c.change('10');
        assert.deepEqual(d.options.map(o=>o.value), ['', '100']);
        assert.equal(d.disabled, false);
        p.change('');
        assert.equal(c.disabled, true);
        assert.equal(d.disabled, true);
        assert.deepEqual(d.options.map(o=>o.value), ['']);
    });
}

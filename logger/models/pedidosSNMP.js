var mongoose = require('mongoose')

var Schema = mongoose.Schema

var OidSchema = new Schema({
    oid: {type: String, required: true},
    value: {type: String, required: true}
})

var PedidoSchema = new Schema({
    id: {type: String, required: true},
    operation: {type: String, required: true},
    version: {type: String, required: true},
    communityString: {type: String, required: true},
    oids: [{type: OidSchema, required: true}]

})

var RespostaSchema = new Schema({
    vbs:[{type: OidSchema, required: true}]
})

var PedidosSchema = new Schema({
    ip: {type: String, required: true},
    port: {type: String, required: true},
    dia: {type: String, required: true},
    hora: {type: String, required: true},
    pedido: {type: PedidoSchema, required: true},
    resposta: {type: RespostaSchema, required: true}
})

module.exports = mongoose.model('Pedidos', PedidosSchema, 'pedidosSNMP')
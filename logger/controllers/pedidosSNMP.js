var Pedidos = require('../models/pedidosSNMP')
var mongoose = require('mongoose')

/////////////////////////////////////////////////////////////////
///////////////////////EXCHANGES/////////////////////////////////
/////////////////////////////////////////////////////////////////

module.exports.listar = () => { //USADO
    return Pedidos
            .find()
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.listarIP = (i) => { //USADO
    return Pedidos
            .find({ip: i})
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.listarPorta = (porta) => { //USADO
    return Pedidos
            .find({port: porta})
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.listarIPPorta = (ip, porta) => { //USADO
    return Pedidos
            .find({ip: ip, port: porta})
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.listarID = (id) => { //USADO
    console.log("TIPO DO ID " + typeof id)
    return Pedidos
            .findOne({"pedido.id": id})
            .sort({dia: -1, hora: -1})
            .exec()
}

// /////////////////////////////////////////////////////////////////
// ///////////////////////RESPOSTAS/////////////////////////////////
// /////////////////////////////////////////////////////////////////

// module.exports.listarResposta = () => { //USADO
//     return Pedidos
//             .find({}, {resposta: 1})
//             .sort({dia: -1, hora: -1})
//             .exec()
// }

// module.exports.listarRespostaIPPorta = (ip, porta) => { //USADO
//     return Pedidos
//             .find({ip: ip, port: porta}, {resposta: 1})
//             .sort({dia: -1, hora: -1})
//             .exec()
// }

// module.exports.listarRespostaID = (id) => { //USADO
//     return Pedidos
//             .find({"pedido.id": id}, {resposta: 1})
//             .sort({dia: -1, hora: -1})
//             .exec()
// }

// /////////////////////////////////////////////////////////////////
// ///////////////////////PEDIDOS///////////////////////////////////
// /////////////////////////////////////////////////////////////////

// module.exports.listarPedido = () => { //USADO
//     return Pedidos
//             .find({}, {pedido: 1})
//             .sort({dia: -1, hora: -1})
//             .exec()
// }

// module.exports.listarPedidoIPPorta = (ip, porta) => { //USADO
//     return Pedidos
//             .find({ip: ip, port: porta}, {pedido: 1})
//             .sort({dia: -1, hora: -1})
//             .exec()
// }

// module.exports.listarPedidoID = (id) => { //USADO
//     return Pedidos
//             .find({"pedido.id": id}, {pedido: 1})
//             .sort({dia: -1, hora: -1})
//             .exec()
// }

/////////////////////////////////////////////////////////////////
///////////////////////ESTATISTICAS//////////////////////////////
/////////////////////////////////////////////////////////////////

module.exports.gets = () => { //USADO
    return Pedidos
            .find({"pedido.operation": "snmpget"})
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.sets = () => { //USADO
    return Pedidos
            .find({"pedido.operation": "snmpset"})
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.listarCS = (cs) => { //USADO
    return Pedidos
            .find({"pedido.communityString": cs})
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.listarDiaHora = (d, h) => {
    return Pedidos
            .find({dia: {$gte: d}, hora: {$gte: h}})
            .sort({dia: -1, hora: -1})
            .exec()
}

module.exports.listarDia = (d) => {
    return Pedidos
            .find({dia: d})
            .sort({dia: -1, hora: -1})
            .exec()
}
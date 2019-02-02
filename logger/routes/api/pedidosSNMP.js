var express = require('express');
var router = express.Router();
var querystring = require('querystring')

var Pedidos = require('./../../controllers/pedidosSNMP')

router.get('/exchange', (req, res)=>{ //USADO DIRETAMENTE
    console.log("QUERO O EXCHANGE")

    if(req.query.ip && req.query.port){
        console.log("IP " + req.query.ip + " E PORTA " + req.query.port)
        Pedidos.listarIPPorta(req.query.ip, req.query.port)
        .then(dados =>{
            //console.log(dados)
            res.jsonp(dados)
        })
        .catch(erroListar =>{
            console.log("ERRO AO OBTER O LOG " + erroListar)
            res.status(500).send("ERRO AO OBTER O LOG " + erroListar)
        })
    }
    else{
        if(req.query.ip){
            console.log("IP " + req.query.ip)
            Pedidos.listarIP(req.query.ip)
            .then(dados =>{
                //console.log(dados)
                res.jsonp(dados)
            })
            .catch(erroListar =>{
                console.log("ERRO AO OBTER O LOG " + erroListar)
                res.status(500).send("ERRO AO OBTER O LOG " + erroListar)
            })
        }
        else{
            if(req.query.port){
                console.log("PORTA " + req.query.port)
                Pedidos.listarPorta(req.query.port)
                .then(dados =>{
                    //console.log(dados)
                    res.jsonp(dados)
                })
                .catch(erroListar =>{
                    console.log("ERRO AO OBTER O LOG " + erroListar)
                    res.status(500).send("ERRO AO OBTER O LOG " + erroListar)
                })
            }
            else{
                if (req.query.id){
                    console.log("ID " + req.query.id)
                    Pedidos.listarID(req.query.id)
                    .then(dados =>{
                        console.log(dados)
                        res.jsonp(dados)
                    })
                    .catch(erroListar =>{
                        console.log("ERRO AO OBTER O LOG POR ID" + erroListar)
                        res.status(500).send("ERRO AO OBTER O LOG POR ID " + erroListar)
                    })
                }
                else{
                    console.log("BARRA")
                    console.log("IP " + req.query.ip + " E PORTA " + req.query.port)
                    Pedidos.listar()
                    .then(dados =>{
                        //console.log(dados)
                        res.jsonp(dados)
                    })
                    .catch(erroListar =>{
                        console.log("ERRO AO OBTER O LOG " + erroListar)
                        res.status(500).send("ERRO AO OBTER O LOG " + erroListar)
                    })
                }
            }
        }
    }
})

// router.get('/resposta', (req, res) =>{

//     if(req.query.ip && req.query.port){
//         Pedidos.listarRespostaIPPorta(req.query.ip, req.query.port)
//         .then(dados =>{
//             //console.log(dados)
//             res.jsonp(dados)
//         })
//         .catch(erroIPPort =>{
//             console.log("ERRO AO OBTER OS DADOS DA RESPOTA POR IP E PORTA " + erroIPPort)
//             res.status(500).send("ERRO AO OBTER OS DADOS DA RESPOTA POR IP E PORTA " + erroIPPort)
//         })
//     }
//     else{
//         if(req.query.id){
//             Pedidos.listarRespostaID(req.query.id)
//             .then(dados =>{
//                 //console.log(dados)
//                 res.jsonp(dados)
//             })
//             .catch(erroID =>{
//                 console.log("ERRO AO OBTER OS DADOS DA RESPOTA POR ID " + erroID)
//                 res.status(500).send("ERRO AO OBTER OS DADOS DA RESPOTA POR ID " + erroID)
//             })
//         }
//         else{
//             Pedidos.listarResposta()
//             .then(dados =>{
//                 //console.log(dados)
//                 res.jsonp(dados)
//             })
//             .catch(erro =>{
//                 console.log("ERRO AO OBTER OS DADOS DA RESPOTA " + erro)
//                 res.status(500).send("ERRO AO OBTER OS DADOS DA RESPOTA " + erro)
//             })
//         }
//     }
// })

// router.get('/pedido', (req, res) =>{

//     if(req.query.ip && req.query.port){
//         Pedidos.listarPedidoIPPorta(req.query.ip, req.query.port)
//         .then(dados =>{
//             //console.log(dados)
//             res.jsonp(dados)
//         })
//         .catch(erroIPPort =>{
//             console.log("ERRO AO OBTER OS DADOS DO PEDIDO POR IP E PORTA " + erroIPPort)
//             res.status(500).send("ERRO AO OBTER OS DADOS DO PEDIDO POR IP E PORTA " + erroIPPort)
//         })
//     }
//     else{
//         if(req.query.id){
//             Pedidos.listarPedidoID(req.query.id)
//             .then(dados =>{
//                 //console.log(dados)
//                 res.jsonp(dados)
//             })
//             .catch(erroID =>{
//                 console.log("ERRO AO OBTER OS DADOS DO PEDIDO POR ID " + erroID)
//                 res.status(500).send("ERRO AO OBTER OS DADOS DO PEDIDO POR ID " + erroID)
//             })
//         }
//         else{
//             Pedidos.listarPedido()
//             .then(dados =>{
//                 //console.log(dados)
//                 res.jsonp(dados)
//             })
//             .catch(erro =>{
//                 console.log("ERRO AO OBTER OS DADOS DO PEDIDO " + erro)
//                 res.status(500).send("ERRO AO OBTER OS DADOS DO PEDIDO " + erro)
//             })
//         }
//     }
// })

router.get('/gets', (req, res) =>{ //USADO DIRETAMENTE
    Pedidos.gets()
    .then(dados =>{
        //console.log(dados)
        res.jsonp(dados)
    })
    .catch(erroGets =>{
        console.log("ERRO AO OBTER OS GETS " + erroGets)
    })
})

router.get('/sets', (req, res) =>{ //USADO DIRETAMENTE
    Pedidos.sets()
    .then(dados =>{
        //console.log(dados)
        res.jsonp(dados)
    })
    .catch(erroSets =>{
        console.log("ERRO AO OBTER OS SETS " + erroSets)
    })
})

router.get('/communityString', (req, res) =>{ //USADO DIRETAMENTE
    if(req.query.cS){
        Pedidos.listarCS(req.query.cS)
        .then(dados =>{
            //console.log(dados)
            res.jsonp(dados)
        })
        .catch(erroCS =>{
            console.log("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR COMMUNITY STRING " + erroCS)
            res.status(500).send("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR COMMUNITY STRING " + erroCS)
        })
    }
    else{
        res.status(404).send("CAMPO COMMUNITY STRING NÃO ENCONTRADO!!")
    }
})

router.get('/date', (req, res) =>{ //USADO DIRETAMENTE
    if(req.query.dia && req.query.hora){
        console.log("DIA => " + req.query.dia + " HORAS => " + req.query.hora)
        Pedidos.listarDiaHora(req.query.dia, req.query.hora)
        .then(dados =>{
            //console.log(dados)
            res.jsonp(dados)
        })
        .catch(erroDH =>{
            console.log("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR DIA E HORA " + erroDH)
            res.status(500).send("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR DIA E HORA " + erroDH)
        })
    }
    else{
        if(req.query.dia){
            Pedidos.listarDia(req.query.dia )
            .then(dados =>{
                //console.log(dados)
                res.jsonp(dados)
            })
            .catch(erroDH =>{
                console.log("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR DIA E HORA " + erroDH)
                res.status(500).send("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR DIA E HORA " + erroDH)
            })
        }
        else{
            Pedidos.listar()
                .then(dados =>{
                    //console.log(dados)
                    res.jsonp(dados)
                })
                .catch(erroListar =>{
                    console.log("ERRO AO OBTER O LOG " + erroListar)
                    res.status(500).send("ERRO AO OBTER O LOG " + erroListar)
                })
        }
    }
})

module.exports = router;
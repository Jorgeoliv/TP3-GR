var express = require('express');
var router = express.Router();
var querystring = require('querystring')
var axios = require('axios')


router.get('/', (req, res) =>{
    if (req.query.ip && req.query.port){
        axios.get('http://localhost:3000/api/exchange?ip=' + req.query.ip + "&port=" + req.query.port)
            .then(dados =>{
                console.log(dados.data)
                res.render('exchange', {info: dados.data})
            })
            .catch(erro =>{
                console.log("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR IP E PORTA " + erro)
                res.status(500).send("ERRO AO OBTER A TROCA DE INFORMAÇÃO por IP PORTA " + erro)
            })
    }
    else{
        if(req.query.ip){
            axios.get('http://localhost:3000/api/exchange?ip=' + req.query.ip)
            .then(dados =>{
                console.log(dados.data)
                res.render('exchange', {info: dados.data})
            })
            .catch(erro =>{
                console.log("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR IP" + erro)
                res.status(500).send("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR IP" + erro)
            })
        }
        else{
            if(req.query.port){
                axios.get('http://localhost:3000/api/exchange?port=' + req.query.port)
                .then(dados =>{
                    console.log(dados.data)
                    res.render('exchange', {info: dados.data})
                })
                .catch(erro =>{
                    console.log("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR PORTA " + erro)
                    res.status(500).send("ERRO AO OBTER A TROCA DE INFORMAÇÃO POR PORTA" + erro)
                })
            }
            else{
                axios.get('http://localhost:3000/api/exchange')
                .then(dados =>{
                    console.log(dados.data)
                    res.render('exchange', {info: dados.data})
                })
                .catch(erro =>{
                    console.log("ERRO AO OBTER A TROCA DE INFORMAÇÃO " + erro)
                    res.status(500).send("ERRO AO OBTER A TROCA DE INFORMAÇÃO " + erro)
                })
            }
        }
    }
})

router.get('/exchangeIndividual', (req, res) =>{
    if(req.query.id){
        axios.get('http://localhost:3000/api/exchange?id=' + req.query.id)
        .then(dados =>{
            console.log(dados.data)
            res.render('exchangeIndividual', {info: dados.data})
        })
        .catch(erroEI =>{
            console.log("ERRO AO OBTER EXCHANGE INDIVIDUAL " + erroEI)
            res.status(500).send("ERRO AO OBTER EXCHANGE INDIVIDUAL " + erroEI)
        })
    }
})

router.get('/gets', (req, res) =>{
    axios.get('http://localhost:3000/api/gets')
    .then(dados =>{
        console.log(dados.data)
        res.render('exchange', {info: dados.data})
    })
    .catch(erroGets =>{
        console.log("ERRO NO /GETS " + erroGets)
        res.status(500).send("ERRO NO /GETS " + erroGets)
    })
})

router.get('/sets', (req, res) =>{
    axios.get('http://localhost:3000/api/sets')
    .then(dados =>{
        console.log(dados.data)
        res.render('exchange', {info: dados.data})
    })
    .catch(erroSets =>{
        console.log("ERRO NO /SETS " + erroSets)
        res.status(500).send("ERRO NO /SETS " + erroSets)
    })
})

router.get('/communityString', (req, res) =>{
    if(req.query.cS){
        axios.get('http://localhost:3000/api/communityString?cS=' + req.query.cS)
        .then(dados =>{
            console.log(dados.data)
            res.render('exchange', {info: dados.data})
        })
        .catch(erroCS =>{
            console.log("ERRO NO /COMMUNITYSTRING " + erroCS)
            res.status(500).send("ERRO NO /COMMUNITYSTRING " + erroCS)
        })
    }
    else{
        console.log("ERRO NO /COMMUNITYSTRING (SEM COMMUNITY STRING) " + erroCS)
        res.status(404).send("ERRO NO /COMMUNITYSTRING (SEM COMMUNITY STRING) " + erroCS)
    }
})

router.get('/date', (req, res) =>{
    if(req.query.dia && req.query.hora){
        axios.get('http://localhost:3000/api/date?dia=' + req.query.dia + "&hora=" + req.query.hora)
        .then(dados =>{
            console.log(dados.data)
            res.render('exchange', {info: dados.data})
        })
        .catch(erroDATE =>{
            console.log("ERRO NO /DATE DIA E HORA" + erroDATE)
            res.status(500).send("ERRO NO /DATE DIA E HORA " + erroDATE)
        })
    }
    else{
        if(req.query.dia){
            axios.get('http://localhost:3000/api/date?dia=' + req.query.dia)
            .then(dados =>{
                console.log(dados.data)
                res.render('exchange', {info: dados.data})
            })
            .catch(erroDATE =>{
                console.log("ERRO NO /DATE DIA" + erroDATE)
                res.status(500).send("ERRO NO /DATE DIA " + erroDATE)
            })
        }
        else{
            console.log("ERRO NO /DATE (SEM PARAMETROS)" + erroDATE)
            res.status(500).send("ERRO NO /DATE DIA (SEM PARAMETROS)" + erroDATE)
        }
    }
})

module.exports = router;
import com.mongodb.*;
import org.snmp4j.smi.VariableBinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

public class Mongo{
    private String ip;
    private int port;
    private MongoClient mongo;
    private DB database;
    private DBCollection collection;

    public Mongo(String ip, int port){
        this.ip = ip;
        this.port = port;
        this.mongo = new MongoClient(ip, port);
        this.database = this.mongo.getDB("tp3");
        this.collection = this.database.createCollection("pedidosSNMP", null);

    }



    public void insert(String id, String ip, String port, String operation, String version, String cS, Vector<? extends VariableBinding> oids, Vector<? extends VariableBinding> vbResposta, String flag){

        BasicDBObject pedido = new BasicDBObject();
        BasicDBObject resposta = new BasicDBObject();
        BasicDBObject exchange = new BasicDBObject();

        BasicDBObject pedidoV= new BasicDBObject();
        BasicDBObject respostaV= new BasicDBObject();

        ArrayList<BasicDBObject> oidArrayPedido = new ArrayList<BasicDBObject>();
        ArrayList<BasicDBObject> oidArrayResposta = new ArrayList<BasicDBObject>();

        for (VariableBinding oid : oids) {
            pedidoV.put("oid", oid.getOid().toString());
            pedidoV.put("value", oid.getVariable().toString());

            oidArrayPedido.add(pedidoV);
        }

        Date d = new Date();
        DateFormat df1 = new SimpleDateFormat("yyyy/MM/dd");
        DateFormat df2 = new SimpleDateFormat("HH:mm:ss");
        String dia = df1.format(d);
        String horas = df2.format(d);

        pedido.put("id", id);
        pedido.put("operation", operation);
        pedido.put("version", version);
        pedido.put("communityString", cS);
        pedido.put("oids", oidArrayPedido);
        if (flag != null)
            pedido.put("flag", flag);

        for (VariableBinding oid : vbResposta) {
            respostaV.put("oid", oid.getOid().toString());
            respostaV.put("value", oid.getVariable().toString());

            oidArrayResposta.add(respostaV);
        }
        resposta.put("vbs", oidArrayResposta);

        exchange.put("ip", ip);
        exchange.put("port", port);
        exchange.put("dia", dia);
        exchange.put("hora", horas);
        exchange.put("pedido", pedido);
        exchange.put("resposta", resposta);

        this.collection.insert(exchange);
    }
}
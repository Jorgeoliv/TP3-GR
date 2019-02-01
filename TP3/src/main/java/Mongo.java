import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        this.database = this.mongo.getDB("myMongoDb");
        this.database.createCollection("tp3", null);
        this.collection = this.database.getCollection("pedidosSNMP");

    }



    public void insert(String id, String ip, String port, String operation, String version, String cS, String oid){
        BasicDBObject document = new BasicDBObject();

        Date d = new Date();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String date = df.format(d);

        document.put("id", id);
        document.put("ip", ip);
        document.put("port", port);
        document.put("operation", operation);
        document.put("version", version);
        document.put("comunityString", cS);
        document.put("oid", oid);
        document.put("date", date);

        this.collection.insert(document);
    }
}

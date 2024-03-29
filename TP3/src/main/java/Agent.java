//import com.sun.jmx.*;//snmp.SnmpPdu;
//import com.github.dockerjava.api.DockerClient;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
//import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
//import org.bouncycastle.asn1.*;
//import org.bouncycastle.asn1.util.ASN1Dump;
import com.spotify.docker.client.messages.*;
import org.snmp4j.PDU;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.asn1.BEROutputStream;
import org.snmp4j.smi.*;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.snmp4j.asn1.BER.*;

class Pedido implements Runnable{
    MIB mib;
    DatagramPacket pedido;
    HashSet<Integer32> pedidosRecebidos;
    private Mongo db;
    private FlowController fc;
    private ArrayList <String> Admin;
    private ArrayList <String> GetSet;
    private ArrayList <String> Get;

    public Pedido(MIB mib, DatagramPacket pedido, HashSet<Integer32> pedidosRecebidos, Mongo db, FlowController fc, ArrayList <String> Admin, ArrayList <String> GetSet, ArrayList <String> Get){
        this.mib = mib;
        this.pedido = pedido;
        this.pedidosRecebidos = pedidosRecebidos;
        this.db = db;
        this.fc = fc;
        this.Admin = Admin;
        this.GetSet= GetSet;
        this.Get = Get;
    }

    public void run(){

        try {
            //System.out.println("recebi um pedido snmp " + pedido.getLength() + ".From: " + pedido.getSocketAddress());

            //Para ver a estrutura da mensagem
                /*ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(pedido.getData()));
                ASN1Primitive obj = bIn.readObject();
                System.out.println(ASN1Dump.dumpAsString(obj));*/

            PDU pdu = new PDU();

            /**
             * Fazer decode do pacote recebido:
             * -> Decode header
             * -> decode integer (versao)
             * -> decode octet string (comminity string)
             * -> decdo do PDU, que vai ser guardado no pdu a cima descrito
             */

            ByteBuffer b = ByteBuffer.wrap(pedido.getData());
            BERInputStream berStream = new BERInputStream(b);
            BER.MutableByte type = new BER.MutableByte();
            int length = BER.decodeHeader(berStream, type);
            //int startPos = (int) berStream.getPosition();

            if (type.getValue() != BER.SEQUENCE) {
                //System.out.println("Erro no PDU! Nao comeca por uma sequencia!");
            }

            Integer32 version = new Integer32();
            version.decodeBER(berStream);
            OctetString securityName = new OctetString();
            securityName.decodeBER(berStream);
            //Agora o PDU já vai ficar carregado com a toda a informação do
            pdu.decodeBER(berStream);

            String op;
            if(pdu.getType() == PDU.GET)
                op = "snmpget";
            else
                op = "snmpset";

            //System.out.println("O ID do pedido é " + pdu.getRequestID());
            if (pedidosRecebidos.contains(pdu.getRequestID())) {
                //System.out.println("Já analisei este pedido. Vou descartá-lo!!");
                db.insert(pdu.getRequestID().toString(), pedido.getAddress().toString(), "" + pedido.getPort(), op, version.toString(), securityName.toString(), pdu.getVariableBindings(), null,"ID REPETIDO");

                return;
            }
            else {
                pedidosRecebidos.add(pdu.getRequestID());
            }

            /**
             * PROEDIMENTOS !!!!
             * Temos de analisar o pacote, se for valido gerar a resposta senao gerar uma resposta com o respetivo erro
             *
             * Podemos fazer isto numa thread à parte ... Assim podemos receber vários pedidos ... Temos de ter controlo de concorrência na nossa estrutura
             * Se for para criar uma thread, então a única coisa que lhe é preciso passar é o PDU, e a porta e o ip para onde tem de responder ou nao ...
             * Vai depender da nossa implementação ... Mas é obrigatório passar o PDU
             */

            PDU valido = analisaPacote(securityName, pdu, Admin, GetSet, Get);
            PDU pduResposta = new PDU();

            boolean erro = false;

            if (valido == null) {
                //Só um exemplo de como é que podemos carregar os objetos ... Depois mudará
                Vector<? extends VariableBinding> vb = pdu.getVariableBindings();
                if (pdu.getType() == PDU.GET) {
                    pduResposta = analisaVBGet(pduResposta, vb, mib, securityName.toString());
                    //System.out.println("RESPOSTA GET " + pduResposta.getVariableBindings().toString());
                }
                else {
                    if(Admin.contains(securityName.toString()) && pedido.getAddress().toString().equals("/127.0.0.1")){
                        int indiceError = -1;
                        for(int i = 0; i<vb.size() && indiceError == -1; i++){
                            String oid = vb.get(i).getOid().toString();
                            if (oid.startsWith("1.3.6.1.3.6000.5.")) {
                                String indice = oid.substring(17);
                                //se o indice  não for o correto deve devolver "true" para o erro
                                boolean erroAdmin = fc.setValorInstancia(indice, vb.get(i).getVariable().toInt());
                                if (erroAdmin) {
                                    indiceError = i;
                                    pduResposta.setErrorStatus(4);
                                    pduResposta.setErrorIndex(i+1);
                                }
                            }
                            else{
                                indiceError = i;
                                pduResposta.setErrorStatus(16);
                                pduResposta.setErrorIndex(i+1);
                            }
                        }

                        (new Thread(fc)).start();

                        pduResposta.setVariableBindings(vb);
                    }
                    else
                        pduResposta = analisaVBSet(pduResposta, vb, mib);
                    //System.out.println("RESPOSTA SET " + pduResposta.toString());
                }
                db.insert(pdu.getRequestID().toString(), pedido.getAddress().toString(), "" + pedido.getPort(), op, version.toString(), securityName.toString(), vb, pduResposta.getVariableBindings(), null);

            }
            else {
                pduResposta = valido;
                db.insert(pdu.getRequestID().toString(), pedido.getAddress().toString(), "" + pedido.getPort(), op, version.toString(), securityName.toString(), pdu.getVariableBindings(), pduResposta.getVariableBindings(), "COMMUNITY STRING OU OPERAÇÃO NÃO SUPORTADA");

            }
            pduResposta.setType(PDU.RESPONSE);
            pduResposta.setRequestID(pdu.getRequestID());
            /**
             * Tratamento do Encode da informação:
             * -> Primeiro iniciar uma stream para poder carregar os dados
             * -> Efetuar encode do header
             * -> Efetuar encode da versao e da community string (normalmente será igual ao recebido)
             * -> Efetuar encode do pdu que foi gerado pela resposta
             */

            ByteArrayOutputStream bbResposta = new ByteArrayOutputStream();

            int tamanhoResposta = version.getBERLength() + securityName.getBERLength() + pduResposta.getBERLength();

            encodeHeader(bbResposta, SEQUENCE, tamanhoResposta);
            version.encodeBER(bbResposta);
            securityName.encodeBER(bbResposta);
            pduResposta.encodeBER(bbResposta);

            byte[] resposta = new byte[bbResposta.size()];
            resposta = bbResposta.toByteArray();

            //Para analisar a estrutura da resposta
                /*ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(resposta));
                ASN1Primitive obj = bIn.readObject();
                System.out.println(ASN1Dump.dumpAsString(obj));*/

            /**
             * Para se conectar ao netsnmp
             * Depois é so enviar o byte[] resposta, que contém os bytes relativos à resposta do pedido
             */
            (new Thread(new Sender(new DatagramPacket(resposta, resposta.length, pedido.getAddress(), pedido.getPort()), pdu))).start();
        }catch (Exception e){
            System.out.println("ERRO: " + e.getMessage());
        }
    }

    /**
     * Vai analisar os pedidos de SET
     * Neste momento ainda só está configurado para o SET no objeto Param ...
     * AInda é possivel efetuar um set noutra instancia, depois teremos de fazer novas validações para isso
     *
     * @param pduResposta
     * @param vb
     * @param mib
     * @return
     */
    private PDU analisaVBSet(PDU pduResposta, Vector<? extends VariableBinding> vb, MIB mib) {

        /**
         * Possivel solução (para ter o set funcional para as 3 instancias):
         * Se o set for maior do que 3, mandamos logo um erro
         * Se o set for de um só elemento -> verificar se é o OID "3.1.4.x", senao erro
         * Se o set for de 2 elementos -> verificar se são os OIDs "1.1.x" e "1.2.x", senao dar erro
         * Se o set for de 3 elementos -> verificar se são os 3 referidos anteriormente, senão dar erro
         */
        int numOIDs = vb.size();
        if(numOIDs > 3){
            //Temos de por tamanho invalido ... agora esta "too big"
            pduResposta.setErrorStatus(1);
            pduResposta.setErrorIndex(1);
        }else{

            int indiceImage = -1, indiceContainer = -1, indiceStatus = -1;
            String oidImage = null, oidContainer = null, oidStatus = null;
            int indiceError = -1;

            for(int i = 0; i<numOIDs && indiceError == -1; i++){
                String oid = vb.get(i).getOid().toString();

                    switch (oid) {
                        case "1.3.6.1.3.6000.1.1.0":
                            indiceImage = i;
                            oidImage = oid.substring(15);
                            break;
                        case "1.3.6.1.3.6000.1.2.0":
                            indiceContainer = i;
                            oidContainer = oid.substring(15);
                            break;
                        default:
                            if (oid.startsWith("1.3.6.1.3.6000.3.2.1.4")) {
                                indiceStatus = i;
                                oidStatus = oid.substring(15);
                            } else
                                indiceError = i;
                    }
                }


            if(indiceError != -1){
                pduResposta.setErrorIndex(indiceError + 1);
                pduResposta.setErrorStatus(2);
            }
            else {
                if ((oidImage != null && oidContainer == null) || (oidImage == null && oidContainer != null)) {
                    //System.out.println("Combinação nao é valida!");
                    pduResposta.setErrorIndex(0);
                    pduResposta.setErrorStatus(3);
                } else {
                    FeedbackSet fbs = new FeedbackSet();
                    if (oidStatus != null) {
                        fbs = mib.setOIDStatusContainership(oidStatus, vb.get(indiceStatus).getVariable().toString(), indiceStatus);
                    }
                    if (oidContainer != null && oidImage != null && !fbs.erro) {
                        try {
                            int valorI = vb.get(indiceImage).getVariable().toInt();
                            String valorC = vb.get(indiceContainer).getVariable().toString();
                            fbs = mib.setOIDParam(oidImage, valorI, indiceImage, oidContainer, valorC, indiceContainer);
                        } catch (UnsupportedOperationException e) {
                            //System.out.println("Deu um erro de valores invalidos");
                            pduResposta.setErrorIndex(indiceImage + 1);
                            pduResposta.setErrorStatus(7);
                        }
                    }

                    if (fbs.erro) {
                        pduResposta.setErrorIndex(fbs.errorIndex);
                        pduResposta.setErrorStatus(fbs.errorStatus);
                    }

                }
            }

        }

        pduResposta.setVariableBindings(vb);
        return pduResposta;
    }


    /**
     * Vai analisar o PDU que tenha o tipo get
     *
     * @param pduResposta
     * @param vb
     * @param mib
     * @return
     */
    private PDU analisaVBGet(PDU pduResposta, Vector<? extends VariableBinding> vb, MIB mib, String cs) {

        for (int i = 0; i < vb.size(); i++) {
            VariableBinding v = vb.get(i);
            //v.setVariable(new Integer32(i));
            String oid = v.getOid().toString();

            /**
             * Metodo da MIB que vai verificar se é valido ou nao o get
             * Se a Instancia for "null" então quer dizer que não houve um match do OID na nossa mib, logo é preciso dar um erro nesse indice ... Os indices começam em 1
             * Depois temos de validar se o que vamos receber é um int ou um integer, para criar a "Variable" correta
             * NOTA: Na nossa MIB tinhamos definido DisplayStrings, mas aqui nao consegui criar isso ... Usamos Octet String, para ja
             */
            if(oid.startsWith("1.3.6.1.3.6000.5.")){
                if (Admin.contains(cs) && pedido.getAddress().toString().equals("/127.0.0.1")) {
                    String indice = oid.substring(17);
                    int val = fc.valorInstancia(indice);
                    if (val == -1) {
                        pduResposta.setErrorIndex(i+1);
                        pduResposta.setErrorStatus(2);
                    } else {
                        v.setVariable(new Integer32(val));
                    }
                }
                else{
                    pduResposta.setErrorStatus(16);
                    pduResposta.setErrorIndex(i+1);
                }

            }
            else {
                if (!oid.startsWith("1.3.6.1.3.6000.")) {
                    //System.out.println("O caminho inicial nao é correto...");
                    pduResposta.setErrorIndex(0);
                    pduResposta.setErrorStatus(2);
                } else {

                    Instancia instancia = mib.getOID(oid.substring(15));

                    //ver se é diferente null (existe?), mandar o valor consoante o tipo
                    //se no int for -1 ou na string null , mandamos o null, para o utilizador saber que esta vazio
                    if (instancia == null) {
                        //System.out.println("Não foi feito um match com a nossa MIB! Logo não é valido o objeto ...");
                        pduResposta.setErrorIndex(i + 1);
                        //no such name ... Nao devia ser este mas vai ser ...
                        pduResposta.setErrorStatus(2);
                        break;
                    } else {
                        if (instancia.eInteiro) {
                            if (instancia.valorInt != -1)
                                v.setVariable(new Integer32(instancia.valorInt));
                        } else {
                            if (instancia.valorStr != null)
                                v.setVariable(new OctetString(instancia.valorStr));
                        }
                    }
                }
            }

        }


        pduResposta.setVariableBindings(vb);
        return pduResposta;
    }


    /**
     * Valida a community string, e ainda o metodo utilizado ...
     * Podemos tambem verificar a versao
     * @return
     */
    private static PDU analisaPacote(OctetString securityName, PDU pdu, ArrayList <String> Admin, ArrayList <String> GetSet, ArrayList <String> Get) {

        PDU pduRes = new PDU();

        if (pdu.getType() == PDU.GET){
            if (!(Admin.contains(securityName.toString()) || GetSet.contains(securityName.toString()) || Get.contains(securityName.toString()))){
                //authorizationerror
                pduRes.setErrorStatus(16);
                pduRes.setErrorIndex(0);
                return pduRes;
            }
        }
        else{
            if (pdu.getType() == PDU.SET){
                if (!(Admin.contains(securityName.toString()) || GetSet.contains(securityName.toString()) || Get.contains(securityName.toString()))){
                    //authorizationerror
                    pduRes.setErrorStatus(16);
                    pduRes.setErrorIndex(0);
                    return pduRes;
                }
                else{
                    if (Get.contains(securityName.toString())){
                        //ERRO DE NAO PERMIÇÃO
                        pduRes.setErrorStatus(16);
                        pduRes.setErrorIndex(0);
                        return pduRes;
                    }
                }
            }
            else{
                pduRes.setErrorIndex(0);
                pduRes.setErrorStatus(19);
                return pduRes;
            }
        }

        return null;

    }
}

public class Agent {

    /**
     * @pedidosRecebidos Para controlar os pedidos que chegaram, e evitar respostas repetidas
     */

    public static void main(String[] args) throws /*DockerCertificateException, DockerException,*/ InterruptedException {
        HashSet<Integer32> pedidosRecebidos = new HashSet<>();



        int port = 6000;
        List allImages = null;
        ArrayList <String> Admin = null;
        ArrayList <String> GetSet = null;
        ArrayList <String> Get = null;
        try {


            //CARREGA COMMUNITY STRINGS

            File file = new File("./CommunityStrings.txt");
            Scanner sc = new Scanner(file);
            String cs = "";
            Admin = new ArrayList<String>();
            GetSet = new ArrayList<String>();
            Get = new ArrayList<String>();

            port = Integer.parseInt(sc.nextLine().split("udpPort:")[1]);

            while (sc.hasNextLine()){
                cs = sc.nextLine();
                if (cs.startsWith("Admin")){
                    Admin.add(cs.split(":")[1]);
                }
                else{
                    if (cs.startsWith("GetSet")){
                        GetSet.add(cs.split(":")[1]);
                    }
                    else{
                        if (cs.startsWith("Get")){
                            Get.add(cs.split(":")[1]);
                        }
                    }
                }
            }

            System.out.println("ADMIN: " + Admin.toString());
            System.out.println("GETSET: " + GetSet.toString());
            System.out.println("GET: " + Get.toString());

            final DockerClient client = DefaultDockerClient
                    .fromEnv()
                    .build();

            allImages = client.listImages();

            client.close();
        }catch (Exception e){
            System.out.println("acabou");
            return;
        }
        //Marcar a data de inicio
        Date d = new Date();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String data = df.format(d);

        MIB mib = new MIB(data);
        mib.carregaImagens(allImages);


        try {
            /**
             * Para poder estabelecer uma conexão com o net-snmp
             * Depois fica em escuta
             */

            //Criar uma instancia de MongoDB
            Mongo db = new Mongo("localhost", 27017);

            DatagramSocket serverSocket = new DatagramSocket(null);
            InetSocketAddress s = new InetSocketAddress("127.0.0.1",port);
            serverSocket.bind(s);
            //Poderá não ser necessário um byte com um tamanho tao grande, mas é so uma questão de depois mudarmos se quisermos ...

            Thread t;
            FlowController fc = new FlowController(1000, 10, 5 ,6); //(periodo, limite instantanio, limite ao longo do tempo (media temporal))
            (new Thread (fc)).start();

            while(true){
                DatagramPacket pedido = new DatagramPacket(new byte[10240], 10240);
                serverSocket.receive(pedido);
                (new Thread(new Pedido(mib, pedido, pedidosRecebidos, db , fc, Admin, GetSet, Get))).start();

                /*w = new Worker(pedido, pedidosRecebidos, mib);
                t = new Thread(w);
                t.start();*/
                //System.out.println("vou espear");
                //Thread.sleep(250);
                if(fc.incrementar()){
                    System.out.println("DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!DORMIR!!!!!!!!");
                    Thread.sleep(2000);
                }
            }

        }

        catch (IOException e) {
            e.printStackTrace();
        }
    }

}

class FlowController implements  Runnable {

    private int contador;
    private int contadorMomento[];
    private int contadorTotal;
    private long fixedTimer;
    private int period;
    private int periodAnt;
    private int upperInstantLimit;
    private int upperMomentLimit;
    private int upperTemporalLimit;
    private TimerTask task;
    private Timer timer;

    private boolean flagMoment;
    int i;
    public FlowController(int p, int uIL, int uML, int uTL){

        this.contador = 0;
        this.contadorMomento = new int[4];
        Arrays.fill(this.contadorMomento, 0);
        this.contadorTotal = 0;
        this.period = p;
        this.periodAnt = p;
        int inc = period/1000;
        this.fixedTimer = 0;
        this.upperInstantLimit = uIL;
        this.upperMomentLimit = uML;
        this.upperTemporalLimit = uTL;

        this.flagMoment = false;
        double div = 1/(((double)this.period /1000) *4);
        this.timer = new Timer("Timer");

        this.task = new TimerTask() {

            public void run() {

                int sum, j;

                if (i == 4)
                    i = 0;

                contadorMomento[i] = contador;
                i++;
                for (sum = 0, j = 0; j < 4; j++) {
                    sum += contadorMomento[j];
                }

                flagMoment = ((sum * div) > upperMomentLimit);

                if (flagMoment)
                    System.out.println("FOI O FLOW MOMENTANIO");

                System.out.println("NOVO CICLO => contador: " + contador);


                contador = 0;
                contadorTotal++;
                if (fixedTimer > 999999999)
                    fixedTimer = 0;
                fixedTimer += inc;

                if (period != periodAnt){
                    periodAnt = period;
                    timer.cancel();
                    timer.purge();
                    System.out.println("MATEI O TIMER 1");
                }
                else{
                    System.out.println("NAO MATEI O TIMER");
                }
            }
        };
    }

    public void run(){
        try {
            Thread.sleep(this.period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("ACORDEI");
        this.timer = null;
        this.timer = new Timer();
        this.task = new TimerTask() {

            public void run() {
                int inc = period/1000;
                double div = 1/(((double)period /1000) *4);

                int sum, j;

                if (i == 4)
                    i = 0;

                contadorMomento[i] = contador;
                i++;
                for (sum = 0, j = 0; j < 4; j++) {
                    sum += contadorMomento[j];
                }

                flagMoment = ((sum * div) > upperMomentLimit);

                if (flagMoment)
                    System.out.println("FOI O FLOW MOMENTANIO");

                System.out.println("NOVO CICLO => contador: " + contador);


                contador = 0;
                contadorTotal++;
                if (fixedTimer > 999999999)
                    fixedTimer = 0;
                fixedTimer += inc;

                if (period != periodAnt){
                    periodAnt = period;
                    timer.cancel();
                    timer.purge();
                }
            }
        };
        this.timer.schedule(this.task, this.period*2, this.period);
    }

    public boolean incrementar(){
        boolean flagInstantania = false;
        boolean flagTemporal = false;
        boolean res;
        double media;

        this.contador++;
        this.contadorTotal++;

        if(!flagMoment) {
            if (this.contador >= this.upperInstantLimit) {
                System.out.println("FOI O FLOW INSTANTANIO");
                flagInstantania = true;
            }

            media = (double) this.contadorTotal / (double) this.fixedTimer;
            if ((!flagInstantania) && media > upperTemporalLimit){
                flagTemporal = true;
                System.out.println("FOI O FLOW TEMPORAL");

            }
        }

        res = flagInstantania || flagMoment || flagTemporal;

    return res;
    }


    public synchronized boolean setValorInstancia(String indice, int val) {

        if(val < 1)
            return true;

        switch (indice){
            case "1.0":
                this.period = val;
                return false;
            case "2.0":
                this.upperInstantLimit = val;
                return false;
            case "3.0":
                this.upperMomentLimit = val;
                return false;
            case "4.0":
                this.upperTemporalLimit = val;
                return false;
            default:
                return true;
        }
    }

    public int valorInstancia(String indice) {

        switch (indice){
            case "1.0":
                return this.period;
            case "2.0":
                return this.upperInstantLimit;
            case "3.0":
                return this.upperMomentLimit;
            case "4.0":
                return this.upperTemporalLimit;
            default:
                return -1;
        }
    }
}

class Sender implements Runnable{
    private DatagramPacket dp;
    public Sender(DatagramPacket dp, PDU pdu){
        this.dp = dp;
    }

    public void run(){
        Random rand = new Random();

        int port = rand.nextInt(40000) + 7000;
        try {
            DatagramSocket ds = new DatagramSocket(port);
            //System.out.println("VOU ENVIAR UMA RESPOSTA PARA O: " + dp.getSocketAddress());
            ds.send(dp);
        }
        catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
}

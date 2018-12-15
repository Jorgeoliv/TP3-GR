//import com.sun.jmx.*;//snmp.SnmpPdu;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.util.ASN1Dump;
import org.snmp4j.PDU;
import org.snmp4j.SNMP4JSettings;
import org.snmp4j.asn1.BER;
import org.snmp4j.asn1.BERInputStream;
import org.snmp4j.asn1.BEROutputStream;
import org.snmp4j.smi.*;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Vector;

import static org.snmp4j.asn1.BER.*;

public class Agent {

    /**
     * @pedidosRecebidos Para controlar os pedidos que chegaram, e evitar respostas repetidas
     */
    private static HashSet<Integer32> pedidosRecebidos = new HashSet<>();

    public static void main(String[] args){

        //Marcar a data de inicio
        Date d = new Date();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String data = df.format(d);

        MIB mib = new MIB(data);

        try {
            /**
             * Para poder estabelecer uma conexão com o net-snmp
             * Depois fica em escuta
             */
            DatagramSocket serverSocket = new DatagramSocket(null);
            InetSocketAddress s = new InetSocketAddress("127.0.0.1",6000);
            serverSocket.bind(s);
            //Poderá não ser necessário um byte com um tamanho tao grande, mas é so uma questão de depois mudarmos se quisermos ...
            DatagramPacket pedido = new DatagramPacket(new byte[10240], 10240);

            while(true){
                serverSocket.receive(pedido);
                System.out.println("recebi um pedido snmp " + pedido.getLength() + ".From: " + pedido.getSocketAddress());

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
                    System.out.println("Erro no PDU! Nao comeca por uma sequencia!");
                }

                Integer32 version = new Integer32();
                version.decodeBER(berStream);
                OctetString securityName = new OctetString();
                securityName.decodeBER(berStream);
                //Agora o PDU já vai ficar carregado com a toda a informação do
                pdu.decodeBER(berStream);

                /**
                 * PROEDIMENTOS !!!!
                 * Temos de analisar o pacote, se for valido gerar a resposta senao gerar uma resposta com o respetivo erro
                 *
                 * Podemos fazer isto numa thread à parte ... Assim podemos receber vários pedidos ... Temos de ter controlo de concorrência na nossa estrutura
                 * Se for para criar uma thread, então a única coisa que lhe é preciso passar é o PDU, e a porta e o ip para onde tem de responder ou nao ...
                 * Vai depender da nossa implementação ... Mas é obrigatório passar o PDU
                 */

                 PDU valido = analisaPacote(version, securityName, pdu);
                 PDU pduResposta = new PDU();

                 boolean erro = false;

                 if(valido == null) {
                     //Só um exemplo de como é que podemos carregar os objetos ... Depois mudará
                     Vector<? extends VariableBinding> vb = pdu.getVariableBindings();
                     if(pdu.getType() == PDU.GET)
                         pduResposta = analisaVBGet(pduResposta, vb, mib);
                     else
                         pduResposta = analisaVBSet(pduResposta, vb, mib);

                 }else
                     pduResposta = valido;


                System.out.println("O ID do pedido é " + pdu.getRequestID());
                if(pedidosRecebidos.contains(pdu.getRequestID()))
                    System.out.println("Já analisei este pedido. Vou descartá-lo!! AINDA É PRECISO FAZER ISTO!");
                else
                    pedidosRecebidos.add(pdu.getRequestID());


                //pduResposta.setErrorIndex(1);
                //pduResposta.setErrorStatus(3);
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

                System.out.println("New string da resposta: " + new String(resposta));

                //Para analisar a estrutura da resposta
                /*ASN1InputStream bIn = new ASN1InputStream(new ByteArrayInputStream(resposta));
                ASN1Primitive obj = bIn.readObject();
                System.out.println(ASN1Dump.dumpAsString(obj));*/

                /**
                 * Para se conectar ao netsnmp
                 * Depois é so enviar o byte[] resposta, que contém os bytes relativos à resposta do pedido
                 */

                DatagramSocket respondeSnmp = new DatagramSocket(null);
                InetSocketAddress snmp = new InetSocketAddress(pedido.getAddress(), pedido.getPort());
                respondeSnmp.connect(snmp);
                respondeSnmp.send(new DatagramPacket(resposta, resposta.length));
            }

        } catch (IOException e) {
            e.printStackTrace();
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
    private static PDU analisaVBSet(PDU pduResposta, Vector<? extends VariableBinding> vb, MIB mib) {

        /**
         * Possivel solução (para ter o set funcional para as 3 instancias):
         * Se o set for maior do que 3, mandamos logo um erro
         * Se o set for de um só elemento -> verificar se é o OID "3.1.4.x", senao erro
         * Se o set for de 2 elementos -> verificar se são os OIDs "1.1.x" e "1.2.x", senao dar erro
         * Se o set for de 3 elementos -> verificar se são os 3 referidos anteriormente, senão dar erro
         */

        if(vb.size() != 2){
            //Temos de por tamanho invalido ... agora esta "too big"
            pduResposta.setErrorStatus(1);
            pduResposta.setErrorIndex(1);
        }else{
            int a = vb.get(0).getVariable().toInt();
            System.out.println(a);
            String b = vb.get(1).getVariable().toString();
            System.out.println(b.getClass());

            /**
             * Tamos a pressupor esta ordem ... Mas os OIDs podem vir trocados, depois temos de fazer parse e mandar da forma correta
             */
            mib.setOIDParam(vb.get(0).getOid().toString(), a, vb.get(1).getOid().toString(), b);
            pduResposta.setErrorStatus(0);
        }

        for (int i = 0; i < vb.size(); i++) {
            System.out.println(vb.get(i));
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
    private static PDU analisaVBGet(PDU pduResposta, Vector<? extends VariableBinding> vb, MIB mib) {
        for (int i = 0; i < vb.size(); i++) {
            System.out.println(vb.get(i));
            VariableBinding v = vb.get(i);
            //v.setVariable(new Integer32(i));
            String oid = v.getOid().toString();

            /**
             * Metodo da MIB que vai verificar se é valido ou nao o get
             * Se a Instancia for "null" então quer dizer que não houve um match do OID na nossa mib, logo é preciso dar um erro nesse indice ... Os indices começam em 1
             * Depois temos de validar se o que vamos receber é um int ou um integer, para criar a "Variable" correta
             * NOTA: Na nossa MIB tinhamos definido DisplayStrings, mas aqui nao consegui criar isso ... Usamos Octet String, para ja
             */
            Instancia instancia = mib.getOID(oid);

            //ver se é diferente null (existe?), mandar o valor consoante o tipo
            //se no int for -1 ou na string null , mandamos o null, para o utilizador saber que esta vazio
            if(instancia == null){
                System.out.println("Não foi feito um match com a nossa MIB! Logo não é valido o objeto ...");
                pduResposta.setErrorIndex(i+1);
                //no such name ... Nao devia ser este mas vai ser ...
                pduResposta.setErrorStatus(2);
                break;
            }
            else{
                if(instancia.eInteiro){
                    if(instancia.valorInt != -1)
                        v.setVariable(new Integer32(instancia.valorInt));
                }
                else{
                    if(instancia.valorStr != null)
                        v.setVariable(new OctetString(instancia.valorStr));
                }
            }

        }

        for (int i = 0; i < vb.size(); i++) {
            System.out.println(vb.get(i));
        }

        pduResposta.setVariableBindings(vb);
        return pduResposta;
    }


    /**
     * Valida a community string, e ainda o metodo utilizado ...
     * Podemos tambem verificar a versao
     *
     * @param version
     * @param securityName
     * @param pdu
     * @return
     */
    private static PDU analisaPacote(Integer32 version, OctetString securityName, PDU pdu) {

        PDU pduRes = new PDU();

        if(!(securityName.toString().equals("public") || securityName.toString().equals("TP3GR"))) {
            //authorizationerror
            pdu.setErrorStatus(16);
            pdu.setErrorIndex(0);
            return pduRes;
        }

        System.out.println(PDU.GET + " " + PDU.SET);
        System.out.println(pdu.getType());

        if(!(pdu.getType() == PDU.GET || pdu.getType() == PDU.SET)) {
            pdu.setErrorIndex(0);
            pdu.setErrorStatus(19);
            return pdu;
        }

        System.out.println("sou vosso amigo");

        return null;
    }

}
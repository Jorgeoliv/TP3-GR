//import com.spotify.docker.client.messages.Image;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;

class FeedbackSet{
    boolean erro;
    int errorIndex;
    int errorStatus;

    public FeedbackSet(){
        erro = false;
    }

    public FeedbackSet(int errorIndex, int errorStatus){
        this.errorIndex = errorIndex + 1;
        this.errorStatus = errorStatus;
        this.erro = true;
    }

    @Override
    public String toString() {
        return "FeedbackSet{" +
                "erro=" + erro +
                ", errorIndex=" + errorIndex +
                ", errorStatus=" + errorStatus +
                '}';
    }
}

class Instancia{
    int valorInt;
    String valorStr;
    String oid;
    boolean eInteiro;

    public Instancia(int valorInt, String oid) {
        this.valorInt = valorInt;
        this.oid = oid;
        this.valorStr = null;
        eInteiro = true;
    }

    public Instancia(String valorStr, String oid) {
        this.valorStr = valorStr;
        this.oid = oid;
        this.valorInt = -1;
        eInteiro = false;
    }

    @Override
    public String toString() {
        return "Instancia{" +
                "valorInt=" + valorInt +
                ", valorStr='" + valorStr + '\'' +
                ", oid='" + oid + '\'' +
                ", eInteiro=" + eInteiro +
                '}';
    }
}


public class MIB {

    private HashMap<String, Instancia> valores = new HashMap<>();
    //Tamanho da tabela container
    int numeroEntradaTableContainer = 0;
    //Lock para a tabela do container
    ReentrantLock tableContainer = new ReentrantLock();
    //lock para o o objeto Param
    ReentrantLock rl = new ReentrantLock();

    public MIB(String dataInicio){

        /**
         * Carrega o objeto Param
         */
        valores.put("1.1.0", new Instancia(-1, "1.1.0")); //get and set
        valores.put("1.2.0", new Instancia(null, "1.2.0")); //get and set
        valores.put("1.3.0", new Instancia(0, "1.3.0")); //get

        valores.put("3.1.0", new Instancia(0, "3.1.0"));

        /**
         * Carrega o objeto relativo ao historio
         */
        //Data de inicio do agente
        valores.put("4.1.0", new Instancia(dataInicio.toString(), "4.1.0"));
        //ultima criação de container
        valores.put("4.2.0", new Instancia(null, "4.2.0"));

        //So para testar a imagem
        //valores.put("2.2.1.1", new Instancia(1, "2.2.1.1"));
    }

    public Instancia getOID(String oid){
        System.out.println(oid.substring(0,1));
        System.out.println(oid.substring(0,0));
        switch(oid.substring(0,1)){
            case "2":
                Instancia i = valores.get(oid);
                System.out.println("===========");
                System.out.println((i == null ? "É nulo" : i.toString()));
                System.out.println("===========");
                return i;
            case "4":
                Instancia j = valores.get(oid);
                System.out.println("===========");
                System.out.println((j == null ? "É nulo" : j.toString()));
                System.out.println("===========");
                return j;
            case "1":
                try {
                    rl.lock();
                    Instancia k = valores.get(oid);
                    System.out.println("===========");
                    System.out.println((k == null ? "É nulo" : k.toString()));
                    System.out.println("===========");
                    return k;
                }finally {
                    rl.unlock();
                }
            case "3":
                try {
                    tableContainer.lock();
                    Instancia p = valores.get(oid);
                    System.out.println("===========");
                    System.out.println((p == null ? "É nulo" : p.toString()));
                    System.out.println("===========");
                    return p;
                }finally {
                    tableContainer.unlock();
                }
            default: return null;
        }

    }

    /**
     * Para carregar a mib das imagens
     */

    public void carregaImagens(List allImages){
        Iterator<List> it = allImages.iterator();

        int i = 0;
        while(it.hasNext()) {
            Image image = (Image) it.next();
            valores.put("2.2.1." + i, new Instancia(i, "2.2.1." + i));
            String aux = image.repoTags().toString();
            valores.put("2.2.2." + i, new Instancia(aux.substring(1, aux.length()-1), "2.2.2." + i));
            i++;
        }
        valores.put("2.1.0", new Instancia(i, "2.1.0"));

        System.out.println("VAMOS VER COMO ESTA:");
        System.out.println(valores);
    }

    /**
     * Falta definir os erros que vamos mandar para tras ...
     * --> Das duas uma, ou fazemos com exceções ou mandamos uma classe com o indice do erro (a nivel de performance acho melhor a classe)
     * Para já estamos a validar aqui os objetos recebidos, mas depois isso será feito no Agent.java
     *
     *
     * @param oidImagem
     * @param value
     * @param oidContainer
     * @param nome
     * @return
     */

    public FeedbackSet setOIDParam(String oidImagem, int value, int indiceI, String oidContainer, String nome, int indiceC){

        //Validar se a imagem existe - 2.1.1.value
        String imageTableIndex = "2.1.1." + value;
        if(!valores.containsKey(imageTableIndex)){
            System.out.println("Não temos essa imagem carregada ..." + imageTableIndex);
            return new FeedbackSet(indiceI, 11);
        }

        rl.lock();
        int flag = valores.get("1.3.0").valorInt;
        if (flag == 0) {
            //Acrescentar os valores no mapa ...
            Instancia auxImagem = valores.get(oidImagem);
            auxImagem.valorInt = value;
            valores.put(oidImagem, auxImagem);
            Instancia auxContainer = valores.get(oidContainer);
            auxContainer.valorStr = nome;
            valores.put(oidContainer, auxContainer);

            //Validar se

            //Alterar a Flag
            Instancia auxFlag = valores.get("1.3.0");
            auxFlag.valorInt = 1;
            valores.put("1.3.0", auxFlag);

            System.out.println(valores);

            //Devemos de libertar aqui o lock, para que outro utilizador possa efetuar um get ou então um set que dê erro
            rl.unlock();

            tableContainer.lock();
            //totalContainer
            Instancia totalContainers = valores.get("3.1.0");
            totalContainers.valorInt++;
            valores.put("3.1.0", totalContainers);
            //ID
            String tableIDContainer = "3.2.1." + numeroEntradaTableContainer;
            Instancia idContainer = new Instancia(numeroEntradaTableContainer, tableIDContainer);
            valores.put(tableIDContainer, idContainer);
            //Nome
            String tableNameContainer = "3.2.2." + numeroEntradaTableContainer;
            Instancia nameContainer = new Instancia(nome, tableNameContainer);
            valores.put(tableNameContainer, nameContainer);
            //Indice de imagem
            String tableImageContainer = "3.2.3." + numeroEntradaTableContainer;
            Instancia imageContainer = new Instancia(value, tableImageContainer);
            valores.put(tableImageContainer, imageContainer);
            //Status do container
            String tableStatusContainer = "3.2.4." + numeroEntradaTableContainer;
            Instancia statusContainer = new Instancia("creating", tableStatusContainer);
            valores.put(tableStatusContainer, statusContainer);
            //Processador <- Aqui depois é preciso que seja o docker a monitorizar isto ...
            String tableProcessorContainer = "3.2.5." + numeroEntradaTableContainer;
            Instancia processorContainer = new Instancia(50, tableProcessorContainer);
            valores.put(tableProcessorContainer, processorContainer);
            tableContainer.unlock();

            /**
             * Necessário trabalhar com o Docker ...
             * Efetuamos umas tentativas falhadas
             */
            //um try/catch, por exemplo, para criar o container
            try {
                final DockerClient client = DefaultDockerClient
                        .fromEnv()
                        .build();

                final ContainerCreation container = client.createContainer(ContainerConfig
                        .builder()
                        .image(nome)
                        .build()
                );

                client.startContainer(container.id());
                final ContainerInfo info = client.inspectContainer(container.id());

                System.out.println(info);
                client.close();

                /**
                 * Só podemos fazer isto se obtivermos sucesso na criaão do container  ...
                 */
                tableContainer.lock();
                //Status do container
                statusContainer = new Instancia("up", tableStatusContainer);
                valores.put(tableStatusContainer, statusContainer);
                tableContainer.unlock();

                Date d = new Date();
                DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                String data = df.format(d);
                Instancia ultimaAtualizacao = valores.get("4.2.0");
                ultimaAtualizacao.valorStr = data;
                valores.put("4.2.0", ultimaAtualizacao);

                return new FeedbackSet();
            }catch(Exception e){
                System.out.println("Deu um erro: " + e.getMessage());
                //Status do container
                tableContainer.lock();
                statusContainer = new Instancia("removing", tableStatusContainer);
                valores.put(tableStatusContainer, statusContainer);
                tableContainer.unlock();
                return new FeedbackSet(-1, 11);
            }
        }else{
            System.out.println("Erro: Outro container a ser criado ..");
            rl.unlock();
            return new FeedbackSet(0, 11);
        }

    }

    private boolean statusContainerValido(String valor){
        switch(valor){
            case "up": return true;
            case "down": return true;
            case "removing": return true;
            case "creating": return true;
            case "changing": return true;
            default: return false;
        }
    }

    public FeedbackSet setOIDStatusContainership(String statusContainership, String valor, int indiceS) {
        Instancia i = valores.get(statusContainership);
        if(i == null){
            System.out.println("Nao existe o OID: " + statusContainership);
            return new FeedbackSet(indiceS, 2);
        }
        tableContainer.lock();
        if(i.valorStr.equals("up") || i.valorStr.equals("down")){
            i.valorStr = valor;
            if(statusContainerValido(valor)) {
                valores.put(statusContainership, i);
                tableContainer.unlock();
                return new FeedbackSet();
            }else{
                System.out.println("O valor da string nao é valido! " + valor);
                return  new FeedbackSet(indiceS, 10);
            }
        }else{
            tableContainer.unlock();
            return new FeedbackSet(indiceS, 10);
        }
    }
}

import com.spotify.docker.client.messages.Image;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

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

        /**
         * Carrega o objeto relativo ao historio
         */
        valores.put("4.1.0", new Instancia(dataInicio.toString(), "4.1.0"));
    }



    public Instancia getOID(String oid){
        try {
            rl.lock();
            Instancia i = valores.get(oid);
            System.out.println("===========");
            System.out.println((i == null ? "É nulo" : i.toString()));
            System.out.println("===========");
            return i;
        }finally {
            rl.unlock();
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
            valores.put("2.1.1." + i, new Instancia(i, "2.1.1." + i));
            String aux = image.repoTags().toString();
            valores.put("2.1.2." + i, new Instancia(aux.substring(1, aux.length()-1), "2.1.2." + i));
            i++;
        }

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

    public boolean setOIDParam(String oidImagem, int value, String oidContainer, String nome){

        if(!oidImagem.equals("1.1.0")) {
            System.out.println("Erro no primeiro argumento");
            if(valores.containsKey(oidImagem))
                System.out.println("Temos de retornar um erro de que nao é permitido fazer set");
            return false;
        }
        if(!oidContainer.equals("1.2.0")){
            System.out.println("Erro no seundo argumento");
            if(valores.containsKey(oidContainer))
                System.out.println("Temos de retornar um erro de que nao é permitido fazer set");
            return false;
        }

        //Validar se a imagem existe - 2.1.1.value
        String imageTableIndex = "2.1.1." + value;
        if(!valores.containsValue(imageTableIndex)){
            System.out.println("Não temos essa imagem carregada ...");
            return false;
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
            System.out.println(valores);

            //Validar se

            //Alterar a Flag
            Instancia auxFlag = valores.get("1.3.0");
            auxFlag.valorInt = 1;
            valores.put("1.3.0", auxFlag);

            //Devemos de libertar aqui o lock, para que outro utilizador possa efetuar um get ou então um set que dê erro
            rl.unlock();

            /**
             * Necessário trabalhar com o Docker ...
             * Efetuamos umas tentativas falhadas
             */
            //um try/catch, por exemplo, para criar o container

            /**
             * Só podemos fazer isto se obtivermos sucesso na criaão do container  ...
             */
            tableContainer.lock();
                //ID
                String tableIDContainer = "3.1.1." + numeroEntradaTableContainer;
                Instancia idContainer = new Instancia(numeroEntradaTableContainer, tableIDContainer);
                valores.put(tableIDContainer, idContainer);
                //Nome
                String tableNameContainer = "3.1.2." + numeroEntradaTableContainer;
                Instancia nameContainer = new Instancia(nome, tableNameContainer);
                valores.put(tableNameContainer, nameContainer);
                //Indice de imagem
                String tableImageContainer = "3.1.3." + numeroEntradaTableContainer;
                Instancia imageContainer = new Instancia(value, tableImageContainer);
                valores.put(tableImageContainer, imageContainer);
                //Status do container
                String tableStatusContainer = "3.1.4." + numeroEntradaTableContainer;
                Instancia statusContainer = new Instancia("up", tableStatusContainer);
                valores.put(tableStatusContainer, statusContainer);
                //Processador <- Aqui depois é preciso que seja o docker a monitorizar isto ...
                String tableProcessorContainer = "3.1.5." + numeroEntradaTableContainer;
                Instancia processorContainer = new Instancia(50, tableProcessorContainer);
                valores.put(tableProcessorContainer, processorContainer);
            tableContainer.unlock();



            return true;
        }else{
            System.out.println("Erro: Outro container a ser criado ..");
            rl.unlock();
            return false;
        }

    }



}

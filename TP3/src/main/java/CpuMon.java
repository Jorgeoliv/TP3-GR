import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.*;

class CpuContainer{
    ContainerCreation container;
    Long totalAnterior;
    Long systemAnterior;
    Long total;
    Long system;
    String statusContainer;
    boolean ativo = true;

    public CpuContainer(Long t, Long s, String statusContainer, ContainerCreation container){
        this.totalAnterior = t;
        this.systemAnterior = s;
        this.statusContainer = statusContainer;
        this.container = container;

    }

}

public class CpuMon implements Runnable{
    HashMap<String, CpuContainer> cpus =  new HashMap<>();
    DockerClient client;
    HashMap<String, Instancia> valores;

    public CpuMon(HashMap<String,Instancia> valores) {

        this.valores = valores;
        try{
            client = DefaultDockerClient
                    .fromEnv()
                    .build();
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    public void inativo(String statusContainer){
        CpuContainer cpu = cpus.get(statusContainer);
        cpu.ativo = false;
        cpus.put(statusContainer, cpu);
    }

    public void ativo(String statusContainer){
        CpuContainer cpu = cpus.get(statusContainer);
        cpu.ativo = true;
        cpus.put(statusContainer, cpu);
    }

    public void adicionaCpu(String status, CpuContainer cpu){
        this.cpus.put(status, cpu);
    }

    @Override
    public void run() {
        while(!Thread.interrupted()){
            for(CpuContainer cpu: this.cpus.values()){
                try {
                    if(cpu.ativo) {
                        ContainerStats info;
                        info = client.stats(cpu.container.id());

                        cpu.total = info.cpuStats().cpuUsage().totalUsage();
                        cpu.system = info.cpuStats().systemCpuUsage();

                        double percentagemCpu = 0.0;
                        Long cpuDelta = cpu.total - cpu.totalAnterior;
                        Long systemDelta = cpu.system - cpu.systemAnterior;
                        double totalPerc = (float) info.cpuStats().cpuUsage().percpuUsage().size();
                        if (cpuDelta > 0 && systemDelta > 0)
                            percentagemCpu = (double) cpuDelta / (double) systemDelta * totalPerc * 100;
                        Instancia status = this.valores.get(cpu.statusContainer);
                        status.valorStr = "" + percentagemCpu;
                        valores.put(cpu.statusContainer, status);
                        cpu.totalAnterior = cpu.total;
                        cpu.systemAnterior = cpu.system;
                    }
                } catch (DockerException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    System.out.println(e.getMessage());
                }
            }
        }
        client.close();
    }
}

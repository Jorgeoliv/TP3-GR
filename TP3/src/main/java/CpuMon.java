import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.*;

class CpuContainer{
    ContainerCreation container;
    Long totalAnterior;
    Long systemAnterior;
    Long total;
    Long system;
    String statusContainer;
    boolean ativo = true;

    public CpuContainer(Long t, Long s, String statusContainer){
        this.totalAnterior = t;
        this.systemAnterior = s;
        this.statusContainer = statusContainer;
    }

}

public class CpuMon implements Runnable{
    HashMap<String, CpuContainer> cpus =  new HashMap<>();
    final DockerClient client = DefaultDockerClient
            .fromEnv()
            .build();
    HashMap<String, Instancia> valores;

    public CpuMon(HashMap<String,Instancia> valores) {
        this.valores = valores;
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
                ContainerStats info;
                info = client.stats(cpu.container.id());
                cpu.total = info.cpuStats().cpuUsage().totalUsage();
                cpu.system = info.cpuStats().systemCpuUsage();

                double percentagemCpu = 0.0;
                Long cpuDelta = cpu.total - cpu.totalAnterior;
                Long systemDelta = cpu.system - cpu.systemAnterior;
                double totalPerc = (float) info.cpuStats().cpuUsage().percpuUsage().size();
                if(cpuDelta > 0 && systemDelta > 0)
                    percentagemCpu = (double)cpuDelta / (double)systemDelta * totalPerc * 100;
                Instancia status = this.valores.get(cpu.statusContainer);
                status.valorInt = (int) percentagemCpu;
                System.out.println("O valor é: " + status.valorInt);
                valores.put(cpu.statusContainer, status);
                cpu.totalAnterior = cpu.total;
                cpu.systemAnterior = cpu.system;
            }
        }
        client.close();
    }
}

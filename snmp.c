#include <unistd.h>
#include <stdio.h>

int main(int argc, char const *argv[]){
	printf("%s %s\n", argv[1], argv[2]);
	int pedidosPorSegundo = atoi(argv[1]);
	int nPedidos = atoi(argv[2]);
	double timeToWait = 1/ ((double) pedidosPorSegundo);
	timeToWait *= 1000000;
	int i = 0;

	if(fork()== 0){
		while(i < nPedidos){
			usleep(timeToWait);
			if(fork() == 0){
				execlp("snmpget", "snmpget", "-v2c", "-c", "public", "localhost:6000", "1.3.6.1.3.6000.3.2.5.1", (char *)NULL);
				_exit(0);
			}
			wait(NULL);
			i++;
		}
	}

	wait(NULL);
	return 0;
}
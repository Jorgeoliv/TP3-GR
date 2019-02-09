snmpset -v2c -c priv localhost:6001 1.3.6.1.3.6000.1.1.0 i 1 1.3.6.1.3.6000.1.2.0 s "Apresentacao"
snmpset -v2c -c priv localhost:6001 1.3.6.1.3.6000.3.2.1.4.1 s "down"
#snmpset -v2c -c priv localhost:6000 1.3.6.1.3.6000.3.2.1.4.1 s "up"
#snmpset -v2c -c admin localhost:6000 1.3.6.1.3.6000.5.1.1 i 20000

snmpget -v2c -c public localhost:6001 1.3.6.1.3.6000.4.1.0 1.3.6.1.3.6000.4.2.0

snmpget -v2c -c public localhost:6001 1.3.6.1.3.6000.3.2.1.1.1 1.3.6.1.3.6000.3.2.1.2.1 1.3.6.1.3.6000.3.2.1.3.1 1.3.6.1.3.6000.3.2.1.4.1 1.3.6.1.3.6000.3.2.1.5.1
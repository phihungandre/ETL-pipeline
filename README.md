<h1 align="center">Harmony Poc</h1>
<br><br>


Génération aléatoire de rapports :
[Data Generator](data_generator/data_generator.py)<br>

Envoi des rapports et alertes :
[Producer](producer/src/main/scala/producer_project.scala)<br>

Stockage des rapports et alertes dans un Data Lake :
[Consumer Data](consumer_data/src/main/scala/consumer_data.scala)<br>

Stockage des alertes dans une base SQL :
[Consumer Alert](consumer_alert/src/main/scala/consumer_alert.scala)<br>

Envoi des notifications d'alertes :

- [Version C++](report_alert_SQL/main.cpp)
- [Version Scala](notification_scala/src/main/scala/Main.scala)

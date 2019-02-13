#FULLY DISTRIBUTED MODE
#NAME NODE CONFIG

etc/hadoop/core-site.xml

<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://ec2-##-###-###-##.compute-#.amazonaws.com</value>
    </property>
	<property>
        <name>hadoop.tmp.dir</name>
        <value>/home/ubuntu/hdfstmp</value>
    </property>
</configuration>


etc/hadoop/hdfs-site.xml

<configuration>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
	<property>
        <name>dfs.permissions</name>
        <value>false</value>
    </property>
</configuration>


etc/hadoop/mapred-site.xml

<configuration>
    <property>
        <name>mapred.job.tracker</name>
        <value>hdfs://ec2-##-###-###-##.compute-#.amazonaws.com</value>
    </property>
</configuration>



#COMMANDS
hdfs namenode –format
HADOOP_HOME/sbin/start-dfs.sh
HADOOP_HOME/sbin/start-yarn.sh
HADOOP_HOME/sbin/mr-jobhistory-daemon.sh start historyserver

hdfs dfs -mkdir /user
hdfs dfs -put ‘/home/ubuntu/100KWikiText.txt’ /user/
hadoop jar freq.jar com.code.freq.frequency /user/100KWikiText.txt output
hdfs dfs –cat /user/ubuntu/output/part-r-00000
